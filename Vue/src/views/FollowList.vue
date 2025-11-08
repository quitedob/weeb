<template>
  <div class="friend-list-page">
    <el-card class="header-card">
      <h2>{{ pageTitle }}</h2>
      <p class="subtitle">{{ pageSubtitle }}</p>
    </el-card>

    <!-- 标签页切换 -->
    <el-card class="tabs-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我的好友" name="friends">
          <template #label>
            <span class="tab-label">
              <el-icon><User /></el-icon>
              我的好友 ({{ friendsCount }})
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="好友申请" name="requests">
          <template #label>
            <span class="tab-label">
              <el-icon><UserFilled /></el-icon>
              好友申请 ({{ requestsCount }})
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
              v-if="activeTab === 'friends'"
              type="danger"
              size="small"
              @click="handleDeleteFriend(user)"
              :loading="user.loading"
            >
              删除好友
            </el-button>
            <template v-else>
              <el-button
                type="success"
                size="small"
                @click="handleAcceptRequest(user)"
                :loading="user.loading"
              >
                接受
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="handleRejectRequest(user)"
                :loading="user.loading"
              >
                拒绝
              </el-button>
            </template>
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
        @size-change="fetchData"
        @current-change="fetchData"
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
import contactApi from '@/api/modules/contact'

const authStore = useAuthStore()

// 数据
const activeTab = ref('friends')
const userList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const friendsCount = ref(0)
const requestsCount = ref(0)

// 计算属性
const pageTitle = computed(() => {
  return activeTab.value === 'friends' ? '我的好友' : '好友申请'
})

const pageSubtitle = computed(() => {
  return activeTab.value === 'friends' 
    ? '管理你的好友列表' 
    : '处理好友申请'
})

const emptyText = computed(() => {
  return activeTab.value === 'friends' 
    ? '你还没有添加任何好友' 
    : '暂无好友申请'
})

// 方法
const fetchFriendsList = async () => {
  try {
    loading.value = true
    const response = await contactApi.getContacts('ACCEPTED')
    
    if (response.code === 0) {
      userList.value = (response.data || []).map(user => ({
        ...user,
        loading: false
      }))
      total.value = response.data?.length || 0
      friendsCount.value = response.data?.length || 0
    } else {
      ElMessage.error(response.message || '获取好友列表失败')
    }
  } catch (error) {
    console.error('获取好友列表失败:', error)
    ElMessage.error('获取好友列表失败')
  } finally {
    loading.value = false
  }
}

const fetchRequestsList = async () => {
  try {
    loading.value = true
    const response = await contactApi.getFriendRequests()
    
    if (response.code === 0) {
      userList.value = (response.data || []).map(user => ({
        ...user,
        loading: false
      }))
      total.value = response.data?.length || 0
      requestsCount.value = response.data?.length || 0
    } else {
      ElMessage.error(response.message || '获取好友申请失败')
    }
  } catch (error) {
    console.error('获取好友申请失败:', error)
    ElMessage.error('获取好友申请失败')
  } finally {
    loading.value = false
  }
}

const fetchData = async () => {
  if (activeTab.value === 'friends') {
    await fetchFriendsList()
  } else {
    await fetchRequestsList()
  }
}

const handleTabChange = () => {
  currentPage.value = 1
  fetchData()
}

