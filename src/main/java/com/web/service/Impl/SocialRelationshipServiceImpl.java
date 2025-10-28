package com.web.service.Impl;

import com.web.constant.ContactStatus;
import com.web.mapper.ContactMapper;
import com.web.mapper.UserFollowMapper;
import com.web.mapper.UserMapper;
import com.web.model.Contact;
import com.web.model.User;
import com.web.model.UserFollow;
import com.web.service.ContactService;
import com.web.service.SocialRelationshipService;
import com.web.service.UserFollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 社交关系服务实现类
 * 统一管理联系人（好友）和关注系统
 * 实现关系状态机和自动关系建立
 */
@Slf4j
@Service
@Transactional
public class SocialRelationshipServiceImpl implements SocialRelationshipService {

    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ContactService contactService;

    @Autowired
    private UserFollowService userFollowService;

    @Override
    public RelationshipStatus getRelationshipStatus(Long userId, Long targetUserId) {
        try {
            // 检查是否为好友
            if (isFriend(userId, targetUserId)) {
                return RelationshipStatus.FRIEND;
            }

            // 检查关注关系
            boolean iFollowTarget = userFollowMapper.isFollowing(userId, targetUserId);
            boolean targetFollowsMe = userFollowMapper.isFollowing(targetUserId, userId);

            if (iFollowTarget && targetFollowsMe) {
                return RelationshipStatus.MUTUAL_FOLLOW;
            } else if (iFollowTarget) {
                return RelationshipStatus.FOLLOWING;
            } else if (targetFollowsMe) {
                return RelationshipStatus.FOLLOWER;
            }

            // 检查是否有待处理的好友请求
            Contact sentRequest = contactMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contact>()
                    .eq("user_id", userId)
                    .eq("friend_id", targetUserId)
                    .eq("status", ContactStatus.PENDING.getCode())
            );
            if (sentRequest != null) {
                return RelationshipStatus.FRIEND_REQUEST_SENT;
            }

            Contact receivedRequest = contactMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contact>()
                    .eq("user_id", targetUserId)
                    .eq("friend_id", userId)
                    .eq("status", ContactStatus.PENDING.getCode())
            );
            if (receivedRequest != null) {
                return RelationshipStatus.FRIEND_REQUEST_RECEIVED;
            }

