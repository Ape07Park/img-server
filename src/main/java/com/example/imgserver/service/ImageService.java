package com.example.imgserver.service;

import com.example.imgserver.domain.dto.ImageUploadResponse;
import com.example.imgserver.exception.InvalidFileException;
import com.example.imgserver.storage.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 이미지 관련 비즈니스 로직 담당.
 * <p>
 * 저장소 구현 세부 사항은 {@link StorageService} 에 위임하므로,
 * 로컬 파일시스템 ↔ MinIO 전환 시 이 클래스는 변경하지 않아도 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final StorageService storageService;

    @Value("${image.url-prefix}")
    private String urlPrefix;

    private static final List<String> ALLOWED_EXTENSIONS =
            Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    /**
     * 이미지 업로드 처리
     *
     * @param project 프로젝트 식별자 (예: "shop", "admin") — 폴더명이 됨
     * @param file    업로드할 파일
     * @return 업로드 결과 (UUID 파일명, 접근 URL)
     */
    public ImageUploadResponse uploadImage(String project, MultipartFile file) {
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        validateExtension(extension);

        String storeFileName = UUID.randomUUID() + extension;
        String datePath = LocalDate.now().toString().replace("-", "/");

        storageService.store(project, datePath, storeFileName, file);
        log.info("이미지 업로드 성공: project={}, path={}/{}", project, datePath, storeFileName);

        String accessUrl = urlPrefix + "/" + project + "/" + datePath + "/" + storeFileName;
        return new ImageUploadResponse(storeFileName, accessUrl);
    }

    /**
     * 이미지 파일 조회
     *
     * @param project  프로젝트 식별자
     * @param datePath 날짜 경로 (예: "2026/02/19")
     * @param filename 파일명
     * @return 이미지 리소스
     */
    public Resource loadImage(String project, String datePath, String filename) {
        Resource resource = storageService.load(project, datePath, filename);

        String contentType = storageService.probeContentType(resource);
        if (contentType == null || !contentType.startsWith("image/")) {
            log.error("이미지 파일이 아님: {}", filename);
            throw new InvalidFileException("이미지 파일이 아닙니다: " + filename);
        }

        log.info("이미지 조회 성공: project={}, datePath={}, filename={}", project, datePath, filename);
        return resource;
    }

    /**
     * 리소스의 Content-Type 반환 (기본값: image/jpeg)
     *
     * @param resource 대상 리소스
     * @return Content-Type 문자열
     */
    public String getContentType(Resource resource) {
        String contentType = storageService.probeContentType(resource);
        if (contentType == null) {
            log.warn("Content-Type 감지 실패, 기본값(image/jpeg) 사용");
            return "image/jpeg";
        }
        return contentType;
    }

    // ─── 유효성 검사 ────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("파일이 비어있습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("파일 크기가 너무 큽니다. 최대 20MB까지 업로드 가능합니다.");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new InvalidFileException("파일명이 올바르지 않습니다.");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new InvalidFileException("파일 확장자가 없습니다.");
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private void validateExtension(String extension) {
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                    "지원하지 않는 파일 형식입니다. 허용된 확장자: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }
}