const handleDeleteFriend = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除好友 ${user.nickname || user.username} 吗？`,
      '确认删除好友',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    user.loading = true
    const response = await contactApi.deleteContact(user.contactId || user.id)
    
    if (response.code === 0) {
      ElMessage.success('删除好友成功')
      await fetchData()
    } else {
      ElMessage.error(response.message || '删除好友失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除好友失败:', error)
      ElMessage.error('删除好友失败')
    }
  } finally {
    user.loading = false
  }
}

const handleAcceptRequest = async (user) => {
  try {
    user.loading = true
    const response = await contactApi.acceptRequest(user.requestId || user.id)
    
    if (response.code === 0) {
      ElMessage.success('已接受好友申请')
      await fetchData()
      // 刷新好友数量
      await fetchFriendsList()
    } else {
      ElMessage.error(response.message || '接受申请失败')
    }
  } catch (error) {
    console.error('接受申请失败:', error)
    ElMessage.error('接受申请失败')
  } finally {
    user.loading = false
  }
}

const handleRejectRequest = async (user) => {
  try {
    user.loading = true
    const response = await contactApi.rejectRequest(user.requestId || user.id)
    
    if (response.code === 0) {
      ElMessage.success('已拒绝好友申请')
      await fetchData()
    } else {
      ElMessage.error(response.message || '拒绝申请失败')
    }
  } catch (error) {
    console.error('拒绝申请失败:', error)
    ElMessage.error('拒绝申请失败')
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
  fetchData()
  // 同时获取两个标签页的数量
  fetchFriendsList()
  fetchRequestsList()
})
</script>

<style scoped>
.friend-list-page {
  padding: var(--space-20);
  max-width: 1200px;
  margin: 0 auto;
}

.header-card {
  margin-bottom: var(--space-20);
  background: var(--apple-bg-primary);
  border: 1px solid var(--apple-border-color);
  border-radius: var(--radius-lg);
}

.header-card h2 {
  margin: 0 0 var(--space-10) 0;
  color: var(--apple-text-primary);
  font-weight: 600;
}

.subtitle {
  margin: 0;
  color: var(--apple-text-secondary);
  font-size: 14px;
}

.tabs-card {
  margin-bottom: var(--space-20);
  background: var(--apple-bg-primary);
  border: 1px solid var(--apple-border-color);
  border-radius: var(--radius-lg);
}

.tab-label {
  display: flex;
  align-items: center;
  gap: var(--space-5);
}

.list-card {
  min-height: 400px;
  background: var(--apple-bg-primary);
  border: 1px solid var(--apple-border-color);
  border-radius: var(--radius-lg);
}

.empty-state {
  padding: 60px 0;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-15);
}

.user-item {
  display: flex;
  align-items: center;
  padding: var(--space-15);
  border: 1px solid var(--apple-border-color);
  border-radius: var(--radius-md);
  transition: all 0.3s ease;
  background: var(--apple-bg-secondary);
}

.user-item:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: var(--apple-blue-light);
}

.user-avatar {
  margin-right: var(--space-15);
}

.user-info {
  flex: 1;
}

.user-name {
  display: flex;
  align-items: center;
  gap: var(--space-10);
  margin-bottom: var(--space-5);
}

.username-link {
  font-size: 16px;
  font-weight: 500;
  color: var(--apple-text-primary);
  text-decoration: none;
  transition: color 0.2s ease;
}

.username-link:hover {
  color: var(--apple-blue);
}

.user-bio {
  color: var(--apple-text-secondary);
  font-size: 14px;
  margin-bottom: var(--space-8);
}

.user-stats {
  display: flex;
  gap: var(--space-15);
  font-size: 13px;
  color: var(--apple-text-tertiary);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.user-actions {
  margin-left: var(--space-15);
  display: flex;
  gap: var(--space-8);
}

.pagination {
  margin-top: var(--space-20);
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .friend-list-page {
    padding: var(--space-10);
  }

  .user-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .user-avatar {
    margin-right: 0;
    margin-bottom: var(--space-10);
  }

  .user-actions {
    margin-left: 0;
    margin-top: var(--space-10);
    width: 100%;
  }

  .user-actions .el-button {
    flex: 1;
  }
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .header-card,
  .tabs-card,
  .list-card {
    background: var(--apple-bg-primary-dark);
    border-color: var(--apple-border-color-dark);
  }

  .user-item {
    background: var(--apple-bg-secondary-dark);
    border-color: var(--apple-border-color-dark);
  }

  .user-item:hover {
    border-color: var(--apple-blue);
  }
}
</style>
