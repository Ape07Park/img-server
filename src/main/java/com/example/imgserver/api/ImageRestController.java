package com.example.imgserver.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.imgserver.domain.dto.ImageUploadResponse;
import com.example.imgserver.service.ImageService;

@Slf4j
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageRestController {

	private final ImageService imageService;


	// POST http://{IP}:8080/api/v1/images?project=projectA
	// Body: form-data (Key: file, Value: [이미지파일])

	/**
	 * 이미지 업로드 API
	 * @param project 프로젝트 식별자
	 * @param file 업로드할 이미지 파일
	 * @return 업로드된 이미지 정보 (파일명, URL)
	 */
	@PostMapping
	public ResponseEntity<ImageUploadResponse> upload(
		@RequestParam("project") String project,
		@RequestParam("file") MultipartFile file) {

		log.info("이미지 업로드 요청: project={}, filename={}", project, file.getOriginalFilename());
		ImageUploadResponse response = imageService.uploadImage(project, file);
		log.info("이미지 업로드 성공: {}", response.getUrl());
		return ResponseEntity.ok(response);
	}

	/**
	 * 이미지 미리보기 API (브라우저에서 바로 볼 수 있도록)
	 * @param project 프로젝트 식별자
	 * @param year 연도
	 * @param month 월
	 * @param day 일
	 * @param filename 파일명
	 * @return 이미지 파일
	 */
	@GetMapping("/preview/{project}/{year}/{month}/{day}/{filename:.+}")
	public ResponseEntity<Resource> previewImage(
		@PathVariable String project,
		@PathVariable String year,
		@PathVariable String month,
		@PathVariable String day,
		@PathVariable String filename) {

		String datePath = year + "/" + month + "/" + day;
		log.info("이미지 미리보기 요청: project={}, datePath={}, filename={}", project, datePath, filename);

		Resource resource = imageService.loadImage(project, datePath, filename);
		String contentType = imageService.getContentType(resource);

		log.info("이미지 미리보기 성공: {}", filename);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(contentType))
			.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
			.body(resource);
	}

	/**
	 * 이미지 다운로드 API (파일로 다운로드)
	 * @param project 프로젝트 식별자
	 * @param year 연도
	 * @param month 월
	 * @param day 일
	 * @param filename 파일명
	 * @return 이미지 파일
	 */
	@GetMapping("/download/{project}/{year}/{month}/{day}/{filename:.+}")
	public ResponseEntity<Resource> downloadImage(
		@PathVariable String project,
		@PathVariable String year,
		@PathVariable String month,
		@PathVariable String day,
		@PathVariable String filename) {

		String datePath = year + "/" + month + "/" + day;
		log.info("이미지 다운로드 요청: project={}, datePath={}, filename={}", project, datePath, filename);

		Resource resource = imageService.loadImage(project, datePath, filename);
		String contentType = imageService.getContentType(resource);

		log.info("이미지 다운로드 성공: {}", filename);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(contentType))
			.header("X-Accel-Redirect", datePath)
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
			.body(resource);
	}
}