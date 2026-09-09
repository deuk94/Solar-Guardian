package com.kopo.solar.cleaning.entity;

public enum CleaningStatus {
    PENDING("대기중"),
    DONE("완료");

    private final String label;

    CleaningStatus(String label) {
        this.label = label;
    }

    // 화면 표시용 한글 라벨
    public String getLabel() {
        return label;
    }
}
