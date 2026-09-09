#!/usr/bin/env python3
# 라즈베리파이에서 실행하는 스크립트
# 임의의 전류/전압 값을 주기적으로 생성해 대시보드 서버로 전송 (실센서 연동 전 임시 데이터 소스)
#
# 설치: pip3 install requests
# 실행: python3 send_sensor_data.py

import math
import random
import time
from datetime import datetime

import requests

# ─── 환경에 맞게 수정 ───
SERVER_URL = "http://100.82.175.26/api/sensor/data"  # Spring 서버(GPU) Tailscale IP (80번 포트로 게시, docker-compose.gpu.yml 참고)
CLEANING_PENDING_URL = "http://100.82.175.26/api/cleaning/pending"  # 세척 진행 여부 확인용
API_KEY = "solar-sensor-key-2026"                           # application.properties의 sensor.api-key와 동일해야 함
LOGIN_ID = "knli0025"                                        # 데이터를 귀속시킬 회원의 로그인 아이디
INTERVAL_SECONDS = 10

# 임의 데이터 생성 범위 (정상값)
CURRENT_MIN, CURRENT_MAX = 0.0, 8.0     # A
VOLTAGE_MIN, VOLTAGE_MAX = 200.0, 240.0  # V

# 전류는 완전 랜덤이 아니라 일출~일몰 사이 정오를 정점으로 하는 곡선을 따름
# (태양광 발전량처럼 시간대별로 자연스럽게 오르내리도록 — 대시보드 시간당 그래프용)
DAY_START_HOUR, DAY_END_HOUR = 6, 18   # 이 구간 밖(야간)은 발전량 0에 수렴
CURRENT_JITTER = 0.4                   # 곡선 값에 더할 잡음 폭(A)

# 이상치 발생 확률과 범위 (서버 sensor.threshold.*를 벗어나는 값 - 알림 기능 테스트용)
# 전압 저하는 실제로 잘 발생하지 않고 주 목적(패널 세척 필요 여부 감지)과도 무관해서
# 전류만 확률적으로 이상치 발생시킴
ANOMALY_CHANCE = 0.004
ANOMALY_CURRENT_LOW = (-2.0, -0.1)    # threshold current-min(0.0) 미만 - 패널 오염 등 발전량 저하 시나리오
ANOMALY_CURRENT_HIGH = (10.5, 15.0)   # threshold current-max(10.0) 초과


def solar_curve_current():
    # 지금 시각 기준, 일출~일몰 사이 정오를 정점으로 하는 발전 곡선 값
    now = datetime.now()
    hour = now.hour + now.minute / 60.0

    if hour < DAY_START_HOUR or hour > DAY_END_HOUR:
        base = 0.0
    else:
        span = DAY_END_HOUR - DAY_START_HOUR
        base = CURRENT_MAX * math.sin(math.pi * (hour - DAY_START_HOUR) / span)

    current = base + random.uniform(-CURRENT_JITTER, CURRENT_JITTER)
    return max(0.0, min(CURRENT_MAX, current))


def generate_reading():
    # 정상 범위 전류/전압 생성, 낮은 확률로 임계값 벗어난 이상치 섞음 (알림 기능 테스트용)
    current = solar_curve_current()
    voltage = random.uniform(VOLTAGE_MIN, VOLTAGE_MAX)

    if random.random() < ANOMALY_CHANCE:
        low, high = random.choice([ANOMALY_CURRENT_LOW, ANOMALY_CURRENT_HIGH])
        current = random.uniform(low, high)

    return {
        "loginId": LOGIN_ID,
        "current": round(current, 2),
        "voltage": round(voltage, 2),
    }


def send(reading):
    # 측정값 하나 서버로 전송
    headers = {"X-API-KEY": API_KEY, "Content-Type": "application/json"}
    response = requests.post(SERVER_URL, json=reading, headers=headers, timeout=5)
    return response


def is_cleaning_pending():
    # 세척 진행 중엔 센서 데이터 전송 쉬기 위한 확인
    # 응답/네트워크 오류 시엔 "진행 중 아님"으로 간주하고 평소처럼 전송 계속
    try:
        headers = {"X-API-KEY": API_KEY}
        response = requests.get(
            CLEANING_PENDING_URL, params={"loginId": LOGIN_ID}, headers=headers, timeout=5
        )
        if response.ok:
            return bool(response.json().get("commandId"))
    except requests.exceptions.RequestException:
        pass
    return False


def main():
    # 세척 진행 중 아닐 때만 INTERVAL_SECONDS 주기로 센서 데이터 계속 전송
    print(f"[{datetime.now()}] 센서 데이터 전송 시작")
    while True:
        if not is_cleaning_pending():
            reading = generate_reading()
            try:
                response = send(reading)
                if response.ok:
                    print(f"[{datetime.now()}] 센서 데이터 전송 성공")
                else:
                    print(f"[{datetime.now()}] 전송 실패({response.status_code}): {response.text}")
            except requests.exceptions.RequestException as e:
                print(f"[{datetime.now()}] 서버 연결 오류: {e}")

        time.sleep(INTERVAL_SECONDS)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n종료합니다.")
