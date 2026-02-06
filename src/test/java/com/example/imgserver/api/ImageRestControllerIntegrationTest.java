package com.example.imgserver.api;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ImageRestControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private static final String TEST_PROJECT = "test-project";
	private static String uploadedFileName;
	private static String uploadedDatePath;

	@Test
	@Order(1)
	@DisplayName("이미지 업로드 성공 테스트")
	void testUploadImage() throws Exception {
		// 테스트용 이미지 파일 생성 (1x1 PNG)
		byte[] pngContent = createTestPngImage();
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"test-image.png",
			"image/png",
			pngContent
		);

		String response = mockMvc.perform(multipart("/api/v1/images")
				.file(file)
				.param("project", TEST_PROJECT))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fileName").exists())
			.andExpect(jsonPath("$.url").exists())
			.andReturn()
			.getResponse()
			.getContentAsString();

		// 업로드된 파일 정보 추출 (다음 테스트를 위해)
		System.out.println("Upload Response: " + response);
	}

	@Test
	@Order(2)
	@DisplayName("잘못된 파일 형식 업로드 실패 테스트")
	void testUploadInvalidFileType() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"test.txt",
			"text/plain",
			"This is not an image".getBytes()
		);

		mockMvc.perform(multipart("/api/v1/images")
				.file(file)
				.param("project", TEST_PROJECT))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("지원하지 않는 파일 형식입니다. 허용된 확장자: .jpg, .jpeg, .png, .gif, .bmp, .webp"));
	}

	@Test
	@Order(3)
	@DisplayName("빈 파일 업로드 실패 테스트")
	void testUploadEmptyFile() throws Exception {
		MockMultipartFile emptyFile = new MockMultipartFile(
			"file",
			"empty.png",
			"image/png",
			new byte[0]
		);

		mockMvc.perform(multipart("/api/v1/images")
				.file(emptyFile)
				.param("project", TEST_PROJECT))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("파일이 비어있습니다."));
	}

	@Test
	@Order(4)
	@DisplayName("파일 크기 초과 테스트")
	void testUploadOversizedFile() throws Exception {
		// 21MB 크기의 파일 (최대 20MB 제한)
		byte[] largeContent = new byte[21 * 1024 * 1024];
		MockMultipartFile largeFile = new MockMultipartFile(
			"file",
			"large.png",
			"image/png",
			largeContent
		);

		mockMvc.perform(multipart("/api/v1/images")
				.file(largeFile)
				.param("project", TEST_PROJECT))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}

	/**
	 * 테스트용 1x1 픽셀 PNG 이미지 생성
	 * Base64로 인코딩된 최소 크기의 PNG
	 */
	private byte[] createTestPngImage() {
		// 1x1 빨간색 픽셀의 PNG 이미지 (Base64 디코딩)
		return new byte[] {
			(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
			0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
			0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
			0x08, 0x02, 0x00, 0x00, 0x00, (byte)0x90, 0x77, 0x53,
			(byte)0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
			0x08, (byte)0xD7, 0x63, (byte)0xF8, (byte)0xCF, (byte)0xC0, 0x00, 0x00,
			0x03, 0x01, 0x01, 0x00, 0x18, (byte)0xDD, (byte)0x8D, (byte)0xB4,
			0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
			(byte)0xAE, 0x42, 0x60, (byte)0x82
		};
	}
}
