#!/usr/bin/env python3
# GPU 데스크탑에서 실행하는 실시간 패널 오염 탐지 스크립트
#
# 라즈베리파이 mjpg-streamer 원본 영상을 받아 YOLO(train.py로 학습한 best.pt)로 추론
# 오염/이물질 클래스(dust, bird, leaf) 중 하나라도 일정 프레임 이상 연속 감지되면 서버에 자동 세척 요청
# 감지 상태는 대시보드 표시용으로 주기적으로 서버에도 보고
# 박스 그려진 영상은 ANNOTATED_STREAM_PORT로 MJPEG 스트리밍, Spring이 중계해서 대시보드에 표시
#
# 설치: pip install -r requirements.txt
# 실행: python inference_server.py

import os

# Windows에서 PyTorch/NumPy가 OpenMP 런타임 중복 로드로 죽는 문제(OMP: Error #15) 회피
# import torch보다 먼저 설정해야 함
os.environ.setdefault("KMP_DUPLICATE_LIB_OK", "TRUE")

import threading
import time
from datetime import datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import cv2
import numpy as np
import requests
from ultralytics import YOLO

# ─── 환경에 맞게 수정 ───
SNAPSHOT_URL = "http://100.75.114.8:8090/?action=snapshot"  # 라즈베리파이 mjpg-streamer, Tailscale IP
SERVER_BASE = "http://localhost"  # Spring 서버(같은 GPU 호스트의 Docker 컨테이너, 80번 포트 게시)
API_KEY = "solar-sensor-key-2026"  # application.properties의 sensor.api-key와 동일해야 함
LOGIN_ID = "knli0025"

MODEL_PATH = "runs/panel_defect_v3/weights/best.pt"
TRIGGER_CLASS_NAMES = ["dust", "bird", "leaf"]  # dataset/data.yaml names 기준, 하나라도 감지되면 세척 트리거
CONFIDENCE_THRESHOLD = 0.8

LOOP_DELAY_SECONDS = 1.0  # 추론 주기 (영상 표시와 분리, GPU 부하 조절용)
CONSECUTIVE_FRAMES_REQUIRED = 10  # 이만큼 연속 감지돼야 순간 오탐으로 안 보고 세척 요청
STATUS_REPORT_INTERVAL_SECONDS = 5  # 서버에 감지 상태를 보고(DB 기록)하는 주기
CLEANING_POLL_INTERVAL_SECONDS = 5  # 세척 진행 중 완료 여부를 확인하는 주기
DISPLAY_LOOP_DELAY_SECONDS = 0.05  # 영상 표시 갱신 주기 (추론 상태와 무관하게 항상 이 주기로 갱신)

ANNOTATED_STREAM_PORT = 8091  # 박스 그려진 영상을 서빙할 포트 (panel.annotated-stream-url이 이 포트를 가리켜야 함)
BOX_COLOR = (36, 200, 255)  # BGR

HEADERS = {"X-API-KEY": API_KEY}

latest_annotated_jpeg = None
latest_annotated_lock = threading.Lock()

latest_frame = None
latest_frame_lock = threading.Lock()

# 표시 스레드가 최신 원본 프레임 위에 얹어서 그릴, 가장 최근 추론 결과 박스 목록
latest_boxes = []
latest_boxes_lock = threading.Lock()


def find_class_ids(model, names):
    # 모델 클래스 이름 목록에서 names에 해당하는 id 집합 찾기
    name_to_id = {cls_name: idx for idx, cls_name in model.names.items()}
    missing = [name for name in names if name not in name_to_id]
    if missing:
        raise ValueError(f"모델에 {missing} 클래스가 없습니다. 사용 가능한 클래스: {list(model.names.values())}")
    return {name_to_id[name] for name in names}


# mjpg-streamer 스냅샷 액션에서 완결된 프레임을 계속 받아와 latest_frame에 채움
# 스트림을 계속 붙잡는 대신 매번 스냅샷을 요청-응답으로 받는 방식
# requests.Session()으로 TCP 연결은 재사용
def frame_grabber_loop():
    global latest_frame
    session = requests.Session()
    while True:
        try:
            response = session.get(SNAPSHOT_URL, timeout=3)
            response.raise_for_status()
            frame = cv2.imdecode(np.frombuffer(response.content, dtype=np.uint8), cv2.IMREAD_COLOR)
            if frame is not None:
                with latest_frame_lock:
                    latest_frame = frame
        except requests.exceptions.RequestException as e:
            print(f"[{datetime.now()}] 스냅샷 요청 오류: {e}")
            time.sleep(1)


