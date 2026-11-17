package com.shadow.apps.ai;

public final class AnswerFormatter {
    private AnswerFormatter() {}

    public static String clean(String value) {
        if (value == null) return "";

        String s = value.replace("\r\n", "\n").replace('\r', '\n');
        return s.trim();
    }
}
