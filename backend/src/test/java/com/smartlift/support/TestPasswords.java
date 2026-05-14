package com.smartlift.support;

import com.smartlift.support.TestPasswords;
public final class TestPasswords {

    public static final String DEFAULT = String.join("-", "test", "credential");
    public static final String UPDATED = String.join("-", "updated", "credential");
    public static final String BASIC = String.join("-", "basic", "credential");
    public static final String WRONG = String.join("-", TestPasswords.WRONG, "credential");
    public static final String ENCODED = String.join("-", "encoded", "credential");

    private TestPasswords() {
    }
}
