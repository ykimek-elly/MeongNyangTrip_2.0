package com.team.meongnyang.recommendation.notification.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NcloudSignatureUtilTest {

    @Test
    @DisplayName("Ncloud SENS 서명을 규격대로 생성한다")
    void createSignature() {
        String signature = NcloudSignatureUtil.createSignature(
                "POST",
                "/alimtalk/v2/services/test-service/messages",
                "1700000000000",
                "test-access-key",
                "test-secret-key"
        );

        assertThat(signature).isEqualTo("ObaGREGyxBtgxukIHwduaSHPz0OUsQRs2fZxF+nLS9c=");
    }
}
