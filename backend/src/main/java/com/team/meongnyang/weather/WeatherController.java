package com.team.meongnyang.weather;

import com.team.meongnyang.weather.dto.WeatherContext;
import com.team.meongnyang.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WeatherController {

  private final WeatherService weatherService;

  @GetMapping("/weather/test")
  public void test () {
    WeatherContext result = weatherService.getWeather(60 , 127);
    System.out.println(result.toString());
  }
}


