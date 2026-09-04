/*
 * 하드웨어(배선/전원) 확인용 임시 테스트 스케치
 * 시리얼 명령 없이 전원만 넣으면 자동으로 계속 회전
 * 여기서도 안 돌면 CLEAN_START 로직이 아니라 배선/전원 문제로 확정 가능
 */

int dirPin = 2;
int stepPin = 3;

// 지정한 방향으로 steps만큼 회전
void turn(int direction, int steps, int delay_us) {
  digitalWrite(dirPin, direction);
  for (int i = 0; i < steps; i++) {
    digitalWrite(stepPin, HIGH);
    delayMicroseconds(delay_us);
    digitalWrite(stepPin, LOW);
    delayMicroseconds(delay_us);
  }
}

// 핀 모드 설정
void setup() {
  pinMode(dirPin, OUTPUT);
  pinMode(stepPin, OUTPUT);
}

// 전원 들어와 있는 동안 계속 회전만 반복
void loop() {
  turn(LOW, 37600, 150);
}
