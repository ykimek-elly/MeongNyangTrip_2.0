package com.team.meongnyang.common;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class S3UploadController {

    private final S3UploadService s3UploadService;

    @PostMapping("/image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "파일이 없어요."));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "이미지 파일만 업로드 가능해요."));
        }

        String url = s3UploadService.upload(file, "lounge");
        return ResponseEntity.ok(ApiResponse.success("업로드 성공", Map.of("url", url)));
    }
}