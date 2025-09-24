<template>
  <div class="search-page-container">
    <div class="page-header">
      <h1>搜索</h1>
    </div>

    <div class="search-section">
      <el-input
        v-model="searchQuery"
        placeholder="搜索用户、群组或消息..."
        clearable
        @keyup.enter="performSearch"
        class="search-input"
      >
        <template #append>
          <el-button @click="performSearch" :loading="searching">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
    </div>

    <el-tabs v-model="activeTab" class="search-tabs" v-if="hasSearched">
      <el-tab-pane label="用户" name="users">
        <div v-if="loadingUsers" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="userResults.length === 0" class="empty-state">
          <el-empty description="没有找到相关用户" />
        </div>
        <div v-else class="search-results">
          <el-card v-for="user in userResults" :key="user.id" shadow="hover" class="result-card">
            <template #header>
              <div class="card-header">
                <div class="user-info">
                  <div class="user-avatar">
                    <img v-if="user.avatar" :src="user.avatar" :alt="user.username" />
                    <div v-else class="avatar-placeholder">
                      {{ user.username?.charAt(0)?.toUpperCase() || 'U' }}
                    </div>
                  </div>
                  <div class="user-details">
                    <span class="username">{{ user.username }}</span>
                    <span class="nickname">{{ user.nickname || '暂无昵称' }}</span>
                  </div>
                </div>
              </div>
            </template>
            <div class="user-stats" v-if="user.stats">
              <p>粉丝数: {{ user.stats.fansCount || 0 }}</p>
              <p>点赞数: {{ user.stats.totalLikes || 0 }}</p>
              <p>文章数: {{ user.stats.articleCount || 0 }}</p>
            </div>
            <template #footer>
              <div class="card-footer">
                <el-button type="primary" text @click="viewUserProfile(user.id)">查看资料</el-button>
                <el-button type="success" text @click="startChat(user)">发送消息</el-button>
                <el-button type="info" text @click="addContact(user)">添加好友</el-button>
              </div>
            </template>
          </el-card>
        </div>

        <!-- 用户搜索分页 -->
        <div class="pagination-container" v-if="userResults.length > 0">
          <el-pagination
            v-model:current-page="userPagination.page"
            v-model:page-size="userPagination.pageSize"
            :total="userPagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="(size) => { userPagination.pageSize = size; searchUsers(1, size); }"
            @current-change="(page) => searchUsers(page, userPagination.pageSize)"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="群组" name="groups">
        <div v-if="loadingGroups" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="groupResults.length === 0" class="empty-state">
          <el-empty description="没有找到相关群组" />
        </div>
        <div v-else class="search-results">
          <el-card v-for="group in groupResults" :key="group.id" shadow="hover" class="result-card">
            <template #header>
              <div class="card-header">
                <div class="group-info">
                  <div class="group-avatar">
                    <img v-if="group.groupAvatarUrl" :src="group.groupAvatarUrl" :alt="group.groupName" />
                    <div v-else class="avatar-placeholder">
                      {{ group.groupName?.charAt(0)?.toUpperCase() || 'G' }}
                    </div>
                  </div>
                  <div class="group-details">
                    <span class="group-name">{{ group.groupName }}</span>
                    <span class="group-owner">群主: {{ group.ownerUsername || '未知' }}</span>
                  </div>
                </div>
              </div>
            </template>
            <div class="group-stats">
              <p>群ID: {{ group.id }}</p>
              <p>成员数: {{ group.memberCount || 0 }}</p>
              <p>创建时间: {{ formatDate(group.createTime) }}</p>
            </div>
            <template #footer>
              <div class="card-footer">
                <el-button type="primary" text @click="viewGroupDetail(group.id)">查看详情</el-button>
                <el-button type="success" text @click="joinGroup(group.id)" :disabled="isMemberOf(group.id)">
                  {{ isMemberOf(group.id) ? '已加入' : '申请加入' }}
                </el-button>
              </div>
            </template>
          </el-card>
        </div>

        <!-- 群组搜索分页 -->
        <div class="pagination-container" v-if="groupResults.length > 0">
          <el-pagination
            v-model:current-page="groupPagination.page"
            v-model:page-size="groupPagination.pageSize"
            :total="groupPagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="(size) => { groupPagination.pageSize = size; searchGroups(1, size); }"
            @current-change="(page) => searchGroups(page, groupPagination.pageSize)"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="消息" name="messages">
        <div v-if="loadingMessages" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="messageResults.length === 0" class="empty-state">
          <el-empty description="没有找到相关消息" />
        </div>
        <div v-else class="search-results">
          <el-card v-for="message in messageResults" :key="message.id" shadow="hover" class="result-card">
            <template #header>
              <div class="card-header">
                <div class="message-info">
                  <span class="sender-name">{{ message.senderName }}</span>
                  <span class="message-time">{{ formatDate(message.createTime) }}</span>
                </div>
              </div>
            </template>
            <div class="message-content">
              <p>{{ message.content }}</p>
            </div>
            <template #footer>
              <div class="card-footer">
                <el-button type="primary" text @click="goToMessage(message)">查看消息</el-button>
              </div>
            </template>
          </el-card>
        </div>

        <!-- 消息搜索分页 -->
        <div class="pagination-container" v-if="messageResults.length > 0">
          <el-pagination
            v-model:current-page="messagePagination.page"
            v-model:page-size="messagePagination.pageSize"
            :total="messagePagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="(size) => { messagePagination.pageSize = size; searchMessages(1, size); }"
            @current-change="(page) => searchMessages(page, messagePagination.pageSize)"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <div v-else class="welcome-state">
      <el-empty description="输入关键词开始搜索吧！">
        <el-icon size="64" color="#c0c4cc"><Search /></el-icon>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { useChatStore } from '@/stores/chatStore';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { Search } from '@element-plus/icons-vue';
