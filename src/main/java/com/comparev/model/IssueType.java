package com.comparev.model;

public enum IssueType {
    CLASS_MISSING("源码缺少类"),
    METHOD_MISSING("源码缺少方法"),
    RETURN_TYPE_MISMATCH("返回类型不一致"),
    ACCESS_MISMATCH("访问修饰符不一致"),
    LOCAL_EXTRA_METHOD("本地多余方法"),
    IMPLEMENTATION_COMPARE("方法实现比对");

    private final String displayName;

    IssueType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static IssueType fromDisplayName(String displayName) {
        for (IssueType issueType : values()) {
            if (issueType.displayName.equals(displayName)) {
                return issueType;
            }
        }
        return null;
    }
}
