package com.armongate.mobilepasssdk.util;

import com.armongate.mobilepasssdk.constant.DataTypes;
import com.armongate.mobilepasssdk.constant.PacketHeaders;
import com.armongate.mobilepasssdk.manager.LogManager;
import com.armongate.mobilepasssdk.model.BLEDataContent;
import com.armongate.mobilepasssdk.model.BLEDataParseFormat;

import java.util.Arrays;
import java.util.HashMap;

public class DataParserUtil {

    // Singleton

    private static DataParserUtil instance = null;
    private DataParserUtil() { }

    public static DataParserUtil getInstance() {
        if (instance == null) {
            instance = new DataParserUtil();
        }

        return instance;
    }

    // Public Functions

    public BLEDataContent parse(byte[] data) {
        if (data.length < 3) {
            LogManager.getInstance().warn("Data received from device has invalid length!", null);
            return null;
        }

        switch (ConverterUtil.dataToInt(data[0])) {
            case 1:
                return parseForProtocolV1(Arrays.copyOfRange(data, 1, data.length));
            case 2:
                return parseForProtocolV2(Arrays.copyOfRange(data, 1, data.length));
            default:
                // TODO Add message to unknown protocol
                LogManager.getInstance().warn("Unknown data protocol received!", null);
                return null;
        }
    }

    // Private Functions

    private BLEDataContent parseForProtocolV1(byte[] data) {
        LogManager.getInstance().error("Protocol V1 is ignored to process response!", null);
        return null;
    }

    private BLEDataContent parseForProtocolV2(byte[] data) {
        if (data[0] == PacketHeaders.PROTOCOLV2.GROUP.AUTH) {
            byte[] typeData = Arrays.copyOfRange(data, 2, data.length);

            switch (data[1]) {
                case PacketHeaders.PROTOCOLV2.AUTH.PUBLICKEY_CHALLENGE:
                    return parseAuthChallengeData(typeData);
                case PacketHeaders.PROTOCOLV2.AUTH.CHALLENGE_RESULT:
                    return parseAuthChallengeResult(typeData);
                case PacketHeaders.PROTOCOLV2.AUTH.CHALLENGE_RESULT_WITH_CODE:
                    return parseAuthChallengeResultWithCode(typeData);
                default:
                    return null;
            }
        }

        return null;
    }

    private BLEDataContent parseAuthChallengeData(byte[] data) {
        // The capabilities byte is only sent by newer firmware. Older devices end
        // the packet after the iv, in which case processData simply stops and the
        // field is absent - equivalent to the documented "length > 83" check on
        // the full packet.
        BLEDataParseFormat[] dataFormat = new BLEDataParseFormat[] {
                new BLEDataParseFormat("deviceId", 32, BLEDataParseFormat.DataType.STRING),
                new BLEDataParseFormat("challenge", 32, BLEDataParseFormat.DataType.DATA),
                new BLEDataParseFormat("iv", 16, BLEDataParseFormat.DataType.DATA),
                new BLEDataParseFormat("capabilities", 1, BLEDataParseFormat.DataType.NUMBER)
        };

        return new BLEDataContent(DataTypes.TYPE.AuthChallengeForPublicKey, DataTypes.RESULT.Succeed, processData(data, dataFormat));
    }

    /**
     * Parse the extended challenge result, answered only to a request that
     * declared result code support.
     *
     * Layout differs from the plain challenge result in two ways, so it is parsed
     * as its own branch: reason is present on success too, and the code comes
     * before the message. That ordering is deliberate on the device side - a
     * small MTU truncates the message and never the code.
     */
    private BLEDataContent parseAuthChallengeResultWithCode(byte[] data) {
        if (data.length == 0) {
            LogManager.getInstance().warn("Challenge result with code received from device has no content!", null);
            return null;
        }

        boolean isSucceed;

        if (data[0] == PacketHeaders.PROTOCOLV2.COMMON.SUCCESS) {
            isSucceed = true;
        } else if (data[0] == PacketHeaders.PROTOCOLV2.COMMON.FAILURE) {
            isSucceed = false;
        } else {
            LogManager.getInstance().warn("Challenge result with code has unknown state: " + data[0], null);
            return null;
        }

        BLEDataParseFormat[] dataFormat = new BLEDataParseFormat[] {
                new BLEDataParseFormat("reason", 1, BLEDataParseFormat.DataType.NUMBER),
                new BLEDataParseFormat("codeLength", 1, BLEDataParseFormat.DataType.NUMBER),
                new BLEDataParseFormat("code", "codeLength", BLEDataParseFormat.DataType.STRING),
                new BLEDataParseFormat("message", BLEDataParseFormat.DataType.STRING)
        };

        return new BLEDataContent(DataTypes.TYPE.AuthChallengeResult,
                isSucceed ? DataTypes.RESULT.Succeed : DataTypes.RESULT.Failed,
                processData(Arrays.copyOfRange(data, 1, data.length), dataFormat));
    }

