#!/usr/bin/env python3
# YOLOv8 커스텀 학습 스크립트 (GPU 서버에서 실행)
# ai/dataset/ 안에 Roboflow에서 받은 데이터셋(train/valid/test + data.yaml) 필요
#
# 설치: pip install ultralytics
# 실행: python3 train.py
#
# 데이터 추가 후 재학습하는 법
# 1. Roboflow 프로젝트에 새로 찍은 이미지(실제 카메라/패널/조명 환경) 업로드·라벨링 후
#    새 버전 Generate해서 ai/dataset/ 을 최신 버전(기존+신규 이미지 전부)으로 다시 받기
# 2. BASE_MODEL은 COCO 베이스("yolov8n.pt") 유지 - 이유는 TROUBLESHOOTING.md
# 3. RUN_NAME을 새 이름으로 바꿔서 기존 결과(runs/panel_defect*/) 안 덮어쓰게

from ultralytics import YOLO

DATA_YAML = "dataset/data.yaml"
BASE_MODEL = "yolov8n.pt"
EPOCHS = 100
IMG_SIZE = 640
BATCH_SIZE = 16
PROJECT_DIR = "runs"
RUN_NAME = "panel_defect_v3"  # 기존 panel_defect, panel_defect_v2 결과를 보존하기 위해 새 이름 사용


# 데이터셋으로 YOLOv8 학습. 결과 가중치는 {PROJECT_DIR}/{RUN_NAME}/weights/best.pt에 저장되고
# ai/inference_server.py가 이 파일을 로드해서 추론에 사용
def main():
    model = YOLO(BASE_MODEL)
    model.train(
        data=DATA_YAML,
        epochs=EPOCHS,
        imgsz=IMG_SIZE,
        batch=BATCH_SIZE,
        project=PROJECT_DIR,
        name=RUN_NAME,
    )


if __name__ == "__main__":
    main()
