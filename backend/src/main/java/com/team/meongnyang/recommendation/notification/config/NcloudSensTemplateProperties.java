package com.team.meongnyang.recommendation.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ncloud.sens.template")
@Getter
@Setter
public class NcloudSensTemplateProperties {

  private String cold;
  private String hot;
  private String rain;
  private String good;
  private String defaultCode; // default는 예약어라 추천
}