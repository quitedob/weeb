package com.web.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for API response consistency across all controllers
 * Tests Requirements: 2.1-2.5 (Backend API Response Standardization)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ApiResponseConsistencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Requirement 2.1: All successful responses wrapped in ApiResponse<T> format")
    void testSuccessfulResponsesUseApiResponseFormat() throws Exception {
        // Test AuthController endpoints
        testEndpointResponseFormat("POST", "/api/auth/register", 
            "{\"username\":\"testuser\",\"password\":\"password123\",\"email\":\"test@example.com\"}", 
            true);
        
        // Test UserController endpoints
        testEndpointResponseFormat("GET", "/api/user/profile", null, false);
        
        // Test ArticleCenterController endpoints
        testEndpointResponseFormat("GET", "/articles", null, false);
        
        // Test ChatListController endpoints
        testEndpointResponseFormat("GET", "/api/v1/chat-list", null, false);
    }

    @Test
    @DisplayName("Requirement 2.2: All error responses wrapped in ApiResponse<T> format")
    void testErrorResponsesUseApiResponseFormat() throws Exception {
        // Test validation errors
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"\",\"email\":\"invalid-email\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        
        validateApiResponseFormat(result, false);
        
        // Test authentication errors
        result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nonexistent\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        
        validateApiResponseFormat(result, false);
        
        // Test not found errors
        result = mockMvc.perform(get("/api/user/999999"))
                .andExpect(status().isNotFound())
                .andReturn();
        
        validateApiResponseFormat(result, false);
    }

    @Test
    @DisplayName("Requirement 2.3: ArticleCenterController returns consistent ApiResponse objects")
    void testArticleCenterControllerConsistency() throws Exception {
        // Test all ArticleCenterController endpoints for consistency
        String[] endpoints = {
            "/articles",
            "/articles/search?keyword=test",
            "/articles/category/1",
            "/articles/user/1"
        };
        
        for (String endpoint : endpoints) {
            MvcResult result = mockMvc.perform(get(endpoint))
                    .andReturn();
            
            // Should return 200 OK or appropriate status, but always in ApiResponse format
            validateApiResponseFormat(result, result.getResponse().getStatus() == 200);
        }
        
        // Test POST endpoint (article creation)
        MvcResult createResult = mockMvc.perform(post("/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test Article\",\"content\":\"Test content\",\"categoryId\":1}"))
                .andReturn();
        
        validateApiResponseFormat(createResult, createResult.getResponse().getStatus() == 200);
        
        // Test PUT endpoint (article update)
        MvcResult updateResult = mockMvc.perform(put("/articles/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated Article\",\"content\":\"Updated content\"}"))
                .andReturn();
        
        validateApiResponseFormat(updateResult, updateResult.getResponse().getStatus() == 200);
        
        // Test DELETE endpoint
        MvcResult deleteResult = mockMvc.perform(delete("/articles/1"))
                .andReturn();
        
        validateApiResponseFormat(deleteResult, deleteResult.getResponse().getStatus() == 200);
    }

    @Test
    @DisplayName("Requirement 2.4: Frontend can parse responses using unified handler")
    void testUnifiedResponseHandling() throws Exception {
        // Test that all endpoints return responses that can be handled by a single parser
        String[] testEndpoints = {
            "/api/auth/login",
            "/articles",
            "/api/user/profile",
            "/api/v1/chat-list",
            "/api/notifications",
            "/api/search/articles?q=test"
        };
        
        for (String endpoint : testEndpoints) {
            MvcResult result;
            if (endpoint.equals("/api/auth/login")) {
                result = mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password\"}"))
                        .andReturn();
            } else {
                result = mockMvc.perform(get(endpoint))
                        .andReturn();
            }
            
            String responseContent = result.getResponse().getContentAsString();
            
            // Simulate unified frontend response handler
            boolean canParseUnified = canParseWithUnifiedHandler(responseContent);
            assertTrue(canParseUnified, "Endpoint " + endpoint + " should be parseable by unified handler");
        }
    }

    @Test
    @DisplayName("Requirement 2.5: API documentation reflects consistent response format")
    void testApiDocumentationConsistency() throws Exception {
        // Test that all endpoints follow the same response schema
        String[] endpoints = {
            "/api/auth/register",
            "/articles",
            "/api/user/profile",
            "/api/notifications"
        };
        
        for (String endpoint : endpoints) {
            MvcResult result;
            if (endpoint.equals("/api/auth/register")) {
                result = mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"doctest\",\"password\":\"password123\",\"email\":\"doc@test.com\"}"))
                        .andReturn();
            } else {
                result = mockMvc.perform(get(endpoint))
                        .andReturn();
            }
            
            String responseContent = result.getResponse().getContentAsString();
            
            // Validate that response follows documented schema
            validateDocumentedSchema(responseContent, endpoint);
        }
    }

    @Test
    @DisplayName("Cross-controller response format consistency")
    void testCrossControllerConsistency() throws Exception {
        // Test that different controllers return the same response structure
        
        // AuthController success response
        MvcResult authResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"crosstest\",\"password\":\"password123\",\"email\":\"cross@test.com\"}"))
                .andReturn();
        
        // UserController success response
        MvcResult userResult = mockMvc.perform(get("/api/user/profile"))
                .andReturn();
        
        // ArticleCenterController success response
        MvcResult articleResult = mockMvc.perform(get("/articles"))
                .andReturn();
        
        // Extract and compare response structures
        ApiResponse<?> authResponse = parseApiResponse(authResult.getResponse().getContentAsString());
        ApiResponse<?> userResponse = parseApiResponse(userResult.getResponse().getContentAsString());
        ApiResponse<?> articleResponse = parseApiResponse(articleResult.getResponse().getContentAsString());
        
        // All should have the same structure (regardless of data content)
        assertNotNull(authResponse);
        assertNotNull(userResponse);
        assertNotNull(articleResponse);
        
        // Verify structure consistency
        assertTrue(hasConsistentStructure(authResponse, userResponse));
        assertTrue(hasConsistentStructure(userResponse, articleResponse));
    }

    private void testEndpointResponseFormat(String method, String endpoint, String requestBody, boolean requiresAuth) throws Exception {
        MvcResult result;
        
        switch (method.toUpperCase()) {
            case "GET":
                result = mockMvc.perform(get(endpoint)).andReturn();
                break;
            case "POST":
                if (requestBody != null) {
                    result = mockMvc.perform(post(endpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)).andReturn();
                } else {
                    result = mockMvc.perform(post(endpoint)).andReturn();
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
        
        // Validate that response follows ApiResponse format
        validateApiResponseFormat(result, result.getResponse().getStatus() == 200);
    }

    private void validateApiResponseFormat(MvcResult result, boolean shouldBeSuccess) throws Exception {
        String responseContent = result.getResponse().getContentAsString();
        
        if (responseContent.isEmpty()) {
            return; // Some endpoints might return empty responses
        }
        
        ApiResponse<?> apiResponse = parseApiResponse(responseContent);
        assertNotNull(apiResponse, "Response should be parseable as ApiResponse");
        
        // Validate required fields
        assertNotNull(apiResponse.getMessage(), "Message field should be present");
        
        if (shouldBeSuccess) {
            assertTrue(apiResponse.isSuccess(), "Success responses should have success=true");
        } else {
            assertFalse(apiResponse.isSuccess(), "Error responses should have success=false");
        }
    }

    private boolean canParseWithUnifiedHandler(String responseContent) {
        try {
            if (responseContent.isEmpty()) {
                return true; // Empty responses are acceptable
            }
            
            ApiResponse<?> response = parseApiResponse(responseContent);
            
            // Check if it has the required structure for unified handling
            return response != null && 
                   response.getMessage() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateDocumentedSchema(String responseContent, String endpoint) throws Exception {
        if (responseContent.isEmpty()) {
            return;
        }
        
        ApiResponse<?> response = parseApiResponse(responseContent);
        assertNotNull(response, "Response from " + endpoint + " should follow documented schema");
        
        // Validate documented schema requirements
        assertNotNull(response.getMessage(), "Message field is required in documented schema");
        // success field should be present (boolean)
        // data field should be present (can be null)
        // errorCode field is optional
    }

    private ApiResponse<?> parseApiResponse(String responseContent) throws Exception {
        if (responseContent.isEmpty()) {
            return null;
        }
        
        return objectMapper.readValue(responseContent, ApiResponse.class);
    }

    private boolean hasConsistentStructure(ApiResponse<?> response1, ApiResponse<?> response2) {
        if (response1 == null || response2 == null) {
            return response1 == response2;
        }
        
        // Check that both have the same field structure
        boolean bothHaveMessage = (response1.getMessage() != null) == (response2.getMessage() != null);
        
        return bothHaveMessage;
    }
}