/*
 * 라즈베리파이로부터 시리얼로 세척 명령을 받아 스텝모터를 구동하는 아두이노 우노 스케치
 * 배선: 스텝모터 드라이버(A4988/DRV8825 등)의 DIR -> dirPin, STEP -> stepPin 연결
 * 통신: 라즈베리파이 cleaning_control.py 의 BAUD_RATE와 반드시 동일해야 함
 */

const int dirPin = 2;
const int stepPin = 3;

/*
 * 스텝모터를 지정한 방향으로 steps만큼 회전 (delay_us는 스텝 간 간격, 속도 조절용)
 * steps/루프 변수는 long이어야 함 - 32767 넘으면 int 오버플로우 (TROUBLESHOOTING.md)
 */
void turn(int direction, long steps, int delay_us) {
  digitalWrite(dirPin, direction);  // 회전방향 설정 - 역방향으로 돌리고 싶으면 LOW로 설정
  for (long i = 0; i < steps; i++) {
    digitalWrite(stepPin, HIGH);
    delayMicroseconds(delay_us);
    digitalWrite(stepPin, LOW);
    delayMicroseconds(delay_us);
  }
}

// 시리얼 시작, 핀 모드 설정, 부팅 확인 신호 전송
void setup() {
  Serial.begin(9600);
  pinMode(dirPin, OUTPUT);
  pinMode(stepPin, OUTPUT);
  Serial.println("READY"); // 전원 인가/리셋 후 부팅 확인용
}

// 라즈베리파이가 보낸 명령 한 줄씩 읽어서 CLEAN_START면 세척 실행 후 CLEAN_DONE으로 응답
void loop() {
  if (Serial.available()) {
    String command = Serial.readStringUntil('\n');
    command.trim();

    Serial.print("받음: [");   // 디버그용 echo - 실제로 뭘 받았는지 확인
    Serial.print(command);
    Serial.println("]");

    if (command == "CLEAN_START") {
      runCleaning();
      Serial.println("CLEAN_DONE");
    }
  }
}

// 세척 브러시 내렸다가 다시 올림 (내려가기 -> 올라가기 한 사이클)
void runCleaning() {
  turn(HIGH, 37600, 150);
  delay(1000);
  turn(LOW, 37600, 100);
  delay(1000);
}