def detect_dirty(results, trigger_class_ids):
    # 트리거 클래스 중 가장 높은 confidence로 오염 여부 판정
    best_confidence = None
    for box in results[0].boxes:
        if int(box.cls) in trigger_class_ids:
            conf = float(box.conf)
            if best_confidence is None or conf > best_confidence:
                best_confidence = conf
    detected = best_confidence is not None and best_confidence >= CONFIDENCE_THRESHOLD
    return detected, best_confidence


def extract_boxes(results):
    # 추론 결과를 draw_boxes/latest_boxes에서 쓰기 좋은 딕셔너리 목록으로 변환
    boxes = []
    for box in results[0].boxes:
        boxes.append({
            "xyxy": box.xyxy[0].tolist(),
            "cls": int(box.cls),
            "conf": float(box.conf),
        })
    return boxes


def draw_boxes(frame, boxes, class_names):
    # 프레임 위에 탐지 박스 + 클래스명·confidence 라벨 그린 사본 반환
    annotated = frame.copy()
    for box in boxes:
        x1, y1, x2, y2 = (int(v) for v in box["xyxy"])
        label = f'{class_names.get(box["cls"], box["cls"])} {box["conf"]:.2f}'
        cv2.rectangle(annotated, (x1, y1), (x2, y2), BOX_COLOR, 2)
        cv2.putText(annotated, label, (x1, max(y1 - 8, 0)), cv2.FONT_HERSHEY_SIMPLEX, 0.6, BOX_COLOR, 2)
    return annotated


# 추론 루프와 별개로 도는 영상 표시 스레드
# 항상 DISPLAY_LOOP_DELAY_SECONDS 주기로 최신 원본 프레임 위에 최근 추론 결과만 얹어서 내보냄
# 추론이 느리거나(GPU 부하) 세척 중이라 멈춰 있어도 영상은 끊김 없이 재생됨
def annotated_frame_loop(class_names):
    global latest_annotated_jpeg
    while True:
        with latest_frame_lock:
            frame = latest_frame
        if frame is not None:
            with latest_boxes_lock:
                boxes = list(latest_boxes)
            annotated = draw_boxes(frame, boxes, class_names) if boxes else frame
            ok, buf = cv2.imencode(".jpg", annotated)
            if ok:
                with latest_annotated_lock:
                    latest_annotated_jpeg = buf.tobytes()
        time.sleep(DISPLAY_LOOP_DELAY_SECONDS)


# 박스 그려진 최신 프레임을 MJPEG로 계속 스트리밍하는 핸들러
class AnnotatedStreamHandler(BaseHTTPRequestHandler):

    # 박스 오버레이 프레임을 MJPEG로 계속 흘려보냄
    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-Type", "multipart/x-mixed-replace; boundary=frame")
        self.end_headers()
        try:
            while True:
                with latest_annotated_lock:
                    jpeg = latest_annotated_jpeg
                if jpeg is not None:
                    self.wfile.write(b"--frame\r\n")
                    self.wfile.write(b"Content-Type: image/jpeg\r\n\r\n")
                    self.wfile.write(jpeg)
                    self.wfile.write(b"\r\n")
                time.sleep(DISPLAY_LOOP_DELAY_SECONDS)
        except ConnectionError:
            pass  # 클라이언트(Spring 프록시)가 연결을 끊은 것 - 정상 종료

    def log_message(self, format, *args):
        pass  # 요청마다 로그 찍으면 너무 시끄러워서 끔


