package com.armongate.mobilepasssdk.constant;

public class PacketHeaders {

    public static final byte PLATFORM_ANDROID = (byte)0xF0;

    public interface PROTOCOLV2 {

        interface COMMON {
            byte FAILURE = 0x20;
            byte SUCCESS = 0x21;
        }

        interface GROUP {
            byte AUTH = 0x01;
        }

        interface AUTH {
            byte PUBLICKEY_CHALLENGE    = 0x01;
            byte CHALLENGE_RESULT       = 0x03;
            byte DIRECTION_CHALLENGE    = 0x05;
        }

        interface FAILURE_REASON {
            byte SUCCESS            = 0x00;
            byte NETWORK_ERROR      = 0x01;
            byte CHALLENGE_FAIL     = 0x02;
            byte INVALID_CHALLENGE  = 0x03;
            byte USER_NOT_FOUND     = 0x04;
            byte DEVICE_ERROR       = 0x05;
        }
    }
}
