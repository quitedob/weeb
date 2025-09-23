package com.web.performance;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmarking tests for database write operations with separated user/user_stats tables
 * Tests Requirements: 3.2, 3.3, 3.4, 3.6 (Database Schema Optimization Performance)
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DatabaseWritePerformanceTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserStatsMapper userStatsMapper;

    private List<User> testUsers;
    private List<UserStats> testUserStats;
    private static final int USER_COUNT = 20;

    @BeforeEach
    void setUp() {
        testUsers = new ArrayList<>();
        testUserStats = new ArrayList<>();
        
        // Create test users and their stats
        for (int i = 0; i < USER_COUNT; i++) {
            User user = new User();
            user.setUsername("writeperf_user_" + i + "_" + System.currentTimeMillis());
            user.setUserEmail("writeperf" + i + "@example.com");
            user.setPassword("hashedpassword" + i);
            user.setNickname("Write Performance Test User " + i);
            user.setCreateTime(new Timestamp(System.currentTimeMillis()));
            
            userMapper.insertUser(user);
            testUsers.add(user);
            
            UserStats stats = new UserStats();
            stats.setUserId(user.getId());
            stats.setFansCount(0);
            stats.setTotalLikes(0);
            stats.setTotalFavorites(0);
            stats.setTotalSponsorship(BigDecimal.ZERO);
            stats.setTotalArticleExposure(0);
            stats.setWebsiteCoins(0);
            stats.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            stats.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            
            userStatsMapper.insertUserStats(stats);
            testUserStats.add(stats);
        }
    }

    @Test
    @DisplayName("Requirement 3.2: Benchmark database write performance with separated tables")
    void testSeparatedTableWritePerformance() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(30);
        AtomicInteger statsUpdates = new AtomicInteger(0);
        AtomicInteger userUpdates = new AtomicInteger(0);
        AtomicLong totalStatsTime = new AtomicLong(0);
        AtomicLong totalUserTime = new AtomicLong(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        long testStart = System.currentTimeMillis();
        
        // Concurrent statistical updates (high frequency operations)
        for (int i = 0; i < 100; i++) {
            final User user = testUsers.get(i % testUsers.size());
            
            futures.add(CompletableFuture.runAsync(() -> {
                long start = System.currentTimeMillis();
                try {
                    userStatsMapper.incrementTotalLikes(user.getId());
                    userStatsMapper.incrementFansCount(user.getId());
                    
                    long end = System.currentTimeMillis();
                    totalStatsTime.addAndGet(end - start);
                    statsUpdates.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Stats update failed: " + e.getMessage());
                }
            }, executor));
        }
        
        // Concurrent user profile updates (should not be blocked)
        for (int i = 0; i < 20; i++) {
            final User user = testUsers.get(i % testUsers.size());
            final int iteration = i;
            
            futures.add(CompletableFuture.runAsync(() -> {
                long start = System.currentTimeMillis();
                try {
                    user.setNickname("Updated User " + iteration);
                    userMapper.updateUser(user);
                    
                    long end = System.currentTimeMillis();
                    totalUserTime.addAndGet(end - start);
                    userUpdates.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("User update failed: " + e.getMessage());
                }
            }, executor));
        }
        
        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(15, TimeUnit.SECONDS);
        
        long testEnd = System.currentTimeMillis();
        long totalTestTime = testEnd - testStart;
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        
        // Calculate performance metrics
        double avgStatsTime = statsUpdates.get() > 0 ? (double) totalStatsTime.get() / statsUpdates.get() : 0;
        double avgUserTime = userUpdates.get() > 0 ? (double) totalUserTime.get() / userUpdates.get() : 0;
        double statsPerSecond = (double) statsUpdates.get() / totalTestTime * 1000;
        double userPerSecond = (double) userUpdates.get() / totalTestTime * 1000;
        
        System.out.println("Separated Tables Write Performance Results:");
        System.out.println("Total test time: " + totalTestTime + "ms");
        System.out.println("Stats updates completed: " + statsUpdates.get());
        System.out.println("User updates completed: " + userUpdates.get());
        System.out.println("Average stats update time: " + String.format("%.2f ms", avgStatsTime));
        System.out.println("Average user update time: " + String.format("%.2f ms", avgUserTime));
        System.out.println("Stats updates per second: " + String.format("%.2f", statsPerSecond));
        System.out.println("User updates per second: " + String.format("%.2f", userPerSecond));
        
        // Performance assertions for optimized schema
        assertTrue(avgStatsTime < 100, "Stats updates should be fast with separated table");
        assertTrue(avgUserTime < 100, "User updates should not be blocked by stats operations");
        assertTrue(statsPerSecond > 5, "Stats update throughput should be good");
        assertTrue(userPerSecond > 1, "User update throughput should not be impacted");
        assertTrue(totalTestTime < 10000, "All operations should complete within 10 seconds");
    }

    @Test
    @DisplayName("Requirement 3.3: Validate no lock contention with concurrent operations")
    void testNoLockContentionValidation() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(40);
        AtomicInteger successfulOperations = new AtomicInteger(0);
        AtomicInteger failedOperations = new AtomicInteger(0);
        AtomicLong maxOperationTime = new AtomicLong(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Create a scenario that would cause lock contention in the old schema
        User targetUser = testUsers.get(0); // All operations target the same user
        
        long testStart = System.currentTimeMillis();
        
        // Multiple threads updating statistics for the same user
        for (int i = 0; i < 50; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                long opStart = System.currentTimeMillis();
                try {
                    userStatsMapper.incrementTotalLikes(targetUser.getId());
                    long opEnd = System.currentTimeMillis();
                    
                    long opTime = opEnd - opStart;
                    maxOperationTime.updateAndGet(current -> Math.max(current, opTime));
                    successfulOperations.incrementAndGet();
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    System.err.println("Stats operation failed: " + e.getMessage());
                }
            }, executor));
        }
        
        // Multiple threads reading/updating user profile for the same user
        for (int i = 0; i < 10; i++) {
            final int iteration = i;
            futures.add(CompletableFuture.runAsync(() -> {
                long opStart = System.currentTimeMillis();
                try {
                    // Read operation
                    User user = userMapper.selectUserById(targetUser.getId());
                    assertNotNull(user);
                    
                    // Update operation
                    user.setNickname("Concurrent Update " + iteration);
                    userMapper.updateUser(user);
                    
                    long opEnd = System.currentTimeMillis();
                    long opTime = opEnd - opStart;
                    maxOperationTime.updateAndGet(current -> Math.max(current, opTime));
                    successfulOperations.incrementAndGet();
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    System.err.println("User operation failed: " + e.getMessage());
                }
            }, executor));
        }
        
        // Wait for all operations
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(20, TimeUnit.SECONDS);
        
        long testEnd = System.currentTimeMillis();
        long totalTime = testEnd - testStart;
        
        executor.shutdown();
        
        System.out.println("Lock Contention Validation Results:");
        System.out.println("Total operations: " + (successfulOperations.get() + failedOperations.get()));
        System.out.println("Successful operations: " + successfulOperations.get());
        System.out.println("Failed operations: " + failedOperations.get());
        System.out.println("Total time: " + totalTime + "ms");
        System.out.println("Max single operation time: " + maxOperationTime.get() + "ms");
        
        // Validate no lock contention
        assertTrue(failedOperations.get() == 0, "No operations should fail due to lock contention");
        assertTrue(maxOperationTime.get() < 1000, "No single operation should take more than 1 second");
        assertTrue(totalTime < 15000, "All concurrent operations should complete within 15 seconds");
        
        // Verify final state consistency
        UserStats finalStats = userStatsMapper.selectUserStatsByUserId(targetUser.getId());
        assertEquals(50, finalStats.getTotalLikes(), "All like increments should be recorded");
    }

    @Test
    @DisplayName("Requirement 3.4: User profile updates not delayed by statistical operations")
    void testProfileUpdatePerformanceDuringStatsOperations() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(25);
        List<Long> profileUpdateTimes = new ArrayList<>();
        AtomicInteger statsOperationsCompleted = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        User testUser = testUsers.get(0);
        
        // Start continuous statistical operations
        CompletableFuture<Void> continuousStats = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 200; i++) {
                try {
                    userStatsMapper.incrementTotalLikes(testUser.getId());
                    userStatsMapper.incrementFansCount(testUser.getId());
                    statsOperationsCompleted.incrementAndGet();
                    Thread.sleep(5); // Small delay to simulate processing
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("Stats operation failed: " + e.getMessage());
                }
            }
        }, executor);
        
        // Measure profile update times during statistical operations
        for (int i = 0; i < 10; i++) {
            final int iteration = i;
            futures.add(CompletableFuture.runAsync(() -> {
                // Wait a bit to ensure stats operations are running
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                
                long start = System.currentTimeMillis();
                try {
                    testUser.setNickname("Profile Update During Stats " + iteration);
                    userMapper.updateUser(testUser);
                    
                    long end = System.currentTimeMillis();
                    synchronized (profileUpdateTimes) {
                        profileUpdateTimes.add(end - start);
                    }
                } catch (Exception e) {
                    System.err.println("Profile update failed: " + e.getMessage());
                }
            }, executor));
        }
        
        // Wait for profile updates to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);
        
        // Stop continuous stats operations
        continuousStats.get(15, TimeUnit.SECONDS);
        
        executor.shutdown();
        
        // Analyze profile update performance
        double avgProfileUpdateTime = profileUpdateTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long maxProfileUpdateTime = profileUpdateTimes.stream().mapToLong(Long::longValue).max().orElse(0L);
        long minProfileUpdateTime = profileUpdateTimes.stream().mapToLong(Long::longValue).min().orElse(0L);
        
        System.out.println("Profile Update Performance During Stats Operations:");
        System.out.println("Profile updates completed: " + profileUpdateTimes.size());
        System.out.println("Stats operations completed: " + statsOperationsCompleted.get());
        System.out.println("Average profile update time: " + String.format("%.2f ms", avgProfileUpdateTime));
        System.out.println("Min profile update time: " + minProfileUpdateTime + "ms");
        System.out.println("Max profile update time: " + maxProfileUpdateTime + "ms");
        
        // Profile updates should not be significantly delayed
        assertTrue(avgProfileUpdateTime < 200, "Average profile update time should be reasonable during stats operations");
        assertTrue(maxProfileUpdateTime < 500, "Even slowest profile update should complete quickly");
        assertTrue(profileUpdateTimes.size() == 10, "All profile updates should complete successfully");
    }

    @Test
    @DisplayName("Requirement 3.6: High-frequency operations throughput test")
    void testHighFrequencyOperationsThroughput() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicInteger likesProcessed = new AtomicInteger(0);
        AtomicInteger followsProcessed = new AtomicInteger(0);
        AtomicInteger userReadsProcessed = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        long testStart = System.currentTimeMillis();
        
        // High-frequency like operations
        for (int i = 0; i < 200; i++) {
            final User user = testUsers.get(i % testUsers.size());
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    userStatsMapper.incrementTotalLikes(user.getId());
                    likesProcessed.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Like operation failed: " + e.getMessage());
                }
            }, executor));
        }
        
        // High-frequency follow operations
        for (int i = 0; i < 150; i++) {
            final User user = testUsers.get(i % testUsers.size());
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    userStatsMapper.incrementFansCount(user.getId());
                    followsProcessed.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Follow operation failed: " + e.getMessage());
                }
            }, executor));
        }
        
        // Concurrent user read operations (should not be blocked)
        for (int i = 0; i < 100; i++) {
            final User user = testUsers.get(i % testUsers.size());
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    User retrievedUser = userMapper.selectUserById(user.getId());
                    if (retrievedUser != null) {
                        userReadsProcessed.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("User read operation failed: " + e.getMessage());
                }
            }, executor));
        }
        
        // Wait for all operations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(20, TimeUnit.SECONDS);
        
        long testEnd = System.currentTimeMillis();
        long totalTime = testEnd - testStart;
        
        executor.shutdown();
        
        // Calculate throughput metrics
        double likeThroughput = (double) likesProcessed.get() / totalTime * 1000;
        double followThroughput = (double) followsProcessed.get() / totalTime * 1000;
        double readThroughput = (double) userReadsProcessed.get() / totalTime * 1000;
        double totalThroughput = (double) (likesProcessed.get() + followsProcessed.get() + userReadsProcessed.get()) / totalTime * 1000;
        
        System.out.println("High-Frequency Operations Throughput Results:");
        System.out.println("Total test time: " + totalTime + "ms");
        System.out.println("Likes processed: " + likesProcessed.get());
        System.out.println("Follows processed: " + followsProcessed.get());
        System.out.println("User reads processed: " + userReadsProcessed.get());
        System.out.println("Like throughput: " + String.format("%.2f ops/sec", likeThroughput));
        System.out.println("Follow throughput: " + String.format("%.2f ops/sec", followThroughput));
        System.out.println("Read throughput: " + String.format("%.2f ops/sec", readThroughput));
        System.out.println("Total throughput: " + String.format("%.2f ops/sec", totalThroughput));
        
        // Throughput assertions for optimized schema
        assertTrue(likeThroughput > 10, "Like operations should have good throughput");
        assertTrue(followThroughput > 10, "Follow operations should have good throughput");
        assertTrue(readThroughput > 15, "Read operations should not be impacted by writes");
        assertTrue(totalThroughput > 20, "Overall system throughput should be good");
        assertTrue(totalTime < 15000, "All high-frequency operations should complete within 15 seconds");
        
        // Verify data consistency
        int totalExpectedLikes = likesProcessed.get();
        int totalExpectedFollows = followsProcessed.get();
        
        int actualLikes = 0;
        int actualFollows = 0;
        
        for (User user : testUsers) {
            UserStats stats = userStatsMapper.selectUserStatsByUserId(user.getId());
            if (stats != null) {
                actualLikes += stats.getTotalLikes();
                actualFollows += stats.getFansCount();
            }
        }
        
        assertEquals(totalExpectedLikes, actualLikes, "All like operations should be recorded correctly");
        assertEquals(totalExpectedFollows, actualFollows, "All follow operations should be recorded correctly");
    }
}