def start_annotated_stream_server():
    # 박스 오버레이 MJPEG 스트림 서버를 별도 스레드로 띄움
    server = ThreadingHTTPServer(("0.0.0.0", ANNOTATED_STREAM_PORT), AnnotatedStreamHandler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()


def report_status(detected, confidence):
    # 현재 오염 감지 상태를 서버에 보고 (대시보드 표시·DB 기록용)
    try:
        response = requests.post(
            f"{SERVER_BASE}/api/panel/detection",
            params={"loginId": LOGIN_ID, "detected": detected, "confidence": confidence},
            headers=HEADERS,
            timeout=5,
        )
        if not response.ok:
            print(f"[{datetime.now()}] 상태 보고 실패({response.status_code}): {response.text}")
    except requests.exceptions.RequestException as e:
        print(f"[{datetime.now()}] 서버 연결 오류(상태 보고): {e}")


# 서버에 자동 세척 요청
# 성공 시 commandId, 거부(이미 진행 중/쿨다운)나 오류 시 None 반환
# 호출부는 None 아닐 때만 cleaning_in_progress로 전환
def request_cleaning():
    try:
        response = requests.post(
            f"{SERVER_BASE}/api/cleaning/request",
            params={"loginId": LOGIN_ID},
            headers=HEADERS,
            timeout=5,
        )
        if response.ok:
            data = response.json()
            print(f"[{datetime.now()}] 자동 세척 요청")
            return data.get("commandId")
        else:
            print(f"[{datetime.now()}] 자동 세척 요청 보류({response.status_code}): {response.text}")
            return None
    except requests.exceptions.RequestException as e:
        print(f"[{datetime.now()}] 서버 연결 오류(세척 요청): {e}")
        return None


# 세척이 아직 진행 중인지 확인
# 응답/네트워크 오류 시엔 진행 중(True)으로 간주 - 추론이 섣불리 재개되지 않게 안전한 쪽으로
def is_cleaning_pending(login_id):
    try:
        response = requests.get(
            f"{SERVER_BASE}/api/cleaning/pending",
            params={"loginId": login_id},
            headers=HEADERS,
            timeout=5,
        )
        if response.ok:
            return bool(response.json().get("commandId"))
        print(f"[{datetime.now()}] 세척 상태 확인 실패({response.status_code}): {response.text}")
        return True
    except requests.exceptions.RequestException as e:
        print(f"[{datetime.now()}] 서버 연결 오류(세척 상태 확인): {e}")
        return True


# 모델 로드 후 스트림 서버·프레임 수집 스레드 띄우고, 메인 루프에서 추론·오염판정·세척요청 반복
def main():
    model = YOLO(MODEL_PATH)
    trigger_class_ids = find_class_ids(model, TRIGGER_CLASS_NAMES)
    print(f"[{datetime.now()}] Solar AI 가동")

    start_annotated_stream_server()
    threading.Thread(target=frame_grabber_loop, daemon=True).start()
    threading.Thread(target=annotated_frame_loop, args=(model.names,), daemon=True).start()

    consecutive_hits = 0
    last_report_time = 0.0
    cleaning_in_progress = False
    last_cleaning_poll_time = 0.0

    while True:
        if cleaning_in_progress:
            # 세척이 끝날 때까지 추론/카운팅/상태보고 쉬고, 완료 여부만 주기적으로 확인
            now = time.time()
            if now - last_cleaning_poll_time >= CLEANING_POLL_INTERVAL_SECONDS:
                last_cleaning_poll_time = now
                if not is_cleaning_pending(LOGIN_ID):
                    print(f"[{datetime.now()}] 세척 완료, Solar AI 재가동")
                    cleaning_in_progress = False
                    consecutive_hits = 0
            time.sleep(LOOP_DELAY_SECONDS)
            continue

        with latest_frame_lock:
            frame = latest_frame

        if frame is None:
            time.sleep(0.1)  # 아직 첫 프레임이 안 들어온 초기 구간
            continue

        results = model.predict(frame, verbose=False)
        with latest_boxes_lock:
            latest_boxes[:] = extract_boxes(results)
        detected, confidence = detect_dirty(results, trigger_class_ids)

        if detected:
            consecutive_hits += 1
            print(f"[{datetime.now()}] 오염 감지 ({consecutive_hits}/{CONSECUTIVE_FRAMES_REQUIRED})")
        else:
            consecutive_hits = 0

        now = time.time()
        if now - last_report_time >= STATUS_REPORT_INTERVAL_SECONDS:
            report_status(detected, confidence)
            last_report_time = now

        if consecutive_hits >= CONSECUTIVE_FRAMES_REQUIRED:
            command_id = request_cleaning()
            consecutive_hits = 0
            if command_id is not None:
                cleaning_in_progress = True
                last_cleaning_poll_time = time.time()
                with latest_boxes_lock:
                    latest_boxes.clear()  # 세척 중엔 더 이상 유효하지 않은 탐지 박스이므로 지움

        time.sleep(LOOP_DELAY_SECONDS)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n Solar AI 종료")
