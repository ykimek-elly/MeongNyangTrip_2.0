package com.team.meongnyang.recommendation.service;

import org.springframework.stereotype.Component;

@Component
public class RecommendationAiResponseParser {

    private static final String NOTIFICATION_SUMMARY_HEADER = "[알림요약]";

    public String extractNotificationSummary(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return null;
        }

        int summaryHeaderIndex = aiResponse.indexOf(NOTIFICATION_SUMMARY_HEADER);
        if (summaryHeaderIndex < 0) {
            return null;
        }

        String summarySection = aiResponse.substring(summaryHeaderIndex + NOTIFICATION_SUMMARY_HEADER.length()).trim();
        if (summarySection.isBlank()) {
            return null;
        }

        int nextSectionIndex = summarySection.indexOf("\n[");
        if (nextSectionIndex >= 0) {
            summarySection = summarySection.substring(0, nextSectionIndex).trim();
        }

        String firstLine = summarySection.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse(null);

        if (firstLine == null || firstLine.isBlank()) {
            return null;
        }

        return firstLine.replaceFirst("^[-*\\s]*", "").trim();
    }

    public String extractRecommendationDescription(String aiResponse) {
        if (aiResponse == null || aiResponse.isBlank()) {
            return null;
        }

        int summaryHeaderIndex = aiResponse.indexOf(NOTIFICATION_SUMMARY_HEADER);
        String descriptionSection = summaryHeaderIndex >= 0
                ? aiResponse.substring(0, summaryHeaderIndex).trim()
                : aiResponse.trim();

        if (descriptionSection.isBlank()) {
            return null;
        }

        descriptionSection = stripLeadingSectionHeader(descriptionSection).trim();
        return descriptionSection.isBlank() ? null : descriptionSection;
    }

    public String defaultDescription(String notificationSummary) {
        return notificationSummary == null ? "" : notificationSummary;
    }

    private String stripLeadingSectionHeader(String text) {
        if (!text.startsWith("[")) {
            return text;
        }

        int headerEndIndex = text.indexOf(']');
        if (headerEndIndex < 0) {
            return text;
        }

        return text.substring(headerEndIndex + 1).trim();
    }
}
