<template>
  <div class="group-page-container">
    <div class="page-header">
      <h1>群组管理</h1>
      <el-button type="primary" @click="openCreateGroupDialog">
        <el-icon><Plus /></el-icon> 创建群组
      </el-button>
    </div>

    <el-tabs v-model="activeTab" class="group-tabs">
      <el-tab-pane label="我管理的群组" name="managedGroups">
        <div v-if="loadingManagedGroups" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="managedGroups.length === 0" class="empty-state">
          <el-empty description="您还没有创建任何群组，快创建一个吧！" />
        </div>
        <div v-else class="group-list">
          <el-card v-for="group in managedGroups" :key="group.id" shadow="hover" class="group-card" @click="navigateToGroupDetail(group.id)">
            <template #header>
              <div class="card-header">
                <span>{{ group.groupName }}</span>
                <el-tag type="success">群主</el-tag>
              </div>
            </template>
            <div class="group-info">
              <p>群ID: {{ group.id }}</p>
              <p>成员数: {{ group.memberCount }}</p>
              <p>创建时间: {{ formatDate(group.createdAt) }}</p>
            </div>
            <template #footer>
              <div class="card-footer">
                <el-button type="primary" text @click.stop="navigateToGroupChat(group.id, group.groupName)">进入群聊</el-button>
                <el-button type="info" text @click.stop="navigateToGroupDetail(group.id)">管理群组</el-button>
              </div>
            </template>
          </el-card>
        </div>

        <!-- 我管理的群组分页 -->
        <div class="pagination-container" v-if="managedGroups.length > 0">
          <el-pagination
            v-model:current-page="managedPagination.page"
            v-model:page-size="managedPagination.pageSize"
            :total="managedPagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="(size) => { managedPagination.pageSize = size; fetchManagedGroups(1, size); }"
            @current-change="(page) => fetchManagedGroups(page, managedPagination.pageSize)"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="我加入的群组" name="joinedGroups">
        <div v-if="loadingJoinedGroups" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="joinedGroups.length === 0" class="empty-state">
          <el-empty description="您还没有加入任何群组，可以通过搜索或邀请加入" />
        </div>
        <div v-else class="group-list">
          <el-card v-for="group in joinedGroups" :key="group.id" shadow="hover" class="group-card" @click="navigateToGroupDetail(group.id)">
            <template #header>
              <div class="card-header">
                <span>{{ group.groupName }}</span>
                <el-tag :type="group.role === 'ADMIN' ? 'warning' : 'info'">
                  {{ group.role === 'ADMIN' ? '管理员' : '成员' }}
                </el-tag>
              </div>
            </template>
            <div class="group-info">
              <p>群ID: {{ group.id }}</p>
              <p>成员数: {{ group.memberCount }}</p>
              <p>群主: {{ group.ownerUsername || '未知' }}</p>
            </div>
            <template #footer>
              <div class="card-footer">
                <el-button type="primary" text @click.stop="navigateToGroupChat(group.id, group.groupName)">进入群聊</el-button>
                <el-button type="info" text @click.stop="navigateToGroupDetail(group.id)">查看详情</el-button>
                <el-button type="danger" text @click.stop="confirmLeaveGroup(group.id)" v-if="group.role !== 'OWNER'">退出群组</el-button>
              </div>
            </template>
          </el-card>
        </div>

        <!-- 我加入的群组分页 -->
        <div class="pagination-container" v-if="joinedGroups.length > 0">
          <el-pagination
            v-model:current-page="joinedPagination.page"
            v-model:page-size="joinedPagination.pageSize"
            :total="joinedPagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="(size) => { joinedPagination.pageSize = size; fetchJoinedGroups(1, size); }"
            @current-change="(page) => fetchJoinedGroups(page, joinedPagination.pageSize)"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="发现群组" name="discoverGroups">
        <div class="discover-groups-content">
          <el-input
            v-model="searchQuery"
            placeholder="搜索群组名称或ID"
            clearable
            @keyup.enter="searchPublicGroups"
            style="margin-bottom: 20px; max-width: 400px;"
          >
            <template #append>
              <el-button @click="searchPublicGroups"><el-icon><Search /></el-icon></el-button>
            </template>
          </el-input>

          <div v-if="loadingDiscoveredGroups" class="loading-state">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="discoveredGroups.length === 0 && !initialDiscoverLoad" class="empty-state">
            <el-empty description="没有搜索到相关群组，换个关键词试试？" />
          </div>
          <div v-else-if="discoveredGroups.length === 0 && initialDiscoverLoad" class="empty-state">
            <el-empty description="输入关键词搜索公开群组吧！" />
          </div>
          <div v-else class="group-list">
            <el-card v-for="group in discoveredGroups" :key="group.id" shadow="hover" class="group-card">
              <template #header>
                <div class="card-header">
                  <span>{{ group.groupName }}</span>
                </div>
              </template>
              <div class="group-info">
                <p>群ID: {{ group.id }}</p>
                <p>群主: {{ group.ownerUsername || '未知' }}</p>
                <p>成员数: {{ group.memberCount }}</p>
              </div>
              <template #footer>
                <el-button type="success" @click="applyToJoinGroup(group.id)" :disabled="isMemberOf(group.id)">
                  {{ isMemberOf(group.id) ? '已加入' : '申请加入' }}
                </el-button>
              </template>
            </el-card>
          </div>

          <!-- 发现群组分页 -->
          <div class="pagination-container" v-if="discoveredGroups.length > 0">
            <el-pagination
              v-model:current-page="discoveredPagination.page"
              v-model:page-size="discoveredPagination.pageSize"
              :total="discoveredPagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="(size) => { discoveredPagination.pageSize = size; searchPublicGroups(1, size); }"
              @current-change="(page) => searchPublicGroups(page, discoveredPagination.pageSize)"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建群组对话框 -->
    <el-dialog v-model="createGroupDialogVisible" title="创建新群组" width="500px" @close="resetCreateGroupForm">
      <el-form ref="createGroupFormRef" :model="createGroupForm" :rules="createGroupRules" label-width="80px">
        <el-form-item label="群组名称" prop="groupName">
          <el-input v-model="createGroupForm.groupName" placeholder="请输入群组名称" />
        </el-form-item>
        <el-form-item label="群简介" prop="description">
          <el-input v-model="createGroupForm.description" type="textarea" placeholder="（选填）请输入群简介" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createGroupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateGroup" :loading="creatingGroup">
          确定创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { useChatStore } from '@/stores/chatStore';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { Plus, Search } from '@element-plus/icons-vue';
