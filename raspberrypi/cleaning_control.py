#!/usr/bin/env python3
# 라즈베리파이에서 실행하는 스크립트
# 서버의 세척 대기 명령을 주기적으로 폴링, 있으면 시리얼로 아두이노에 전달해 모터 구동
# 아두이노가 작업 마치고 응답하면 서버에 완료 보고
#
# 설치: pip3 install requests pyserial
# 실행: python3 cleaning_control.py

import time
from datetime import datetime

import requests
import serial

# ─── 환경에 맞게 수정 ───
SERVER_BASE = "http://100.82.175.26"  # Spring 서버(GPU) Tailscale IP (80번 포트로 게시, docker-compose.gpu.yml 참고)
API_KEY = "solar-sensor-key-2026"            # application.properties의 sensor.api-key와 동일해야 함
LOGIN_ID = "knli0025"                         # 세척 요청을 걸어둔 회원의 로그인 아이디
POLL_INTERVAL_SECONDS = 5

SERIAL_PORT = "/dev/ttyUSB0"  # ls /dev/tty* 로 확인 (보통 /dev/ttyACM0 또는 /dev/ttyUSB0) - USB 재연결로 번호가 바뀔 수 있음
BAUD_RATE = 9600              # 아두이노 스케치의 Serial.begin() 값과 반드시 일치해야 함
RESPONSE_TIMEOUT_SECONDS = 60  # 아두이노의 CLEAN_DONE 응답을 기다리는 최대 시간

HEADERS = {"X-API-KEY": API_KEY}


def open_arduino():
    # 시리얼 포트 열고, 아두이노 리셋 후 부팅될 때까지 대기
    ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
    time.sleep(2)  # 시리얼 포트를 열면 아두이노가 리셋되므로 부팅될 때까지 대기
    ser.reset_input_buffer()
    return ser


def get_pending_command_id():
    # 대기 중인 세척 명령 있으면 그 id, 없으면 None
    url = f"{SERVER_BASE}/api/cleaning/pending"
    response = requests.get(url, params={"loginId": LOGIN_ID}, headers=HEADERS, timeout=5)
    response.raise_for_status()
    return response.json().get("commandId")


def report_complete(command_id):
    # 세척 완료를 서버에 보고
    url = f"{SERVER_BASE}/api/cleaning/{command_id}/complete"
    response = requests.post(url, headers=HEADERS, timeout=5)
    response.raise_for_status()


def run_cleaning(ser):
    # 아두이노에 세척 시작 명령 보내고 CLEAN_DONE 응답 대기, 성공 여부 반환
    print(f"[{datetime.now()}] 세척 명령 전송")
    ser.write(b"CLEAN_START\n")

    deadline = time.time() + RESPONSE_TIMEOUT_SECONDS
    while time.time() < deadline:
        line = ser.readline().decode(errors="ignore").strip()
        if line:
            print(f"[{datetime.now()}] 세척 모듈 응답")
        if line == "CLEAN_DONE":
            return True
    return False


def main():
    # POLL_INTERVAL_SECONDS 주기로 세척 대기 명령 확인, 있으면 실행
    print(f"[{datetime.now()}] 세척 제어 시작 ")
    ser = open_arduino()

    while True:
        try:
            command_id = get_pending_command_id()
            if command_id:
                print(f"[{datetime.now()}] 대기 중인 세척 명령 발견")
                if run_cleaning(ser):
                    report_complete(command_id)
                    print(f"[{datetime.now()}] 세척 완료")
                else:
                    # 응답 없어도 서버 쪽 명령은 여전히 PENDING이라 다음 폴링에서 재시도
                    # (아두이노가 실제론 끝냈는데 응답만 못 받았으면 모터 중복 구동 가능성 있음)
                    print(f"[{datetime.now()}] 아두이노 응답 시간 초과 - 다음 폴링에서 재시도")
        except requests.exceptions.RequestException as e:
            print(f"[{datetime.now()}] 서버 연결 오류: {e}")
        except serial.SerialException as e:
            print(f"[{datetime.now()}] 시리얼 통신 오류: {e}")

        time.sleep(POLL_INTERVAL_SECONDS)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n종료합니다.")
