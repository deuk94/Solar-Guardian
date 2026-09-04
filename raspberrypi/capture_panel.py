#!/usr/bin/env python3
"""라즈베리파이에서 실행하는 스크립트.

주기적으로 패널 사진 한 장을 서버에 업로드한다 (AI 패널 상태용 스냅샷).
카메라를 직접 열지 않고, mjpg-streamer가 이미 잡고 있는 스트림에서 스냅샷
액션으로 한 장만 받아온다

설치: pip3 install requests
실행: python3 capture_panel.py (mjpg-streamer가 먼저 켜져 있어야 함)
"""

import time
from datetime import datetime

import requests

# ─── 환경에 맞게 수정 ───
SERVER_URL = "http://100.82.175.26/api/panel/snapshot"  # Spring 서버(GPU) ip
API_KEY = "solar-sensor-key-2026"
LOGIN_ID = "knli0025"
CAPTURE_INTERVAL_SECONDS = 60

SNAPSHOT_URL = "http://127.0.0.1:8090/?action=snapshot"  # mjpg-streamer의 스냅샷 액션
CAPTURE_PATH = "/tmp/panel_snapshot.jpg"


def capture_photo():
    # mjpg-streamer 스냅샷 액션에서 사진 한 장 받아 로컬 파일로 저장
    response = requests.get(SNAPSHOT_URL, timeout=10)
    response.raise_for_status()
    with open(CAPTURE_PATH, "wb") as f:
        f.write(response.content)


def upload_photo():
    # 로컬에 저장한 사진을 서버로 업로드
    with open(CAPTURE_PATH, "rb") as f:
        files = {"file": ("panel.jpg", f, "image/jpeg")}
        data = {"loginId": LOGIN_ID}
        headers = {"X-API-KEY": API_KEY}
        response = requests.post(SERVER_URL, files=files, data=data, headers=headers, timeout=10)
        return response


def main():
    # CAPTURE_INTERVAL_SECONDS 주기로 촬영·업로드 반복
    print(f"[{datetime.now()}] 패널 스냅샷 시작 ")
    while True:
        try:
            capture_photo()
            response = upload_photo()
            if response.ok:
                print(f"[{datetime.now()}] 스냅샷 업로드 성공")
            else:
                print(f"[{datetime.now()}] 업로드 실패({response.status_code}): {response.text}")
        except requests.exceptions.RequestException as e:
            print(f"[{datetime.now()}] 스냅샷/서버 연결 오류: {e}")

        time.sleep(CAPTURE_INTERVAL_SECONDS)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n종료합니다.")
