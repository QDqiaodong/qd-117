package com.inventory.common;

public enum WarningLevel {

    DANGER("danger", "危险"),
    WARNING("warning", "警告"),
    NORMAL("normal", "正常");

    private final String code;
    private final String desc;

    WarningLevel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