import searchApi from '@/api/modules/search';
import groupApi from '@/api/modules/group';
import contactApi from '@/api/modules/contact';

const router = useRouter();
const authStore = useAuthStore();
const chatStore = useChatStore();

const searchQuery = ref('');
const activeTab = ref('users');
const searching = ref(false);
const hasSearched = ref(false);

const userResults = ref([]);
const groupResults = ref([]);
const messageResults = ref([]);

const loadingUsers = ref(false);
const loadingGroups = ref(false);
const loadingMessages = ref(false);

// 分页相关
const userPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
});

const groupPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
});

const messagePagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
});

// 用户加入的群组列表（用于判断是否已加入）
const userGroups = ref([]);

// 执行搜索
const performSearch = async () => {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入搜索关键词');
    return;
  }

  searching.value = true;
  hasSearched.value = true;

  try {
    // 并行搜索所有类型
    await Promise.all([
      searchUsers(),
      searchGroups(),
      searchMessages()
    ]);
  } catch (error) {
    console.error('搜索失败:', error);
    ElMessage.error('搜索失败');
  } finally {
    searching.value = false;
  }
};

// 搜索用户
const searchUsers = async (page = 1, pageSize = 10) => {
  loadingUsers.value = true;
  try {
    const response = await searchApi.searchUsers(searchQuery.value, page - 1, pageSize);
    if (response.code === 200 && response.data) {
      userResults.value = response.data.list || [];
      userPagination.total = response.data.total || 0;
      userPagination.page = page;
      userPagination.pageSize = pageSize;
    } else {
      userResults.value = [];
      userPagination.total = 0;
    }
  } catch (error) {
    console.error('搜索用户失败:', error);
    userResults.value = [];
    userPagination.total = 0;
  } finally {
    loadingUsers.value = false;
  }
};

// 搜索群组
const searchGroups = async (page = 1, pageSize = 10) => {
  loadingGroups.value = true;
  try {
    const response = await searchApi.searchGroups(searchQuery.value, page - 1, pageSize);
    if (response.code === 200 && response.data) {
      groupResults.value = response.data.list || [];
      groupPagination.total = response.data.total || 0;
      groupPagination.page = page;
      groupPagination.pageSize = pageSize;
    } else {
      groupResults.value = [];
      groupPagination.total = 0;
    }
  } catch (error) {
    console.error('搜索群组失败:', error);
    groupResults.value = [];
    groupPagination.total = 0;
  } finally {
    loadingGroups.value = false;
  }
};

// 搜索消息
const searchMessages = async (page = 1, pageSize = 10) => {
  loadingMessages.value = true;
  try {
    const response = await searchApi.searchMessages(searchQuery.value, page - 1, pageSize);
    if (response.code === 200 && response.data) {
      messageResults.value = response.data.list || [];
      messagePagination.total = response.data.total || 0;
      messagePagination.page = page;
      messagePagination.pageSize = pageSize;
    } else {
      messageResults.value = [];
      messagePagination.total = 0;
    }
  } catch (error) {
    console.error('搜索消息失败:', error);
    messageResults.value = [];
    messagePagination.total = 0;
  } finally {
    loadingMessages.value = false;
  }
};

