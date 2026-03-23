package com.team.meongnyang.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * 이메일 발송 서비스 (Gmail SMTP)
 * - 아이디 찾기: 이메일 마스킹 후 발송
 * - 임시 비밀번호 발송: 8자리 랜덤 비밀번호 발송
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    @Autowired
    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /** 아이디(이메일) 찾기 결과 발송 */
    public void sendFindIdEmail(String toEmail, String maskedEmail) {
        String subject = "[멍냥트립] 아이디 찾기 결과";
        String body = """
                <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
                  <h2 style="color: #f97316;">멍냥트립 🐾</h2>
                  <p>안녕하세요! 아이디 찾기 결과를 안내드립니다.</p>
                  <div style="background: #fff7ed; border-radius: 12px; padding: 20px; margin: 16px 0;">
                    <p style="margin: 0; font-size: 16px;">가입하신 이메일: <strong>%s</strong></p>
                  </div>
                  <p style="color: #9ca3af; font-size: 12px;">본인이 요청하지 않은 경우 이 메일을 무시하세요.</p>
                </div>
                """.formatted(maskedEmail);
        send(toEmail, subject, body);
    }

    /** 임시 비밀번호 발송 */
    public void sendTempPasswordEmail(String toEmail, String tempPassword) {
        String subject = "[멍냥트립] 임시 비밀번호 발급";
        String body = """
                <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
                  <h2 style="color: #f97316;">멍냥트립 🐾</h2>
                  <p>임시 비밀번호를 발급해 드렸습니다. 로그인 후 반드시 비밀번호를 변경해 주세요.</p>
                  <div style="background: #fff7ed; border-radius: 12px; padding: 20px; margin: 16px 0;">
                    <p style="margin: 0; font-size: 20px; letter-spacing: 4px; font-weight: bold;">%s</p>
                  </div>
                  <p style="color: #9ca3af; font-size: 12px;">본인이 요청하지 않은 경우 즉시 비밀번호를 변경하세요.</p>
                </div>
                """.formatted(tempPassword);
        send(toEmail, subject, body);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("[EmailService] 메일 발송 실패 to={}: {}", to, e.getMessage());
            throw new RuntimeException("메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
