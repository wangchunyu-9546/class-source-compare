package com.comparev.model;

public enum Severity {
    ERROR("不兼容"),
    WARNING("需确认"),
    INFO("提示");

    private final String displayName;

    Severity(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
