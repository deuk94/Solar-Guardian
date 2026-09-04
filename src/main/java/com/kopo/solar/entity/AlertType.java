package com.kopo.solar.entity;

public enum AlertType {
    CURRENT_HIGH("전류 과다"),
    CURRENT_LOW("전류 부족"),
    VOLTAGE_HIGH("전압 과다"),
    VOLTAGE_LOW("전압 부족");

    private final String label;

    AlertType(String label) {
        this.label = label;
    }

    // 화면 표시용 한글 라벨
    public String getLabel() {
        return label;
    }
}
