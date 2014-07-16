package com.manniwood.mpjw.util;

public class ColumnLabelConverter {

    /**
     * Convert a column label, such as updated_on
     * to a Java bean set method, such as setUpdatedOn.
     * @param label
     * @return
     */
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