            return RelationshipStatus.STRANGER;

        } catch (Exception e) {
            log.error("获取关系状态失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return RelationshipStatus.STRANGER;
        }
    }

    @Override
    @Transactional
    public boolean followUser(Long userId, Long targetUserId) {
        try {
            // 不能关注自己
            if (userId.equals(targetUserId)) {
                log.warn("用户尝试关注自己: userId={}", userId);
                return false;
            }

            // 检查是否已关注
            if (userFollowMapper.isFollowing(userId, targetUserId)) {
                log.info("用户已关注目标用户: userId={}, targetUserId={}", userId, targetUserId);
                return true;
            }

            // 创建关注关系
            userFollowService.followUser(userId, targetUserId);
            boolean followed = true;

            // 检查是否互相关注，如果是则自动建立好友关系
            if (userFollowMapper.isFollowing(targetUserId, userId)) {
                handleMutualFollowAutoFriend(userId, targetUserId);
            }

            log.info("关注成功: userId={}, targetUserId={}", userId, targetUserId);
            return true;

        } catch (Exception e) {
            log.error("关注用户失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean unfollowUser(Long userId, Long targetUserId) {
        try {
            userFollowService.unfollowUser(userId, targetUserId);
            log.info("取消关注: userId={}, targetUserId={}", userId, targetUserId);
            return true;
        } catch (Exception e) {
            log.error("取消关注失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean sendFriendRequest(Long userId, Long targetUserId, String message) {
        try {
            // 使用ContactService发送好友请求
            com.web.vo.contact.ContactApplyVo applyVo = new com.web.vo.contact.ContactApplyVo();
            applyVo.setFriendId(targetUserId);
            applyVo.setRemarks(message);
            
            contactService.apply(applyVo, userId);
            log.info("发送好友请求成功: userId={}, targetUserId={}", userId, targetUserId);
            return true;

        } catch (Exception e) {
            log.error("发送好友请求失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean acceptFriendRequest(Long userId, Long requestId) {
        try {
            contactService.accept(requestId, userId);
            log.info("接受好友请求成功: userId={}, requestId={}", userId, requestId);
            return true;
        } catch (Exception e) {
            log.error("接受好友请求失败: userId={}, requestId={}", userId, requestId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean rejectFriendRequest(Long userId, Long requestId) {
        try {
            contactService.declineOrBlock(requestId, userId, ContactStatus.REJECTED);
            log.info("拒绝好友请求成功: userId={}, requestId={}", userId, requestId);
            return true;
        } catch (Exception e) {
            log.error("拒绝好友请求失败: userId={}, requestId={}", userId, requestId, e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean removeFriend(Long userId, Long friendId, boolean keepFollow) {
        try {
            // 查找好友关系记录
            Contact contact = contactMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contact>()
                    .and(wrapper -> wrapper
                        .eq("user_id", userId).eq("friend_id", friendId)
                        .or()
                        .eq("user_id", friendId).eq("friend_id", userId)
                    )
                    .eq("status", ContactStatus.ACCEPTED.getCode())
            );

            if (contact == null) {
                log.warn("好友关系不存在: userId={}, friendId={}", userId, friendId);
                return false;
            }

            // 删除好友关系
            contactMapper.deleteById(contact.getId());

            // 如果不保留关注关系，则取消关注
            if (!keepFollow) {
                userFollowMapper.unfollowUser(userId, friendId);
            }

            log.info("删除好友成功: userId={}, friendId={}, keepFollow={}", userId, friendId, keepFollow);
            return true;

        } catch (Exception e) {
            log.error("删除好友失败: userId={}, friendId={}", userId, friendId, e);
            return false;
        }
    }

    @Override
    public boolean isMutualFollow(Long userId, Long targetUserId) {
        try {
            return userFollowMapper.isFollowing(userId, targetUserId) 
                && userFollowMapper.isFollowing(targetUserId, userId);
        } catch (Exception e) {
            log.error("检查互相关注失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return false;
        }
    }

    @Override
    public boolean isFriend(Long userId, Long targetUserId) {
        try {
            Contact contact = contactMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contact>()
                    .and(wrapper -> wrapper
                        .eq("user_id", userId).eq("friend_id", targetUserId)
                        .or()
                        .eq("user_id", targetUserId).eq("friend_id", userId)
                    )
                    .eq("status", ContactStatus.ACCEPTED.getCode())
            );
            return contact != null;
        } catch (Exception e) {
            log.error("检查好友关系失败: userId={}, targetUserId={}", userId, targetUserId, e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getFriendList(Long userId) {
        try {
            List<com.web.dto.UserDto> friends = contactMapper.selectContactsByUserAndStatus(
                userId, ContactStatus.ACCEPTED.getCode()
            );
            
            return friends.stream().map(friend -> {
                Map<String, Object> friendInfo = new HashMap<>();
                friendInfo.put("userId", friend.getId());
                friendInfo.put("username", friend.getName());
                friendInfo.put("avatar", friend.getAvatar());
                friendInfo.put("relationshipStatus", RelationshipStatus.FRIEND.name());
                return friendInfo;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取好友列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getFollowingList(Long userId) {
        try {
            List<Long> followingIds = userFollowMapper.getFollowingIds(userId);
            return buildUserInfoList(followingIds, RelationshipStatus.FOLLOWING);
        } catch (Exception e) {
            log.error("获取关注列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getFollowerList(Long userId) {
        try {
            List<Long> followerIds = userFollowMapper.getFollowerIds(userId);
            return buildUserInfoList(followerIds, RelationshipStatus.FOLLOWER);
        } catch (Exception e) {
            log.error("获取粉丝列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getMutualFollowList(Long userId) {
        try {
            List<Long> followingIds = userFollowMapper.getFollowingIds(userId);
            List<Long> followerIds = userFollowMapper.getFollowerIds(userId);
            
            // 找出互相关注的用户
            Set<Long> mutualFollowIds = new HashSet<>(followingIds);
            mutualFollowIds.retainAll(followerIds);
            
            return buildUserInfoList(new ArrayList<>(mutualFollowIds), RelationshipStatus.MUTUAL_FOLLOW);

        } catch (Exception e) {
            log.error("获取互相关注列表失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getFriendRecommendations(Long userId, int limit) {
        try {
            // 基于共同好友的推荐算法
            List<Long> friendIds = contactMapper.selectContactUserIdsByUserAndStatus(
                userId, ContactStatus.ACCEPTED.getCode()
            );

            if (friendIds.isEmpty()) {
                return new ArrayList<>();
            }

            // 获取好友的好友（二度关系）
            Map<Long, Integer> recommendationScores = new HashMap<>();
            for (Long friendId : friendIds) {
                List<Long> friendOfFriendIds = contactMapper.selectContactUserIdsByUserAndStatus(
                    friendId, ContactStatus.ACCEPTED.getCode()
                );
                
                for (Long candidateId : friendOfFriendIds) {
                    // 排除自己和已经是好友的用户
                    if (!candidateId.equals(userId) && !friendIds.contains(candidateId)) {
                        recommendationScores.put(candidateId, 
                            recommendationScores.getOrDefault(candidateId, 0) + 1);
                    }
                }
            }

            // 按共同好友数量排序
            List<Long> recommendedIds = recommendationScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            return buildUserInfoList(recommendedIds, RelationshipStatus.STRANGER);

        } catch (Exception e) {
            log.error("获取好友推荐失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Map<String, Object>> getFollowRecommendations(Long userId, int limit) {
        try {
            // 基于关注关系的推荐
            List<Long> followingIds = userFollowMapper.getFollowingIds(userId);
            
            if (followingIds.isEmpty()) {
                return new ArrayList<>();
            }

            // 获取关注用户的关注列表
            Map<Long, Integer> recommendationScores = new HashMap<>();
            for (Long followingId : followingIds) {
                List<Long> theirFollowingIds = userFollowMapper.getFollowingIds(followingId);
                
                for (Long candidateId : theirFollowingIds) {
                    if (!candidateId.equals(userId) && !followingIds.contains(candidateId)) {
                        recommendationScores.put(candidateId,
                            recommendationScores.getOrDefault(candidateId, 0) + 1);
                    }
                }
            }

            List<Long> recommendedIds = recommendationScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            return buildUserInfoList(recommendedIds, RelationshipStatus.STRANGER);

        } catch (Exception e) {
            log.error("获取关注推荐失败: userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public Map<String, Object> getSocialStatistics(Long userId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 好友数
            Long friendCountLong = contactMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contact>()
                    .and(wrapper -> wrapper
                        .eq("user_id", userId)
                        .or()
                        .eq("friend_id", userId)
                    )
                    .eq("status", ContactStatus.ACCEPTED.getCode())
            );
            int friendCount = friendCountLong != null ? friendCountLong.intValue() : 0;
            stats.put("friendCount", friendCount);

            // 关注数
            int followingCount = userFollowMapper.getFollowingIds(userId).size();
            stats.put("followingCount", followingCount);

            // 粉丝数
            int followerCount = userFollowMapper.getFollowerIds(userId).size();
            stats.put("followerCount", followerCount);

            // 互相关注数
            List<Long> followingIds = userFollowMapper.getFollowingIds(userId);
            List<Long> followerIds = userFollowMapper.getFollowerIds(userId);
            Set<Long> mutualFollowSet = new HashSet<>(followingIds);
            mutualFollowSet.retainAll(followerIds);
            stats.put("mutualFollowCount", mutualFollowSet.size());

            // 待处理好友请求数
            Long pendingRequestCountLong = contactMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contact>()
                    .eq("friend_id", userId)
                    .eq("status", ContactStatus.PENDING.getCode())
            );
            int pendingRequestCount = pendingRequestCountLong != null ? pendingRequestCountLong.intValue() : 0;
            stats.put("pendingRequestCount", pendingRequestCount);

            return stats;

        } catch (Exception e) {
            log.error("获取社交统计失败: userId={}", userId, e);
            return new HashMap<>();
        }
    }

    @Override
    @Transactional
    public void handleMutualFollowAutoFriend(Long userId, Long targetUserId) {
        try {
            // 检查是否已经是好友
            if (isFriend(userId, targetUserId)) {
                log.info("用户已经是好友，跳过自动建立: userId={}, targetUserId={}", userId, targetUserId);
                return;
            }

            // 检查是否互相关注
            if (!isMutualFollow(userId, targetUserId)) {
                log.info("用户未互相关注，跳过自动建立: userId={}, targetUserId={}", userId, targetUserId);
                return;
            }

            // 自动建立好友关系
            Contact contact = new Contact();
            contact.setUserId(userId);
            contact.setFriendId(targetUserId);
            contact.setStatus(ContactStatus.ACCEPTED.getCode());
            contact.setRemarks("互相关注自动建立");
            contact.setCreateTime(new Date());
            contact.setUpdateTime(new Date());
            
            contactMapper.insert(contact);

            log.info("互相关注自动建立好友关系成功: userId={}, targetUserId={}", userId, targetUserId);

        } catch (Exception e) {
            log.error("互相关注自动建立好友失败: userId={}, targetUserId={}", userId, targetUserId, e);
        }
    }

    /**
     * 构建用户信息列表
     */
    private List<Map<String, Object>> buildUserInfoList(List<Long> userIds, RelationshipStatus status) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        return userIds.stream()
            .map(id -> {
                User user = userMapper.selectById(id);
                if (user == null) {
                    return null;
                }
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("userId", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("nickname", user.getNickname());
                userInfo.put("avatar", user.getAvatar());
                userInfo.put("relationshipStatus", status.name());
                return userInfo;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
