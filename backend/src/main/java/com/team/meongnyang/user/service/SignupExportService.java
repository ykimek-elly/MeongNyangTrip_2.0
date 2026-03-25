package com.team.meongnyang.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team.meongnyang.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.format.DateTimeFormatter;

/**
 * 회원가입 완료 사용자 정보를 JSON 파일에 누적 저장.
 * AI 알림 서비스 개발 및 테스트용 데이터 수집 목적.
 * 비동기 처리로 회원가입 응답 지연 없음.
 */
@Slf4j
@Service
public class SignupExportService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Value("${app.signup.export-path:exports/registered_users.json}")
    private String exportPath;

    @Async
    public void export(User user) {
        try {
            File file = new File(exportPath);
            file.getParentFile().mkdirs();

            // 기존 파일 읽기 또는 빈 배열 초기화
            ArrayNode users;
            if (file.exists() && file.length() > 0) {
                users = (ArrayNode) MAPPER.readTree(file);
            } else {
                users = MAPPER.createArrayNode();
            }

            // 중복 방지 — 동일 email 이미 존재하면 스킵
            for (var node : users) {
                if (user.getEmail().equals(node.path("email").asText())) {
                    return;
                }
            }

            // 사용자 정보 노드 생성
            ObjectNode entry = MAPPER.createObjectNode();
            entry.put("userId", user.getUserId());
            entry.put("email", user.getEmail());
            entry.put("nickname", user.getNickname());
            entry.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
            entry.put("notificationEnabled", user.isNotificationEnabled());
            if (user.getRegDate() != null) {
                entry.put("signedUpAt", user.getRegDate().format(FORMATTER));
            }

            users.add(entry);
            MAPPER.writeValue(file, users);
            log.info("[SignupExport] 저장 완료 — userId={}, email={}", user.getUserId(), user.getEmail());

        } catch (Exception e) {
            log.warn("[SignupExport] JSON 저장 실패 (회원가입에는 영향 없음): {}", e.getMessage());
        }
    }
}
