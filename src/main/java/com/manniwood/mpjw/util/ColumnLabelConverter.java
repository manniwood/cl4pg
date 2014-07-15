package com.manniwood.mpjw.util;

public class ColumnLabelConverter {
    public static String convert(String label) {
        StringBuilder sb = new StringBuilder();
        sb.append("set");
        sb.append(Character.toUpperCase(label.charAt(0)));
        boolean needsUpper = false;
        for (int i = 1; i < label.length(); i++) {
            char c = label.charAt(i);
            if (c == '_') {
                needsUpper = true;
                continue;
            }
            if (needsUpper) {
                c = Character.toUpperCase(c);
                needsUpper = false;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
