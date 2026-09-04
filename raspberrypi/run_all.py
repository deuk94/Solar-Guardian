#!/usr/bin/env python3
# 라즈베리파이에서 실행하는 통합 실행 스크립트
# send_sensor_data.py / cleaning_control.py / capture_panel.py 를 한 프로세스에서 스레드로 동시 실행
# (각 파일 따로 실행할 필요 없이 이거 하나만 실행하면 됨)
# 개별 스크립트 설정값(SERVER_URL, LOGIN_ID 등)은 각 파일에서 그대로 수정
#
# 설치: pip3 install requests pyserial
# 실행: python3 run_all.py

import threading
import capture_panel
import cleaning_control
import send_sensor_data


def run(name, target):
    # 스레드 하나 죽어도 나머지엔 영향 없이 로그만 남기고 넘어가게 예외 처리
    try:
        target()
    except Exception as e:
        print(f"[{name}] 예기치 못한 오류로 종료: {e}")


def main():
    # 세 스크립트 각각 스레드로 띄우고 모두 종료할 때까지 대기
    tasks = [
        ("센서 전송", send_sensor_data.main),
        ("세척 제어", cleaning_control.main),
        ("패널 스냅샷", capture_panel.main),
    ]
    threads = [threading.Thread(target=run, args=(name, target), daemon=True) for name, target in tasks]

    for t in threads:
        t.start()
    for t in threads:
        t.join()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n종료합니다.")
