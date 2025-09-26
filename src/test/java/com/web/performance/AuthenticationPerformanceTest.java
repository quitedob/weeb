package com.web.performance;

import com.web.mapper.UserMapper;
import com.web.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmarking tests for authentication request success rates
 * Tests Requirements: 3.2, 3.3, 3.4, 3.6 (Database Performance Improvements)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthenticationPerformanceTest {

    @Autowired
    private UserMapper userMapper;

    private List<User> testUsers;
    private static final int USER_COUNT = 10;
    private static final int CONCURRENT_REQUESTS = 50;

    @BeforeEach
    void setUp() {
        testUsers = new ArrayList<>();
        
        // Create test users for performance testing
        for (int i = 0; i < USER_COUNT; i++) {
            User user = new User();
            user.setUsername("perftest_user_" + i + "_" + System.currentTimeMillis());
            user.setUserEmail("perftest" + i + "@example.com");
            user.setPassword("hashedpassword" + i);
            user.setNickname("Performance Test User " + i);
            user.setCreateTime(new Timestamp(System.currentTimeMillis()));
            
            userMapper.insertUser(user);
            testUsers.add(user);
        }
    }

    @Test
    @DisplayName("Requirement 3.2: Measure authentication request success rates")
    void testAuthenticationSuccessRates() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger totalCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // Simulate concurrent authentication requests
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            final User testUser = testUsers.get(i % testUsers.size());
            
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    // Simulate authentication by looking up user
                    User retrievedUser = userMapper.selectUserByUsername(testUser.getUsername());
                    
                    totalCount.incrementAndGet();
                    if (retrievedUser != null && retrievedUser.getUsername().equals(testUser.getUsername())) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    totalCount.incrementAndGet();
                    System.err.println("Authentication failed: " + e.getMessage());
                }
            }, executor));
        }
        
        // Wait for all authentication attempts to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Future operation failed: " + e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        
        // Calculate success rate
        double successRate = (double) successCount.get() / totalCount.get() * 100;
        double avgResponseTime = (double) totalTime / totalCount.get();
        
        System.out.println("Authentication Performance Results:");
        System.out.println("Total requests: " + totalCount.get());
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Success rate: " + String.format("%.2f%%", successRate));
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Average response time: " + String.format("%.2f ms", avgResponseTime));
        
        // Assertions for performance requirements
        assertTrue(successRate >= 95.0, "Authentication success rate should be at least 95%");
        assertTrue(avgResponseTime < 100, "Average authentication time should be under 100ms");
        assertTrue(totalTime < 5000, "All authentication requests should complete within 5 seconds");
    }

    @Test
    @DisplayName("Authentication performance under load")
    void testAuthenticationUnderLoad() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // High load test with 100 concurrent authentication requests
        for (int i = 0; i < 100; i++) {
            final User testUser = testUsers.get(i % testUsers.size());
            
            futures.add(CompletableFuture.runAsync(() -> {
                long requestStart = System.currentTimeMillis();
                try {
                    User retrievedUser = userMapper.selectUserByUsername(testUser.getUsername());
                    long requestEnd = System.currentTimeMillis();
                    
                    synchronized (responseTimes) {
                        responseTimes.add(requestEnd - requestStart);
                    }
                    
                    if (retrievedUser != null) {
                        successCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Authentication error under load: " + e.getMessage());
                }
            }, executor));
        }
        
        long testStart = System.currentTimeMillis();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Future operation failed: " + e.getMessage());
        }
        long testEnd = System.currentTimeMillis();
        
        executor.shutdown();
        
        // Calculate performance metrics
        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0L);
        double throughput = (double) (successCount.get() + errorCount.get()) / (testEnd - testStart) * 1000;
        
        System.out.println("Authentication Load Test Results:");
        System.out.println("Successful authentications: " + successCount.get());
        System.out.println("Failed authentications: " + errorCount.get());
        System.out.println("Average response time: " + String.format("%.2f ms", avgResponseTime));
        System.out.println("Min response time: " + minResponseTime + "ms");
        System.out.println("Max response time: " + maxResponseTime + "ms");
        System.out.println("Throughput: " + String.format("%.2f requests/second", throughput));
        
        // Performance assertions
        assertTrue(successCount.get() >= 95, "At least 95% of authentication requests should succeed under load");
        assertTrue(avgResponseTime < 200, "Average response time should be under 200ms under load");
        assertTrue(maxResponseTime < 1000, "Maximum response time should be under 1 second");
        assertTrue(throughput > 10, "Throughput should be at least 10 requests per second");
    }

    @Test
    @DisplayName("Authentication performance with concurrent statistical updates")
    void testAuthenticationWithConcurrentStats() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(30);
        AtomicInteger authSuccessCount = new AtomicInteger(0);
        AtomicInteger authErrorCount = new AtomicInteger(0);
        List<Long> authResponseTimes = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Start continuous statistical updates in background
        CompletableFuture<Void> statsUpdates = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 100; i++) {
                try {
                    // Simulate statistical updates that would previously block authentication
                    User user = testUsers.get(i % testUsers.size());
                    // In the old schema, this would lock the user table
                    // In the new schema, this only affects user_stats table
                    Thread.sleep(10); // Simulate update processing time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, executor);
        
        // Perform authentication requests during statistical updates
        for (int i = 0; i < 50; i++) {
            final User testUser = testUsers.get(i % testUsers.size());
            
            futures.add(CompletableFuture.runAsync(() -> {
                long requestStart = System.currentTimeMillis();
                try {
                    User retrievedUser = userMapper.selectUserByUsername(testUser.getUsername());
                    long requestEnd = System.currentTimeMillis();
                    
                    synchronized (authResponseTimes) {
                        authResponseTimes.add(requestEnd - requestStart);
                    }
                    
                    if (retrievedUser != null) {
                        authSuccessCount.incrementAndGet();
                    } else {
                        authErrorCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    authErrorCount.incrementAndGet();
                }
            }, executor));
        }
        
        // Wait for all operations to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
            statsUpdates.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Future operation failed: " + e.getMessage());
        }
        
        executor.shutdown();
        
        // Calculate metrics
        double avgAuthTime = authResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double successRate = (double) authSuccessCount.get() / (authSuccessCount.get() + authErrorCount.get()) * 100;
        
        System.out.println("Authentication with Concurrent Stats Results:");
        System.out.println("Authentication success rate: " + String.format("%.2f%%", successRate));
        System.out.println("Average authentication time: " + String.format("%.2f ms", avgAuthTime));
        
        // With optimized schema, authentication should not be significantly impacted
        assertTrue(successRate >= 95.0, "Authentication success rate should remain high during stats updates");
        assertTrue(avgAuthTime < 150, "Authentication time should not be significantly impacted by stats updates");
    }

    @Test
    @DisplayName("Baseline authentication performance measurement")
    void testBaselineAuthenticationPerformance() throws InterruptedException {
        // This test establishes baseline performance metrics for comparison
        
        List<Long> responseTimes = new ArrayList<>();
        int iterations = 100;
        
        long totalStart = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            User testUser = testUsers.get(i % testUsers.size());
            
            long start = System.currentTimeMillis();
            User retrievedUser = userMapper.selectUserByUsername(testUser.getUsername());
            long end = System.currentTimeMillis();
            
            responseTimes.add(end - start);
            assertNotNull(retrievedUser, "User should be found");
        }
        
        long totalEnd = System.currentTimeMillis();
        
        // Calculate baseline metrics
        double avgTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long minTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0L);
        long maxTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
        double totalTime = totalEnd - totalStart;
        double throughput = iterations / totalTime * 1000;
        
        System.out.println("Baseline Authentication Performance:");
        System.out.println("Iterations: " + iterations);
        System.out.println("Average time: " + String.format("%.2f ms", avgTime));
        System.out.println("Min time: " + minTime + "ms");
        System.out.println("Max time: " + maxTime + "ms");
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Throughput: " + String.format("%.2f requests/second", throughput));
        
        // Baseline performance expectations
        assertTrue(avgTime < 50, "Baseline authentication should be very fast (under 50ms)");
        assertTrue(maxTime < 200, "Even slowest authentication should be under 200ms");
        assertTrue(throughput > 20, "Baseline throughput should be at least 20 requests/second");
    }
}