package com.team.meongnyang.user.controller;

import com.team.meongnyang.common.ApiResponse;
import com.team.meongnyang.user.dto.PetRequestDto;
import com.team.meongnyang.user.dto.PetResponseDto;
import com.team.meongnyang.user.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 반려동물 CRUD API
 *
 * TODO: JWT 완성 후 @RequestHeader("X-User-Id") → @AuthenticationPrincipal 로 교체 예정
 */
@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    /** GET /api/v1/pets → 내 반려동물 목록 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PetResponseDto>>> getPets(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(
                ApiResponse.success("반려동물 목록 조회 성공", petService.getPetsByUserId(userId)));
    }

    /** POST /api/v1/pets → 반려동물 등록 */
    @PostMapping
    public ResponseEntity<ApiResponse<PetResponseDto>> addPet(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PetRequestDto request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("반려동물 등록 성공", petService.addPet(userId, request)));
    }

    /** PUT /api/v1/pets/{id} → 반려동물 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PetResponseDto>> updatePet(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PetRequestDto request) {
        return ResponseEntity.ok(
                ApiResponse.success("반려동물 수정 성공", petService.updatePet(id, userId, request)));
    }

    /** DELETE /api/v1/pets/{id} → 반려동물 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePet(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        petService.deletePet(id, userId);
        return ResponseEntity.ok(ApiResponse.success("반려동물 삭제 성공", null));
    }

    /** PATCH /api/v1/pets/{id}/representative → 대표 반려동물 설정 */
    @PatchMapping("/{id}/representative")
    public ResponseEntity<ApiResponse<PetResponseDto>> setRepresentative(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(
                ApiResponse.success("대표 반려동물 설정 성공", petService.setRepresentative(id, userId)));
    }
}
