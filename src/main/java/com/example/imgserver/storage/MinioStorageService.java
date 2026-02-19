package com.example.imgserver.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * MinIO 객체 스토리지 기반 저장소 구현체 (미구현 스텁).
 * <p>
 * 전환 방법:
 * <pre>
 *   # application-prod.yaml
 *   image:
 *     storage: minio
 *     minio:
 *       endpoint: http://minio-host:9000
 *       access-key: ${MINIO_ACCESS_KEY}
 *       secret-key: ${MINIO_SECRET_KEY}
 *       bucket: images
 * </pre>
 * <p>
 * 구현 시 build.gradle 에 의존성 추가:
 * <pre>
 *   implementation 'io.minio:minio:8.5.x'
 * </pre>
 */
@Service
@ConditionalOnProperty(name = "image.storage", havingValue = "minio")
public class MinioStorageService implements StorageService {

    // TODO: MinioClient 주입 후 구현
    // @Value("${image.minio.bucket}")
    // private String bucket;
    // private final MinioClient minioClient;

    @Override
    public void store(String project, String datePath, String filename, MultipartFile file) {
        // TODO: minioClient.putObject(PutObjectArgs.builder()
        //           .bucket(bucket)
        //           .object(project + "/" + datePath + "/" + filename)
        //           .stream(file.getInputStream(), file.getSize(), -1)
        //           .contentType(file.getContentType())
        //           .build());
        throw new UnsupportedOperationException("MinIO 저장소는 아직 구현되지 않았습니다.");
    }

    @Override
    public Resource load(String project, String datePath, String filename) {
        // TODO: presigned URL 또는 InputStream 으로 Resource 반환
        throw new UnsupportedOperationException("MinIO 저장소는 아직 구현되지 않았습니다.");
    }

    @Override
    public String probeContentType(Resource resource) {
        // TODO: MinIO 오브젝트 메타데이터에서 Content-Type 조회
        throw new UnsupportedOperationException("MinIO 저장소는 아직 구현되지 않았습니다.");
    }
}