    private BLEDataContent parseAuthChallengeResult(byte[] data) {
        if (data.length == 0) {
            LogManager.getInstance().warn("Challenge result received from device has no content!", null);
            return null;
        }

        switch (data[0]) {
            case PacketHeaders.PROTOCOLV2.COMMON.SUCCESS:
                return new BLEDataContent(DataTypes.TYPE.AuthChallengeResult, DataTypes.RESULT.Succeed, null);
            case PacketHeaders.PROTOCOLV2.COMMON.FAILURE:
                BLEDataParseFormat[] dataFormat = new BLEDataParseFormat[] {
                        new BLEDataParseFormat("reason", 1, BLEDataParseFormat.DataType.NUMBER),
                        new BLEDataParseFormat("message", BLEDataParseFormat.DataType.STRING)
                };

                return new BLEDataContent(DataTypes.TYPE.AuthChallengeResult,
                        DataTypes.RESULT.Failed,
                        processData(Arrays.copyOfRange(data, 1, data.length), dataFormat));
            default:
                return null;
        }
    }

    private HashMap<String, Object> processData(byte[] data, BLEDataParseFormat[] format) {
        HashMap<String, Object> hashResult = new HashMap<>();

        int currentIndex = 0;

        for (BLEDataParseFormat formatItem: format) {
            if (currentIndex >= data.length) {
                // Completed
                break;
            }

            boolean removeLengthField = false;
            int length;
            if (formatItem.useLeftData != null && formatItem.useLeftData) {
                length = -1;
            } else if (formatItem.useLengthFromField != null && !formatItem.useLengthFromField.isEmpty()) {
                // Never let a length read from the packet go negative, otherwise
                // it would be taken for the "use whatever is left" sentinel below
                length = hashResult.containsKey(formatItem.useLengthFromField) ? Math.max(0, (int)hashResult.get(formatItem.useLengthFromField)) : 0;
                removeLengthField = true;
            } else {
                length = formatItem.dataLength;
            }

            // Lengths that come from the packet itself (codeLength) are clamped to
            // the buffer: a truncated or malformed packet must not read past it,
            // where copyOfRange would silently pad with zeros. A negative length
            // means "use whatever is left".
            int endIndex = length >= 0 ? Math.min(currentIndex + length, data.length) : data.length;
            byte[] subData = Arrays.copyOfRange(data, currentIndex, endIndex);

            if (formatItem.dataType == BLEDataParseFormat.DataType.STRING) {
                hashResult.put(formatItem.fieldName, ConverterUtil.dataToString(subData));
            } else if (formatItem.dataType == BLEDataParseFormat.DataType.NUMBER) {
                hashResult.put(formatItem.fieldName, ConverterUtil.dataToInt(subData));
            } else if (formatItem.dataType == BLEDataParseFormat.DataType.BOOLEAN) {
                hashResult.put(formatItem.fieldName, ConverterUtil.dataToBool(subData));
            } else if (formatItem.dataType == BLEDataParseFormat.DataType.DATA) {
                hashResult.put(formatItem.fieldName, ConverterUtil.bytesToHexString(subData));
            } else {
                hashResult.put(formatItem.fieldName, "");
            }

            if (removeLengthField) {
                hashResult.remove(formatItem.useLengthFromField);
            }

            currentIndex = endIndex;
        }

        return hashResult;
    }

}