// 查看用户资料
const viewUserProfile = (userId) => {
  router.push(`/user/${userId}`);
};

// 开始聊天
const startChat = (user) => {
  const chatSession = {
    id: user.id,
    name: user.username,
    avatar: user.avatar,
    type: 'PRIVATE',
    unreadCount: 0,
    lastMessage: null,
    lastMessageTime: new Date().toISOString(),
  };
  chatStore.setActiveChat(chatSession);
  router.push('/chat');
};

// 添加联系人
const addContact = async (user) => {
  ElMessageBox.prompt('请输入申请理由（可选）', '添加好友', {
    confirmButtonText: '发送申请',
    cancelButtonText: '取消',
    inputPlaceholder: '申请理由'
  }).then(async ({ value }) => {
    const loading = ElLoading.service({ text: '正在发送申请...' });
    try {
      const response = await contactApi.applyContact({
        username: user.username,
        remarks: value || '申请添加为好友'
      });
      
      if (response.code === 200) {
        ElMessage.success('好友申请发送成功');
      } else {
        ElMessage.error(response.message || '发送申请失败');
      }
    } catch (error) {
      console.error('发送好友申请失败:', error);
      ElMessage.error(error.response?.data?.message || error.message || '发送申请失败');
    } finally {
      loading.close();
    }
  }).catch(() => {
    // 用户取消
  });
};

// 查看群组详情
const viewGroupDetail = (groupId) => {
  router.push(`/group/${groupId}`);
};

// 加入群组
const joinGroup = async (groupId) => {
  ElMessageBox.confirm('确定要申请加入该群组吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info',
  }).then(async () => {
    const loading = ElLoading.service({ text: '正在处理...' });
    try {
      const response = await groupApi.applyToJoinGroup({
        groupId: groupId,
        reason: '申请加入群组'
      });
      
      if (response.code === 200) {
        ElMessage.success('申请加入成功');
        // 刷新用户群组列表
        fetchUserGroups();
      } else {
        ElMessage.error(response.message || '申请失败');
      }
    } catch (error) {
      console.error(`申请加入群组 ${groupId} 失败:`, error);
      ElMessage.error(error.response?.data?.message || error.message || '申请失败');
    } finally {
      loading.close();
    }
  }).catch(() => {
    // 用户取消
  });
};

// 获取用户群组列表
const fetchUserGroups = async () => {
  try {
    const response = await groupApi.getUserJoinedGroups();
    if (response.code === 200 && response.data) {
      userGroups.value = response.data;
    }
  } catch (error) {
    console.error('获取用户群组失败:', error);
  }
};

// 检查是否已经是群成员
const isMemberOf = (groupId) => {
  return userGroups.value.some(group => group.id === groupId);
};

// 跳转到消息
const goToMessage = (message) => {
  // 根据消息类型跳转到对应的聊天界面
  const chatSession = {
    id: message.targetId,
    name: message.targetName || message.senderName,
    avatar: '',
    type: message.type === 'GROUP' ? 'GROUP' : 'PRIVATE',
    unreadCount: 0,
    lastMessage: message.content,
    lastMessageTime: message.createTime,
  };
  chatStore.setActiveChat(chatSession);
  router.push('/chat');
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString();
};

// 组件挂载时获取用户群组列表
import { onMounted } from 'vue';
onMounted(() => {
  if (authStore.currentUser) {
    fetchUserGroups();
  }
});
</script>

<style scoped>
.search-page-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 1.8em;
  color: #303133;
  margin: 0;
}

.search-section {
  margin-bottom: 20px;
}

.search-input {
  max-width: 600px;
}

.search-tabs {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.el-tabs__content {
  overflow-y: auto;
  height: calc(100% - 55px);
}

.search-results {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  padding-top: 10px;
}

.result-card {
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

.result-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info, .group-info, .message-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar, .group-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar img, .group-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 18px;
}

.user-details, .group-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.username, .group-name {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}

.nickname, .group-owner {
  font-size: 14px;
  color: #909399;
}

.message-info {
  justify-content: space-between;
  width: 100%;
}

.sender-name {
  font-weight: bold;
  color: #303133;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.user-stats, .group-stats, .message-content {
  margin: 15px 0;
}

.user-stats p, .group-stats p {
  font-size: 0.9em;
  color: #606266;
  margin: 5px 0;
}

.message-content p {
  color: #303133;
  line-height: 1.5;
  margin: 0;
  word-break: break-word;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
  margin-top: 10px;
}

.empty-state, .loading-state, .welcome-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  color: #909399;
}

.welcome-state {
  flex-grow: 1;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
