package com.example.imgserver.service;


import com.example.imgserver.exception.ImageNotFoundException;
import com.example.imgserver.exception.ImageUploadException;
import com.example.imgserver.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.example.imgserver.domain.dto.ImageUploadResponse;

@Slf4j
@Service
public class ImageService {

	@Value("${image.dir}")
	private String uploadDir;

	@Value("${image.url-prefix}")
	private String urlPrefix;

	private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp");
	private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

	/**
	 * 이미지 업로드 처리
	 * @param project 프로젝트 식별자 (예: "shop", "admin") - 폴더명이 됨
	 * @param file 업로드할 파일
	 * @return 업로드 결과 (URL 포함)
	 */
	public ImageUploadResponse uploadImage(String project, MultipartFile file) {

		// 파일 유효성 검사
		validateFile(file);

		// 원본 파일 확장자 추출 (예: .jpg, .png)
		String originalFilename = file.getOriginalFilename();
		String extension = extractExtension(originalFilename);

		// 확장자 검증
		validateExtension(extension);

		// 서버에 저장할 파일명 생성 (UUID 사용) -> 파일명 중복 방지
		String storeFileName = UUID.randomUUID() + extension;


		// 저장할 디렉토리 경로 생성 (/var/www/images/shop)
		String datePath = LocalDate.now().toString().replace("-", "/");
		String projectPath = uploadDir + "/" + project + "/" + datePath;
		File folder = new File(projectPath);

		// 해당 프로젝트 폴더가 없으면 생성
		if (!folder.exists()) {
			boolean created = folder.mkdirs();
			if(created) {
				log.info("폴더 생성 완료: {}", projectPath);
			} else {
				log.error("폴더 생성 실패: {}", projectPath);
				throw new ImageUploadException("이미지 저장 폴더를 생성할 수 없습니다.");
			}
		}

		// 파일 저장 (로컬 디스크로 전송)
		Path fullPath = Paths.get(projectPath, storeFileName);
		try {
			file.transferTo(fullPath);


			log.info("이미지 업로드 성공: {}", fullPath);
		} catch (IOException e) {
			log.error("파일 저장 실패: {}", fullPath, e);
			throw new ImageUploadException("이미지 저장 중 오류가 발생했습니다.", e);
		}

		// 접근 가능한 URL 생성 (예: http://1.2.3.4/images/shop/2026/02/05/uuid.jpg)
		// Nginx가 /images 경로를 /var/www/images로 매핑해줄 것이므로 아래와 같이 조합
		String accessUrl = urlPrefix + "/" + project + "/" + datePath + "/" + storeFileName;

		return new ImageUploadResponse(storeFileName, accessUrl);
	}

	/**
	 * 이미지 파일 조회 (날짜 경로 포함)
	 * @param project 프로젝트 식별자
	 * @param datePath 날짜 경로 (예: "2026/02/05")
	 * @param filename 파일명
	 * @return 이미지 리소스
	 */
	public Resource loadImage(String project, String datePath, String filename) {
		try {
			Path filePath = Paths.get(uploadDir, project, datePath, filename);
			Resource resource = new UrlResource(filePath.toUri());

			if (!resource.exists() || !resource.isReadable()) {
				log.error("이미지를 찾을 수 없거나 읽을 수 없음: {}", filePath);
				throw new ImageNotFoundException("이미지를 찾을 수 없습니다: " + filename);
			}

			// 파일이 실제 이미지인지 검증
			String contentType = Files.probeContentType(filePath);
			if (contentType == null || !contentType.startsWith("image/")) {
				log.error("이미지 파일이 아님: {}", filePath);
				throw new InvalidFileException("이미지 파일이 아닙니다: " + filename);
			}

			log.info("이미지 조회 성공: {}", filePath);

			// TODO 이미지 이름 원본으로 복원하기

			return resource;

		} catch (MalformedURLException e) {
			log.error("잘못된 파일 경로: {}", filename, e);
			throw new ImageNotFoundException("이미지를 찾을 수 없습니다: " + filename);
		} catch (IOException e) {
			log.error("파일 타입 확인 실패: {}", filename, e);
			throw new ImageNotFoundException("이미지를 조회할 수 없습니다: " + filename);
		}
	}

	/**
	 * 이미지 Content-Type 조회
	 * @param resource 이미지 리소스
	 * @return Content-Type (기본값: image/jpeg)
	 */
	public String getContentType(Resource resource) {
		try {
			String contentType = Files.probeContentType(resource.getFile().toPath());
			return contentType != null ? contentType : "image/jpeg";
		} catch (IOException e) {
			log.warn("Content-Type 감지 실패, 기본값 사용");
			return "image/jpeg";
		}
	}

	/**
	 * 파일 유효성 검사
	 */
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

	/**
	 * 확장자 추출
	 */
	private String extractExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			throw new InvalidFileException("파일 확장자가 없습니다.");
		}
		return filename.substring(filename.lastIndexOf(".")).toLowerCase();
	}

	/**
	 * 확장자 검증
	 */
	private void validateExtension(String extension) {
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new InvalidFileException(
				"지원하지 않는 파일 형식입니다. 허용된 확장자: " + String.join(", ", ALLOWED_EXTENSIONS)
			);
		}
	}
}