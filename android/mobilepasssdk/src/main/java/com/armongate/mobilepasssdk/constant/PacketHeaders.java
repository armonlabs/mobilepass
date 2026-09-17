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
            byte PUBLICKEY_CHALLENGE                    = 0x01;
            byte CHALLENGE_RESULT                       = 0x03;
            byte DIRECTION_CHALLENGE                    = 0x05;
            byte MACFIT_CHALLENGE                       = 0x06;
            byte MACFIT_CHALLENGE_WITH_INSTALLATIONID   = 0x07;
            /**
             * Same body as MACFIT_CHALLENGE_WITH_INSTALLATIONID, sent only to
             * declare that this client understands the extended result.
             *
             * Must never be sent speculatively: firmware that does not know this
             * type parses the request as a direction challenge, reads every field
             * from the wrong offset and fails the pass.
             */
            byte MACFIT_CHALLENGE_WITH_RESULTCODE       = 0x08;
            /** Extended challenge result, answered only to MACFIT_CHALLENGE_WITH_RESULTCODE */
            byte CHALLENGE_RESULT_WITH_CODE             = 0x09;
        }

        /**
         * Capability bits appended to the public key challenge by newer firmware.
         *
         * The byte may be absent altogether. Unknown bits are ignored rather than
         * treated as an invalid packet, so that further capabilities can be added
         * without an SDK update.
         */
        interface CAPABILITY {
            int RESULT_CODE = 0x01;
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
