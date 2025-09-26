package com.web.integration;

import com.web.common.ApiResponse;
import com.web.model.FileRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * New Features Integration Test
 * Test newly added comment, file management, and follow system features
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebMvc
public class NewFeaturesIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Test file management functionality
     */
    @Test
    public void testFileManagement() {
        String baseUrl = getBaseUrl();

        // 1. Test file upload
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Create temporary test file
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("test", ".txt");
            Files.write(tempFile, "This is a test file".getBytes());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFile.toFile()));
            body.add("isPublic", false);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/files/upload",
                requestEntity,
                ApiResponse.class
            );

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(200, response.getBody().getCode());

            System.out.println("File upload test passed");

            // 2. Test getting file list
            ResponseEntity<ApiResponse> listResponse = restTemplate.getForEntity(
                baseUrl + "/api/files/my?page=1&size=10",
                ApiResponse.class
            );

            assertEquals(HttpStatus.OK, listResponse.getStatusCode());
            assertNotNull(listResponse.getBody());
            assertEquals(200, listResponse.getBody().getCode());

            System.out.println("File list retrieval test passed");

        } catch (IOException e) {
            fail("File operation failed: " + e.getMessage());
        } finally {
            // Clean up temporary file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    /**
     * Test chat API alignment
     */
    @Test
    public void testChatAPIAlignment() {
        String baseUrl = getBaseUrl();

        // 1. Test getting chat list
        ResponseEntity<ApiResponse> chatListResponse = restTemplate.getForEntity(
            baseUrl + "/api/chats",
            ApiResponse.class
        );

        assertEquals(HttpStatus.OK, chatListResponse.getStatusCode());
        assertNotNull(chatListResponse.getBody());
        assertEquals(200, chatListResponse.getBody().getCode());

        System.out.println("Chat list API test passed");

        // 2. Test creating new chat
        Map<String, Long> createChatRequest = new HashMap<>();
        createChatRequest.put("targetId", 2L);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Long>> createChatEntity = new HttpEntity<>(createChatRequest, headers);

        ResponseEntity<ApiResponse> createChatResponse = restTemplate.postForEntity(
            baseUrl + "/api/chats",
            createChatEntity,
            ApiResponse.class
        );

        assertEquals(HttpStatus.OK, createChatResponse.getStatusCode());
        assertNotNull(createChatResponse.getBody());
        assertEquals(200, createChatResponse.getBody().getCode());

        System.out.println("Create chat API test passed");
    }

    /**
     * Test integrated features
     */
    @Test
    public void testIntegratedFeatures() {
        String baseUrl = getBaseUrl();

        // Test file upload success
        testFileManagement();

        // Test chat API works properly
        testChatAPIAlignment();

        System.out.println("All new features integration tests passed!");
    }
}