import groupApi from '@/api/modules/group';
import searchApi from '@/api/modules/search';

const router = useRouter();
const authStore = useAuthStore();
const chatStore = useChatStore();

const activeTab = ref('managedGroups');
const managedGroups = ref([]);
const joinedGroups = ref([]);
const discoveredGroups = ref([]);
const loadingManagedGroups = ref(false);
const loadingJoinedGroups = ref(false);
const loadingDiscoveredGroups = ref(false);
const initialDiscoverLoad = ref(true);
const searchQuery = ref('');

// 分页相关
const managedPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
});

const joinedPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
});

const discoveredPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
});

const createGroupDialogVisible = ref(false);
const creatingGroup = ref(false);
const createGroupFormRef = ref(null);
const createGroupForm = reactive({
  groupName: '',
  description: ''
});

const createGroupRules = {
  groupName: [
    { required: true, message: '请输入群组名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ]
};

// 获取我管理的群组
const fetchManagedGroups = async (page = 1, pageSize = 10) => {
  loadingManagedGroups.value = true;
  try {
    // 使用新的获取我作为群主的群组API
    const response = await groupApi.getUserOwnedGroups();
    if (response.code === 0 && response.data) {
      // Handle new GroupDto field structure - map createTime to createdAt if needed
      managedGroups.value = response.data.map(group => ({
        ...group,
        createdAt: group.createdAt || group.createTime,
        // Ensure consistent field naming
        ownerUsername: group.ownerUsername || group.owner?.username
      }));
      managedPagination.total = response.data.length;
      managedPagination.page = page;
      managedPagination.pageSize = pageSize;
    } else {
      managedGroups.value = [];
      managedPagination.total = 0;
      ElMessage.error(response.message || '获取管理的群组失败');
    }
  } catch (error) {
    console.error('获取管理的群组失败:', error);
    ElMessage.error('获取管理的群组失败');
    managedGroups.value = [];
    managedPagination.total = 0;
  } finally {
    loadingManagedGroups.value = false;
  }
};

// 获取我加入的群组
const fetchJoinedGroups = async (page = 1, pageSize = 10) => {
  loadingJoinedGroups.value = true;
  try {
    const response = await groupApi.getUserJoinedGroups();
    if (response.code === 0 && response.data) {
      // Handle new GroupDto field structure and filter out owned groups
      const memberGroups = response.data
        .filter(group => group.ownerId !== authStore.currentUser?.id)
        .map(group => ({
          ...group,
          createdAt: group.createdAt || group.createTime,
          // Ensure consistent field naming
          ownerUsername: group.ownerUsername || group.owner?.username,
          // Map role field properly (1 for owner, other values for member roles)
          role: group.role === 1 ? 'OWNER' : (group.role === 2 ? 'ADMIN' : 'MEMBER')
        }));

      joinedGroups.value = memberGroups;
      joinedPagination.total = memberGroups.length;
      joinedPagination.page = page;
      joinedPagination.pageSize = pageSize;
    } else {
      joinedGroups.value = [];
      joinedPagination.total = 0;
      ElMessage.error(response.message || '获取加入的群组失败');
    }
  } catch (error) {
    console.error('获取加入的群组失败:', error);
    ElMessage.error('获取加入的群组失败');
    joinedGroups.value = [];
    joinedPagination.total = 0;
  } finally {
    loadingJoinedGroups.value = false;
  }
};

// 搜索公开群组
const searchPublicGroups = async (page = 1, pageSize = 10) => {
  if (!searchQuery.value.trim()) {
    discoveredGroups.value = [];
    initialDiscoverLoad.value = true;
    return;
  }

  loadingDiscoveredGroups.value = true;
  initialDiscoverLoad.value = false;

  try {
    const response = await searchApi.searchGroups(searchQuery.value, page - 1, pageSize);
    if (response.code === 0 && response.data) {
      // Handle new GroupDto field structure
      const groups = (response.data.list || response.data || []).map(group => ({
        ...group,
        createdAt: group.createdAt || group.createTime,
        ownerUsername: group.ownerUsername || group.owner?.username
      }));
      
      discoveredGroups.value = groups;
      discoveredPagination.total = response.data.total || groups.length;
      discoveredPagination.page = page;
      discoveredPagination.pageSize = pageSize;
    } else {
      discoveredGroups.value = [];
      discoveredPagination.total = 0;
      ElMessage.error(response.message || '搜索群组失败');
    }
  } catch (error) {
    console.error('搜索群组失败:', error);
    ElMessage.error('搜索群组失败');
    discoveredGroups.value = [];
    discoveredPagination.total = 0;
  } finally {
    loadingDiscoveredGroups.value = false;
  }
};

// 创建群组对话框
const openCreateGroupDialog = () => {
  createGroupDialogVisible.value = true;
};

// 重置创建群组表单
const resetCreateGroupForm = () => {
  if (createGroupFormRef.value) {
    createGroupFormRef.value.resetFields();
  }
  createGroupForm.groupName = '';
  createGroupForm.description = '';
};

// 处理创建群组
const handleCreateGroup = async () => {
  if (!createGroupFormRef.value) return;

  await createGroupFormRef.value.validate(async (valid) => {
    if (valid) {
      creatingGroup.value = true;
      try {
        const payload = {
          groupName: createGroupForm.groupName,
          description: createGroupForm.description,
        };

        const response = await groupApi.createGroup(payload);
        if (response.code === 0) {
          ElMessage.success('群组创建成功');
          createGroupDialogVisible.value = false;
          resetCreateGroupForm();
          fetchManagedGroups();
          fetchJoinedGroups();
        } else {
          ElMessage.error(response.message || '群组创建失败');
        }
      } catch (error) {
        console.error('创建群组失败:', error);
        ElMessage.error(error.response?.data?.message || error.message || '创建群组失败');
      } finally {
        creatingGroup.value = false;
      }
    }
  });
};

// 申请加入群组
const applyToJoinGroup = async (groupId) => {
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
      
      if (response.code === 0) {
        ElMessage.success('申请加入成功');
        // 刷新已加入的群组列表
        fetchJoinedGroups();
        searchPublicGroups();
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

// 确认退出群组
const confirmLeaveGroup = (groupId) => {
  ElMessageBox.confirm('确定要退出该群组吗？退出后将无法接收群消息。', '提示', {
    confirmButtonText: '确定退出',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const loading = ElLoading.service({ text: '正在退出...' });
    try {
      const response = await groupApi.leaveGroup(groupId);
      if (response.code === 0) {
        ElMessage.success('已成功退出群组');
        fetchJoinedGroups();
      } else {
        ElMessage.error(response.message || '退出失败');
      }
    } catch (error) {
      console.error('退出群组失败:', error);
      ElMessage.error(error.response?.data?.message || error.message || '退出失败');
    } finally {
      loading.close();
    }
  }).catch(() => {
    // 用户取消
  });
};

// 检查是否已经是成员
const isMemberOf = (groupId) => {
  return joinedGroups.value.some(group => group.id === groupId) ||
         managedGroups.value.some(group => group.id === groupId);
};

// 导航到群组详情页
const navigateToGroupDetail = (groupId) => {
  router.push(`/group/${groupId}`);
};

// 导航到群组聊天
const navigateToGroupChat = (groupId, groupName) => {
  const groupChatSession = {
    id: groupId,
    name: groupName,
    avatar: '', // 可以从群组信息中获取
    type: 'GROUP',
    unreadCount: 0,
    lastMessage: null,
    lastMessageTime: new Date().toISOString(),
  };
  chatStore.setActiveChat(groupChatSession);
  router.push('/chat');
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString();
};

// 组件挂载时加载数据
onMounted(() => {
  if (authStore.currentUser) {
    fetchManagedGroups();
    fetchJoinedGroups();
  } else {
    ElMessage.error('用户未登录，无法加载群组数据');
    router.replace('/login');
  }
});
</script>

<style scoped>
.group-page-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 1.8em;
  color: #303133;
  margin: 0;
}

.group-tabs {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.el-tabs__content {
  overflow-y: auto;
  height: calc(100% - 55px);
}

.group-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  padding-top: 10px;
}

.group-card {
  cursor: pointer;
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

.group-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.group-info p {
  font-size: 0.9em;
  color: #606266;
  margin: 5px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
  margin-top: 10px;
}

.empty-state, .loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  color: #909399;
}

.discover-groups-content {
  padding: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
