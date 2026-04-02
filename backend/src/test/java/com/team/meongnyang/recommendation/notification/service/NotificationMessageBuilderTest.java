package com.team.meongnyang.recommendation.notification.service;

import com.team.meongnyang.place.entity.Place;
import com.team.meongnyang.recommendation.notification.config.NcloudSensProperties;
import com.team.meongnyang.recommendation.notification.dto.NotificationRequest;
import com.team.meongnyang.recommendation.notification.template.KakaoTemplateManager;
import com.team.meongnyang.user.entity.Pet;
import com.team.meongnyang.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageBuilderTest {

    @Test
    @DisplayName("날씨별 template code와 content를 함께 생성한다")
    void buildRequest() {
        NcloudSensProperties properties = createProperties();
        NotificationMessageBuilder builder = new NotificationMessageBuilder(
                new KakaoTemplateManager(properties),
                properties
        );

        NotificationRequest request = builder.buildRequest(
                createUser("010-1234-5678"),
                createPet("초코"),
                createPlace(100L, "수원 산책길"),
                "산책하기 좋은 코스예요.",
                "SUNNY"
        );

        NotificationRequest.Message message = request.getMessages().get(0);

        assertThat(request.getTemplateCode()).isEqualTo("TPL_SUNNY");
        assertThat(request.getPlusFriendId()).isEqualTo("@meongnyangtrip");
        assertThat(message.getTo()).isEqualTo("01012345678");
        assertThat(message.getTemplateParameter()).containsEntry("petName", "초코");
        assertThat(message.getTemplateParameter()).containsEntry("placeName", "수원 산책길");
        assertThat(message.getTemplateParameter()).containsEntry("comment", "산책하기 좋은 코스예요.");
        assertThat(message.getContent()).contains("수원 산책길");
    }

    @Test
    @DisplayName("comment 줄바꿈을 공백으로 정리하고 rain template을 사용한다")
    void buildRequestWithRainTemplateAndNormalizedComment() {
        NcloudSensProperties properties = createProperties();
        NotificationMessageBuilder builder = new NotificationMessageBuilder(
                new KakaoTemplateManager(properties),
                properties
        );

        NotificationRequest request = builder.buildRequest(
                createUser("821012345678"),
                createPet("보리"),
                createPlace(3L, "실내 카페"),
                "비 오는 날\n머물기 좋은 장소예요.",
                "RAINY"
        );

        NotificationRequest.Message message = request.getMessages().get(0);

        assertThat(request.getTemplateCode()).isEqualTo("TPL_RAIN");
        assertThat(message.getTemplateParameter().get("comment")).isEqualTo("비 오는 날 머물기 좋은 장소예요.");
    }

    @Test
    @DisplayName("날씨별 template code가 비면 default code로 fallback한다")
    void buildRequestFallsBackToDefaultTemplateCodeWhenWeatherTemplateCodeMissing() {
        NcloudSensProperties properties = createProperties();
        properties.getTemplate().setSunny(null);

        NotificationMessageBuilder builder = new NotificationMessageBuilder(
                new KakaoTemplateManager(properties),
                properties
        );

        NotificationRequest request = builder.buildRequest(
                createUser("010-1234-5678"),
                createPet("초코"),
                createPlace(100L, "수원 산책길"),
                "산책하기 좋은 코스예요.",
                "SUNNY"
        );

        assertThat(request.getTemplateCode()).isEqualTo("TPL_DEFAULT");
    }

    private NcloudSensProperties createProperties() {
        NcloudSensProperties properties = new NcloudSensProperties();
        properties.setPlusFriendId("@meongnyangtrip");

        NcloudSensProperties.Template template = new NcloudSensProperties.Template();
        template.setSunny("TPL_SUNNY");
        template.setRain("TPL_RAIN");
        template.setCloudy("TPL_CLOUDY");
        template.setHot("TPL_HOT");
        template.setCold("TPL_COLD");
        template.setDefaultCode("TPL_DEFAULT");
        properties.setTemplate(template);
        return properties;
    }

    private User createUser(String phoneNumber) {
        return User.builder()
                .userId(1L)
                .email("test@example.com")
                .nickname("tester")
                .phoneNumber(phoneNumber)
                .build();
    }

    private Pet createPet(String petName) {
        return Pet.builder()
                .petId(1L)
                .user(createUser("01012345678"))
                .petName(petName)
                .petType(Pet.PetType.values()[0])
                .petBreed("breed")
                .petGender(Pet.PetGender.values()[0])
                .petSize(Pet.PetSize.SMALL)
                .petAge(3)
                .petWeight(BigDecimal.valueOf(3.2))
                .petActivity(Pet.PetActivity.NORMAL)
                .build();
    }

    private Place createPlace(Long placeId, String title) {
        return Place.builder()
                .id(placeId)
                .title(title)
                .address("경기도 수원시")
                .latitude(37.27)
                .longitude(127.01)
                .category("PLACE")
                .build();
    }
}
