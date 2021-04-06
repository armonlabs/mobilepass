package com.armongate.mobilepasssdk.util;

import java.math.BigInteger;
import java.util.Arrays;

public class ConverterUtil {

    private static String DIGITS = "0123456789ABCDEF";

    public static String dataToString(byte[] data) {
        return new String(data);
    }

    public static long cardDataToDecimal(byte[] data) {
        long result = 0;
        long factor = 1;

        for (int i=0; i< data.length; i++) {
            long value = data[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }

        return result;
    }

    public static int dataToInt(byte[] data) {
        return new BigInteger(data).intValue();
    }

    public static int dataToInt(byte data) {
        return Integer.decode(String.format("0x%02X", data));
    }

    public static boolean dataToBool(byte[] data) {
        int value = new BigInteger(data).intValue();
        return value != 0;
    }

    public static String dataToIpAddress(byte[] data) {
        String result = "";

        for (int i = 0; i < data.length; i++) {
            if (result.length() > 0) {
                result += ".";
            }

            result += dataToInt(data[i]);
        }

        return result;
    }

    public static byte[] ipAddressToData(String ipAddress) {
        String[]    parts   = ipAddress.split("\\.");
        byte[]      result  = new byte[] {};

        for (String part: parts) {
            try {
                result = ArrayUtil.add(result, (byte)Integer.parseInt(part));
            } catch (Exception ex) {}
        }

        return result;
    }

    public static byte[] longToData(long value, int length, byte repeating, boolean fillLeading) {
        BigInteger bigInt = new BigInteger(String.valueOf(value));
        return fillArray(bigInt.toByteArray(), length, repeating, fillLeading);
    }

    public static byte[] stringToData(String value, int length, byte repeating, boolean fillLeading) {
        return fillArray(value.getBytes(), length, repeating, fillLeading);
    }

    public static byte[] fillArray(byte[] data, int length, byte repeating, boolean fillLeading) {
        byte[] fillArray = new byte[length];
        Arrays.fill(fillArray, repeating);

        System.arraycopy(data, 0, fillArray, fillLeading ? fillArray.length - data.length : 0, data.length);

        return fillArray;
    }

    public static byte mergeToData(int deviceNumber, int direction, int relayNumber) {
        String deviceBinary = ("00" + Integer.toBinaryString(deviceNumber));
        String directionBinary = ("00" + Integer.toBinaryString(direction));
        String relayBinary = ("0000" + Integer.toBinaryString(relayNumber));

        String deviceValue = deviceBinary.substring(deviceBinary.length() - 2);
        String directionValue = directionBinary.substring(directionBinary.length() - 2);
        String relayValue = relayBinary.substring(relayBinary.length() - 4);

        int resultValue = Integer.parseInt(deviceValue + directionValue + relayValue, 2);
        byte[] result = BigInteger.valueOf(resultValue).toByteArray();

        return result.length > 0 ? result[result.length - 1] : (byte)0x00;
    }

    public static String bytesToHexString(byte[] data) {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i != data.length; i++) {
            int v = data[i] & 0xff;

            buffer.append(DIGITS.charAt(v >> 4));
            buffer.append(DIGITS.charAt(v & 0xf));
        }

        return buffer.toString();
    }

    public static byte[] hexStringToBytes(String data) {
        int length = data.length();
        byte[] result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            result[i / 2] = (byte) ((Character.digit(data.charAt(i), 16) << 4) + Character
                    .digit(data.charAt(i + 1), 16));
        }
        return result;
    }

}
