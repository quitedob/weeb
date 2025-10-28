<template>
  <div class="follow-list-page">
    <el-card class="header-card">
      <h2>{{ pageTitle }}</h2>
      <p class="subtitle">{{ pageSubtitle }}</p>
    </el-card>

    <!-- 标签页切换 -->
    <el-card class="tabs-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我的关注" name="following">
          <template #label>
            <span class="tab-label">
              <el-icon><User /></el-icon>
              我的关注 ({{ followingCount }})
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="我的粉丝" name="followers">
          <template #label>
            <span class="tab-label">
              <el-icon><UserFilled /></el-icon>
              我的粉丝 ({{ followersCount }})
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 用户列表 -->
    <el-card class="list-card" v-loading="loading">
      <div v-if="userList.length === 0" class="empty-state">
        <el-empty :description="emptyText" />
      </div>

      <div v-else class="user-list">
        <div
          v-for="user in userList"
          :key="user.id"
          class="user-item"
        >
          <div class="user-avatar">
            <el-avatar
              :size="60"
              :src="user.avatar || 'https://cube.elemecdn.com/e/fd/0fc7d20532fdaf769a25683617711png.png'"
            />
          </div>

          <div class="user-info">
            <div class="user-name">
              <router-link :to="`/user/${user.id}`" class="username-link">
                {{ user.username }}
              </router-link>
              <el-tag v-if="user.userLevel >= 4" size="small" type="warning">
                {{ getUserLevelName(user.userLevel) }}
              </el-tag>
            </div>
            <div class="user-bio">{{ user.bio || '这个人很懒，什么都没写' }}</div>
            <div class="user-stats">
              <span class="stat-item">
                <el-icon><Document /></el-icon>
                {{ user.articleCount || 0 }} 文章
              </span>
              <span class="stat-item">
                <el-icon><User /></el-icon>
                {{ user.followerCount || 0 }} 粉丝
              </span>
            </div>
          </div>

          <div class="user-actions">
            <el-button
              v-if="activeTab === 'following'"
              type="danger"
              size="small"
              @click="handleUnfollow(user)"
              :loading="user.loading"
            >
              取消关注
            </el-button>
            <el-button
              v-else
              :type="user.isFollowing ? 'info' : 'primary'"
              size="small"
              @click="handleFollowToggle(user)"
              :loading="user.loading"
            >
              {{ user.isFollowing ? '已关注' : '关注' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-if="total > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadUserList"
        @current-change="loadUserList"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/authStore'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, UserFilled, Document } from '@element-plus/icons-vue'
import followApi from '@/api/modules/follow'
import userApi from '@/api/modules/user'

const authStore = useAuthStore()

// 数据
const activeTab = ref('following')
const userList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const followingCount = ref(0)
const followersCount = ref(0)

// 计算属性
const pageTitle = computed(() => {
  return activeTab.value === 'following' ? '我的关注' : '我的粉丝'
})

const pageSubtitle = computed(() => {
  return activeTab.value === 'following' 
    ? '管理你关注的用户' 
    : '查看关注你的用户'
})

const emptyText = computed(() => {
  return activeTab.value === 'following' 
    ? '你还没有关注任何人' 
    : '还没有人关注你'
})

// 方法
const loadUserList = async () => {
  if (!authStore.currentUser) {
    ElMessage.error('请先登录')
    return
  }

  loading.value = true
  try {
    const userId = authStore.currentUser.id
    let response

    if (activeTab.value === 'following') {
      response = await followApi.getFollowingList(userId, currentPage.value, pageSize.value)
    } else {
      response = await followApi.getFollowersList(userId, currentPage.value, pageSize.value)
    }

    if (response.data.success) {
      userList.value = (response.data.data.list || []).map(user => ({
        ...user,
        loading: false,
        isFollowing: activeTab.value === 'followers' ? user.isFollowing : true
      }))
      total.value = response.data.data.total || 0
    } else {
      ElMessage.error(response.data.message || '加载失败')
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadStats = async () => {
  try {
    const response = await followApi.getFollowStats()
    if (response.data.success) {
      followingCount.value = response.data.data.followingCount || 0
      followersCount.value = response.data.data.followersCount || 0
    }
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  loadUserList()
}

const handleUnfollow = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消关注 ${user.username} 吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    user.loading = true
    const response = await userApi.unfollowUser(user.id)

    if (response.data.success) {
      ElMessage.success('已取消关注')
      followingCount.value--
      loadUserList()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消关注失败:', error)
      ElMessage.error('操作失败，请稍后重试')
    }
  } finally {
    user.loading = false
  }
}

const handleFollowToggle = async (user) => {
  user.loading = true
  try {
    let response
    if (user.isFollowing) {
      response = await userApi.unfollowUser(user.id)
      if (response.data.success) {
        user.isFollowing = false
        ElMessage.success('已取消关注')
        followingCount.value--
      }
    } else {
      response = await userApi.followUser(user.id)
      if (response.data.success) {
        user.isFollowing = true
        ElMessage.success('关注成功')
        followingCount.value++
      }
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    user.loading = false
  }
}

const getUserLevelName = (level) => {
  const levels = {
    1: '新用户',
    2: '活跃用户',
    3: '资深用户',
    4: '版主',
    5: '管理员'
  }
  return levels[level] || '用户'
}

// 初始化
onMounted(() => {
  loadStats()
  loadUserList()
})
</script>

<style scoped>
.follow-list-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header-card {
  margin-bottom: 20px;
}

.header-card h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.tabs-card {
  margin-bottom: 20px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 5px;
}

.list-card {
  min-height: 400px;
}

.empty-state {
  padding: 60px 0;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  transition: all 0.3s;
}

.user-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.user-avatar {
  margin-right: 15px;
}

.user-info {
  flex: 1;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.username-link {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  text-decoration: none;
}

.username-link:hover {
  color: #409EFF;
}

.user-bio {
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
}

.user-stats {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #909399;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.user-actions {
  margin-left: 15px;
}

.pagination {
  margin-top: 20px;
  justify-content: center;
}
</style>
