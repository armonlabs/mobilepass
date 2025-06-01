package com.armongate.mobilepasssdk.util;

import android.text.TextUtils;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class QRCodeValidator {
    private static final Pattern QR_CODE_PATTERN = Pattern.compile(
        "https://(app|sdk).armongate.com/(rq|bd|o|s)/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(/[0-2])?$"
    );

    public static ValidationResult validate(String qrcodeContent) {
        if (TextUtils.isEmpty(qrcodeContent)) {
            return new ValidationResult(false, null, null, null, null, true);
        }

        Matcher matcher = QR_CODE_PATTERN.matcher(qrcodeContent);
        if (!matcher.matches()) {
            return new ValidationResult(false, null, null, null, null, false);
        }

        String prefix = matcher.group(2);
        String uuid = matcher.group(3);
        String direction = matcher.group(4);

        String parsedContent = (prefix != null ? prefix : "") + "/" + (uuid != null ? uuid : "");
        if (prefix != null && prefix.equals("rq")) {
            parsedContent += (direction != null ? direction : "");
        }

        return new ValidationResult(true, parsedContent, prefix, uuid, direction, false);
    }

    public static class ValidationResult {
        public final boolean isValid;
        public final String parsedContent;
        public final String prefix;
        public final String uuid;
        public final String direction;
        public final boolean isEmpty;

        public ValidationResult(boolean isValid, String parsedContent, String prefix, 
                String uuid, String direction, boolean isEmpty) {
            this.isValid = isValid;
            this.parsedContent = parsedContent;
            this.prefix = prefix;
            this.uuid = uuid;
            this.direction = direction;
            this.isEmpty = isEmpty;
        }
    }
} 