package com.example.imgserver.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageUploadResponse {
	private String fileName;  // 저장된 파일명 (UUID)
	private String url;       // 접근 가능한 전체 URL
}
