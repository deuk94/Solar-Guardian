package com.kopo.solar.device.entity;

public enum DeviceStatus {
    NORMAL("정상"),
    ABNORMAL("이상");

    private final String label;

    DeviceStatus(String label) {
        this.label = label;
    }

    // 화면 표시용 한글 라벨
    public String getLabel() {
        return label;
    }
}
