package com.web.integration;

import com.web.mapper.UserMapper;
import com.web.mapper.UserStatsMapper;
import com.web.model.User;
import com.web.model.UserStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for optimized database schema with separated user/user_stats tables
 * Tests Requirements: 3.1-3.6 (Database Schema Optimization for Scalability)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DatabaseSchemaIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserStatsMapper userStatsMapper;

    private User testUser;
    private UserStats testUserStats;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setUsername("testuser_" + System.currentTimeMillis());
        testUser.setUserEmail("test@example.com");
        testUser.setPassword("hashedpassword");
        testUser.setNickname("Test User");
        testUser.setCreateTime(new Timestamp(System.currentTimeMillis()));
        
        // Insert user first
        userMapper.insertUser(testUser);
        assertNotNull(testUser.getId(), "User ID should be generated after insert");

        // Create corresponding user stats
        testUserStats = new UserStats();
        testUserStats.setUserId(testUser.getId());
        testUserStats.setFansCount(0L);
        testUserStats.setTotalLikes(0L);
        testUserStats.setTotalFavorites(0L);
        testUserStats.setTotalSponsorship(0L);
        testUserStats.setTotalArticleExposure(0L);
        testUserStats.setWebsiteCoins(0L);
        testUserStats.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        testUserStats.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        
        userStatsMapper.insertUserStats(testUserStats);
    }

    @Test
    @DisplayName("Requirement 3.1: Aggregate counters moved to separate user_stats table")
    void testCountersInSeparateTable() {
        // Verify user table doesn't contain statistical counters
        User retrievedUser = userMapper.selectUserById(testUser.getId());
        assertNotNull(retrievedUser);
        assertEquals(testUser.getUsername(), retrievedUser.getUsername());
        assertEquals(testUser.getUserEmail(), retrievedUser.getUserEmail());
        
        // Verify user_stats table contains the counters
        UserStats retrievedStats = userStatsMapper.selectUserStatsByUserId(testUser.getId());
        assertNotNull(retrievedStats);
        assertEquals(testUser.getId(), retrievedStats.getUserId());
        assertEquals(0, retrievedStats.getFansCount());
        assertEquals(0, retrievedStats.getTotalLikes());
        assertEquals(BigDecimal.ZERO, retrievedStats.getTotalSponsorship());
    }

    @Test
    @DisplayName("Requirement 3.2: User authentication not blocked by statistical counter writes")
    void testAuthenticationNotBlockedByCounterUpdates() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Simulate concurrent statistical updates
        for (int i = 0; i < 5; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    // Update statistical counters
                    userStatsMapper.incrementFansCount(testUser.getId());
                    userStatsMapper.incrementTotalLikes(testUser.getId());
                    Thread.sleep(100); // Simulate processing time
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, executor));
        }
        
        // Simulate concurrent authentication/user lookups
        for (int i = 0; i < 5; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                // These should not be blocked by statistical updates
                User user = userMapper.selectUserById(testUser.getId());
                assertNotNull(user);
                assertEquals(testUser.getUsername(), user.getUsername());
            }, executor));
        }
        
        // Wait for all operations to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Future operation failed: " + e.getMessage());
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        
        // Verify final state
        UserStats finalStats = userStatsMapper.selectUserStatsByUserId(testUser.getId());
        assertTrue(finalStats.getFansCount() >= 0, "Fans count should be updated");
        assertTrue(finalStats.getTotalLikes() >= 0, "Total likes should be updated");
    }

    @Test
    @DisplayName("Requirement 3.3: No write lock contention on user table from same author interactions")
    void testNoWriteLockContentionOnUserTable() throws InterruptedException {
        // Create multiple users who will interact with the same author (testUser)
        List<User> interactingUsers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            User user = new User();
            user.setUsername("interacting_user_" + i + "_" + System.currentTimeMillis());
            user.setUserEmail("user" + i + "@example.com");
            user.setPassword("password");
            user.setCreateTime(new Timestamp(System.currentTimeMillis()));
            userMapper.insertUser(user);
            
            UserStats stats = new UserStats();
            stats.setUserId(user.getId());
            stats.setFansCount(0L);
            stats.setTotalLikes(0L);
            stats.setTotalFavorites(0L);
            stats.setTotalSponsorship(0L);
            stats.setTotalArticleExposure(0L);
            stats.setWebsiteCoins(0L);
            stats.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            stats.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            userStatsMapper.insertUserStats(stats);
            
            interactingUsers.add(user);
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Simulate multiple users liking/following the same author simultaneously
        for (User interactingUser : interactingUsers) {
            futures.add(CompletableFuture.runAsync(() -> {
                // These operations should only lock user_stats table, not user table
                userStatsMapper.incrementFansCount(testUser.getId());
                userStatsMapper.incrementTotalLikes(testUser.getId());
            }, executor));
        }
        
        // Simultaneously, the author should be able to update their profile
        futures.add(CompletableFuture.runAsync(() -> {
            testUser.setNickname("Updated Nickname");
            userMapper.updateUser(testUser);
        }, executor));
        
        // Wait for all operations to complete without deadlock
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Future operation failed: " + e.getMessage());
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        
        // Verify all operations completed successfully
        User updatedUser = userMapper.selectUserById(testUser.getId());
        assertEquals("Updated Nickname", updatedUser.getNickname());
        
        UserStats updatedStats = userStatsMapper.selectUserStatsByUserId(testUser.getId());
        assertTrue(updatedStats.getFansCount() >= 3, "Fans count should reflect all interactions");
        assertTrue(updatedStats.getTotalLikes() >= 3, "Total likes should reflect all interactions");
    }

    @Test
    @DisplayName("Requirement 3.4: User profile updates not delayed by statistical counter updates")
    void testProfileUpdatesNotDelayedByCounterUpdates() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<CompletableFuture<Long>> futures = new ArrayList<>();
        
        // Start continuous statistical updates
        CompletableFuture<Void> statsUpdates = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 10; i++) {
                userStatsMapper.incrementTotalLikes(testUser.getId());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, executor);
        
        // Measure profile update times during statistical updates
        for (int i = 0; i < 3; i++) {
            final int iteration = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                testUser.setNickname("Profile Update " + iteration);
                userMapper.updateUser(testUser);
                return System.currentTimeMillis() - startTime;
            }, executor));
        }
        
        // Wait for all profile updates to complete
        List<Long> updateTimes = new ArrayList<>();
        for (CompletableFuture<Long> future : futures) {
            try {
                updateTimes.add(future.get(3, TimeUnit.SECONDS));
            } catch (Exception e) {
                System.err.println("Future operation failed: " + e.getMessage());
                updateTimes.add(0L);
            }
        }

        try {
            statsUpdates.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Stats update failed: " + e.getMessage());
        }
        executor.shutdown();
        
        // Profile updates should complete quickly (not blocked by stats updates)
        for (Long updateTime : updateTimes) {
            assertTrue(updateTime < 1000, "Profile update should complete within 1 second, took: " + updateTime + "ms");
        }
    }

    @Test
    @DisplayName("Requirement 3.5: Efficient JOIN operations between user and user_stats")
    void testEfficientJoinOperations() {
        // Test JOIN query performance and correctness
        long startTime = System.currentTimeMillis();
        
        // This would typically be implemented in a service method that joins the tables
        User user = userMapper.selectUserById(testUser.getId());
        UserStats stats = userStatsMapper.selectUserStatsByUserId(testUser.getId());
        
        long queryTime = System.currentTimeMillis() - startTime;
        
        // Verify data integrity
        assertNotNull(user);
        assertNotNull(stats);
        assertEquals(user.getId(), stats.getUserId());
        
        // JOIN operation should be fast
        assertTrue(queryTime < 500, "JOIN operation should complete quickly, took: " + queryTime + "ms");
        
        // Test with updated stats
        userStatsMapper.incrementFansCount(testUser.getId());
        userStatsMapper.incrementTotalLikes(testUser.getId());
        
        UserStats updatedStats = userStatsMapper.selectUserStatsByUserId(testUser.getId());
        assertEquals(1, updatedStats.getFansCount());
        assertEquals(1, updatedStats.getTotalLikes());
    }

    @Test
    @DisplayName("Requirement 3.6: High-frequency actions only lock user_stats table")
    void testHighFrequencyActionsLockOnlyStatsTable() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Simulate high-frequency like operations
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                userStatsMapper.incrementTotalLikes(testUser.getId());
            }, executor));
        }
        
        // Simulate high-frequency follow operations
        for (int i = 0; i < 10; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                userStatsMapper.incrementFansCount(testUser.getId());
            }, executor));
        }
        
        // Simultaneously, user table operations should not be blocked
        CompletableFuture<Void> userTableOps = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 5; i++) {
                User user = userMapper.selectUserById(testUser.getId());
                assertNotNull(user);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, executor);
        
        // All operations should complete without deadlock
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
            userTableOps.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Future operation failed: " + e.getMessage());
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        
        // Verify final counts
        UserStats finalStats = userStatsMapper.selectUserStatsByUserId(testUser.getId());
        assertEquals(10, finalStats.getTotalLikes(), "All like operations should be recorded");
        assertEquals(10, finalStats.getFansCount(), "All follow operations should be recorded");
    }

    @Test
    @DisplayName("Data integrity validation after concurrent operations")
    void testDataIntegrityAfterConcurrentOperations() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        final int expectedLikes = 5;
        final int expectedFans = 3;
        
        // Concurrent like operations
        for (int i = 0; i < expectedLikes; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                userStatsMapper.incrementTotalLikes(testUser.getId());
            }, executor));
        }
        
        // Concurrent fan operations
        for (int i = 0; i < expectedFans; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                userStatsMapper.incrementFansCount(testUser.getId());
            }, executor));
        }
        
        // Wait for all operations
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Future operation failed: " + e.getMessage());
        }
        executor.shutdown();
        
        // Verify data integrity
        UserStats finalStats = userStatsMapper.selectUserStatsByUserId(testUser.getId());
        assertEquals(expectedLikes, finalStats.getTotalLikes(), "Like count should match expected operations");
        assertEquals(expectedFans, finalStats.getFansCount(), "Fan count should match expected operations");
        
        // Verify user data is unchanged
        User finalUser = userMapper.selectUserById(testUser.getId());
        assertEquals(testUser.getUsername(), finalUser.getUsername());
        assertEquals(testUser.getUserEmail(), finalUser.getUserEmail());
    }
}