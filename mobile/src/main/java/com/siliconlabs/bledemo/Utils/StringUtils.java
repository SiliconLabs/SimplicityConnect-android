package com.siliconlabs.bledemo.Utils;

public class StringUtils {

    public static String HEX_VALUES = "0123456789abcdefABCDEF";

    public static String getStringWithoutWhitespaces(String text) {
        return text.replaceAll(" ","");
    }

    public static String getStringWithoutColons(String text) {
        if (text == null || text.equals("")) return "";

        StringBuilder result = new StringBuilder();
        String[] splittedStrings = text.split(":");

        for (String str : splittedStrings) {
            result.append(str);
        }

        return result.toString();
    }

}
