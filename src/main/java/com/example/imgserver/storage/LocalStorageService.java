package com.example.imgserver.storage;

import com.example.imgserver.exception.ImageNotFoundException;
import com.example.imgserver.exception.ImageUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 로컬 파일시스템 기반 저장소 구현체.
 * <p>
 * {@code image.storage=local} (기본값) 일 때 활성화된다.
 * MinIO 로 전환 시 이 클래스 대신 MinioStorageService 가 주입된다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "image.storage", havingValue = "local", matchIfMissing = true)
//      동작 원리: 설정 파일에 지정한 속성이 존재하거나, 특정 값을 가질 때만 해당 Bean을 컨테이너에 등록
//     주요 속성:
//     name 또는 value: 체크할 프로퍼티의 이름 (필수).
//     havingValue: 프로퍼티가 가져야 할 비교 대상 값.
//     matchIfMissing: 프로퍼티가 설정 파일에 없을 때 기본으로 조건 충족 여부 (기본값 false)
public class LocalStorageService implements StorageService {

    @Value("${image.dir}")
    private String uploadDir;

    @Override
    public void store(String project, String datePath, String filename, MultipartFile file) {
        String dirPath = uploadDir + "/" + project + "/" + datePath;
        File folder = new File(dirPath);

        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (!created) {
                log.error("폴더 생성 실패: {}", dirPath);
                throw new ImageUploadException("이미지 저장 폴더를 생성할 수 없습니다.");
            }
            log.info("폴더 생성 완료: {}", dirPath);
        }

        Path fullPath = Paths.get(dirPath, filename);
        try {
            file.transferTo(fullPath);
            log.info("파일 저장 완료: {}", fullPath);
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", fullPath, e);
            throw new ImageUploadException("이미지 저장 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Resource load(String project, String datePath, String filename) {
        Path filePath = Paths.get(uploadDir, project, datePath, filename);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.error("이미지를 찾을 수 없거나 읽을 수 없음: {}", filePath);
                throw new ImageNotFoundException("이미지를 찾을 수 없습니다: " + filename);
            }
            return resource;
        } catch (MalformedURLException e) {
            log.error("잘못된 파일 경로: {}", filePath, e);
            throw new ImageNotFoundException("이미지를 찾을 수 없습니다: " + filename);
        }
    }

    @Override
    public String probeContentType(Resource resource) {
        try {
            return Files.probeContentType(resource.getFile().toPath());
        } catch (IOException e) {
            log.warn("Content-Type 감지 실패: {}", resource.getFilename());
            return null;
        }
    }
}
