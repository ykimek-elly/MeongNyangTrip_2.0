package com.team.meongnyang.recommendation.notification.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class NcloudSignatureUtil {

    private NcloudSignatureUtil() {
    }

    public static String createSignature(String method, String urlPath, String timestamp, String accessKey, String secretKey) {
        try {
            String message = method + " " + urlPath + "\n" + timestamp + "\n" + accessKey;
            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Ncloud SENS 서명 생성에 실패했습니다.", e);
        }
    }
}
