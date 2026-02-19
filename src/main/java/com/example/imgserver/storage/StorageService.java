package com.example.imgserver.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장소 추상화 인터페이스.
 * <p>
 * 현재: {@link LocalStorageService} (로컬 파일시스템)
 * 추후: MinioStorageService (MinIO 객체 스토리지)
 * <p>
 * 전환 방법: application.yaml 의 {@code image.storage} 값을 변경
 * <pre>
 *   image.storage: local  → LocalStorageService
 *   image.storage: minio  → MinioStorageService
 * </pre>
 */
public interface StorageService {

    /**
     * 파일을 저장소에 저장한다.
     * 실패 시 {@link com.example.imgserver.exception.ImageUploadException} (unchecked) 을 던진다.
     *
     * @param project   프로젝트 식별자 (예: "shop", "admin")
     * @param datePath  날짜 경로    (예: "2026/02/19")
     * @param filename  저장할 파일명 (예: "550e84...jpg")
     * @param file      업로드 파일
     */
    void store(String project, String datePath, String filename, MultipartFile file);

    /**
     * 저장소에서 파일을 불러온다.
     * 파일이 없거나 읽을 수 없으면 {@link com.example.imgserver.exception.ImageNotFoundException} 을 던진다.
     *
     * @param project  프로젝트 식별자
     * @param datePath 날짜 경로
     * @param filename 파일명
     * @return 파일 리소스
     */
    Resource load(String project, String datePath, String filename);

    /**
     * 리소스의 Content-Type 을 반환한다.
     * 판별 불가 시 {@code null} 을 반환하며, 호출자가 기본값을 처리한다.
     *
     * @param resource 대상 리소스
     * @return MIME 타입 문자열 (예: "image/jpeg"), 판별 불가 시 null
     */
    String probeContentType(Resource resource);
}
