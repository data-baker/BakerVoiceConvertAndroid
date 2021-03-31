package com.databaker.voiceconvert.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ArrayUtils {

    public static byte[] toByteArray(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }


    public static byte[] plus(byte[] byteArray, byte[] elements) {
        int thisSize = byteArray.length;
        int arraySize = elements.length;
        byte[] result = Arrays.copyOf(byteArray, thisSize + arraySize);
        System.arraycopy(elements, 0, result, thisSize, arraySize);
        return result;
    }

}
