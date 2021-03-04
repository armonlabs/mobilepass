package com.armongate.mobilepasssdk.util;

import com.armongate.mobilepasssdk.model.BLEDataParseFormat;

import java.util.Arrays;

public class ArrayUtil {

    public static byte[] concat(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    public static <T> T[] concat(T[] first, T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    public static byte[] add(byte[] arr, byte newElement) {
        arr = Arrays.copyOf(arr, arr.length + 1);
        arr[arr.length - 1] = newElement;
        return arr;
    }

    public static BLEDataParseFormat[] add(BLEDataParseFormat[] arr, BLEDataParseFormat newElement) {
        arr = Arrays.copyOf(arr, arr.length + 1);
        arr[arr.length - 1] = newElement;
        return arr;
    }

}
