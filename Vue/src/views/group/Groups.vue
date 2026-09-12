<template>
  <div class="groups-container">
    <div class="page-header">
      <h1>我的群组</h1>
      <div class="header-actions">
        <el-button @click="goToGroupManage">
          <el-icon><Setting /></el-icon> 管理群组
        </el-button>
        <el-button type="primary" @click="openCreateGroupDialog">
          <el-icon><Plus /></el-icon> 创建群组
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="group-tabs">
      <el-tab-pane label="我加入的群组" name="myGroups">
        <div v-if="loadingMyGroups" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="myGroups.length === 0" class="empty-state">
          <el-empty description="您还没有加入任何群组，快去发现或创建一个吧！" />
        </div>
        <div v-else class="group-list">
          <el-card v-for="group in myGroups" :key="group.id" shadow="hover" class="group-card" @click="navigateToGroupDetail(group.id)">
            <template #header>
              <div class="card-header">
                <span>{{ group.groupName }}</span>
                <el-tag size="small" :type="group.role === 'OWNER' ? 'success' : group.role === 'ADMIN' ? 'warning' : 'info'">
                  {{ group.role === 'OWNER' ? '群主' : group.role === 'ADMIN' ? '管理员' : '成员' }}
                </el-tag>
              </div>
            </template>
            <div class="group-info">
              <p>群ID: {{ group.id }}</p>
              <p>成员数: {{ group.memberCount }}</p>
              </div>
            <template #footer>
                <div class="card-footer">
                    <el-button type="primary" text @click.stop="navigateToGroupChat(group)">进入群聊</el-button>
                    <el-button type="info" text @click.stop="navigateToGroupDetail(group.id)">查看详情</el-button>
                </div>
            </template>
          </el-card>
        </div>
        <el-pagination v-if="myPagination.total > 0"
          v-model:current-page="myPagination.page" v-model:page-size="myPagination.pageSize"
          :total="myPagination.total" layout="total, sizes, prev, pager, next, jumper"
          @size-change="size => fetchMyGroups(1, size)"
          @current-change="page => fetchMyGroups(page, myPagination.pageSize)" />
      </el-tab-pane>

      <el-tab-pane label="发现群组" name="discoverGroups">
        <div class="discover-groups-content">
          <el-input
            v-model="searchQuery"
            placeholder="搜索群组名称或ID"
            clearable
            @keyup.enter="searchPublicGroups(1, discoveredPagination.pageSize)"
            style="margin-bottom: 20px; max-width: 400px;"
          >
            <template #append>
              <el-button @click="searchPublicGroups(1, discoveredPagination.pageSize)"><el-icon><Search /></el-icon></el-button>
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
                <p>群主: {{ group.ownerUsername || group.ownerId || '未知' }}</p>
                <p>成员数: {{ group.memberCount }}</p>
              </div>
              <template #footer>
                <el-button type="success" @click="applyToJoinGroup(group.id)" :disabled="isMemberOf(group)">
                  {{ isMemberOf(group) ? '已加入' : '申请加入' }}
                </el-button>
              </template>
            </el-card>
          </div>
        </div>
        <el-pagination v-if="discoveredPagination.total > 0"
          v-model:current-page="discoveredPagination.page" v-model:page-size="discoveredPagination.pageSize"
          :total="discoveredPagination.total" layout="total, sizes, prev, pager, next, jumper"
          @size-change="size => searchPublicGroups(1, size)"
          @current-change="page => searchPublicGroups(page, discoveredPagination.pageSize)" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="createGroupDialogVisible" title="创建新群组" width="500px" @close="resetCreateGroupForm">
      <el-form ref="createGroupFormRef" :model="createGroupForm" :rules="createGroupRules" label-width="80px">
        <el-form-item label="群组名称" prop="groupName">
          <el-input v-model="createGroupForm.groupName" placeholder="请输入群组名称" />
        </el-form-item>
        <el-form-item label="群简介" prop="groupDescription">
          <el-input v-model="createGroupForm.groupDescription" type="textarea" :maxlength="100" placeholder="（选填）请输入群简介" />
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
import { ref, onMounted, onUnmounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/api';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { Plus, Search, Setting } from '@element-plus/icons-vue';
import { useChatStore } from '@/stores/chatStore';
import { useAuthStore } from '@/stores/authStore'; // Added

import { createRequestScope } from '@/utils/requestScope';

const router = useRouter();
const chatStore = useChatStore();
const authStore = useAuthStore(); // Added

const myGroupsRequests = createRequestScope();
const discoveryRequests = createRequestScope();
onUnmounted(() => { myGroupsRequests.cancel(); discoveryRequests.cancel(); });
const myPagination = reactive({ page: 1, pageSize: 10, total: 0 });
const discoveredPagination = reactive({ page: 1, pageSize: 10, total: 0 });

const activeTab = ref('myGroups');
const myGroups = ref([]);
const discoveredGroups = ref([]);
const loadingMyGroups = ref(false);
const loadingDiscoveredGroups = ref(false);
const initialDiscoverLoad = ref(true);
const searchQuery = ref('');

const createGroupDialogVisible = ref(false);
const creatingGroup = ref(false);
const createGroupFormRef = ref(null);
const createGroupForm = reactive({
  groupName: '',
  groupDescription: ''
});

const createGroupRules = {
  groupName: [
    { required: true, message: '请输入群组名称', trigger: 'blur' },
    { min: 1, max: 20, message: '长度在 1 到 20 个字符', trigger: 'blur' }
  ]
};

const fetchMyGroups = async (page = myPagination.page, pageSize = myPagination.pageSize) => {
  const request = myGroupsRequests.begin();
  loadingMyGroups.value = true;
  try {
    const response = await api.group.getUserJoinedGroups({ page: page - 1, size: pageSize }, { signal: request.signal });
    if (!request.isCurrent()) return;
    if (response && response.code === 0 && response.data) {
        myGroups.value = response.data.list.map(group => ({
            ...group,
            role: group.currentUserRole || { 1: 'OWNER', 2: 'ADMIN', 3: 'MEMBER' }[Number(group.role)] ||
              (String(group.ownerId) === String(authStore.currentUser?.id) ? 'OWNER' : 'MEMBER')
        }));
        myPagination.total = response.data.total;
        const lastPage = Math.max(1, Math.ceil(response.data.total / pageSize));
        if (page > lastPage) return fetchMyGroups(lastPage, pageSize);
        myPagination.page = page;
        myPagination.pageSize = pageSize;
    } else {
        myGroups.value = [];
        ElMessage.error(response.message || '获取我的群组列表失败');
    }
  } catch (error) {
    if (!request.isCurrent()) return;
    console.error('获取我的群组列表失败:', error);
    ElMessage.error('获取我的群组列表失败');
    myGroups.value = [];
  } finally {
    if (request.isCurrent()) loadingMyGroups.value = false;
  }
};

const searchPublicGroups = async (page = 1, pageSize = discoveredPagination.pageSize) => {
  const request = discoveryRequests.begin();
  if (!searchQuery.value.trim()) {
    discoveredPagination.total = 0;
    if (request.isCurrent()) loadingDiscoveredGroups.value = false;
    discoveredGroups.value = [];
    initialDiscoverLoad.value = true;
    return;
  }
  loadingDiscoveredGroups.value = true;
  initialDiscoverLoad.value = false;
  try {
    const response = await api.search.searchGroups(searchQuery.value, page - 1, pageSize, {}, { signal: request.signal });
    if (!request.isCurrent()) return;
     if (response && response.code === 0 && response.data) {
        // Handle both list format and direct array format
        const groupList = response.data.list;
        discoveredPagination.total = response.data.total;
        discoveredPagination.page = page;
        discoveredPagination.pageSize = pageSize;
        discoveredGroups.value = groupList.map(group => ({
            ...group,
            // Assuming backend sends 'id', 'groupName', 'ownerUsername', 'memberCount'
        }));
    } else {
        discoveredGroups.value = [];
        // ElMessage.error(response.message || '未找到群组'); // Optionally show message
    }
  } catch (error) {
    if (!request.isCurrent()) return;
    console.error('搜索群组失败:', error);
    ElMessage.error('搜索群组失败');
    discoveredGroups.value = [];
  } finally {
    if (request.isCurrent()) loadingDiscoveredGroups.value = false;
  }
};

const openCreateGroupDialog = () => {
  createGroupDialogVisible.value = true;
};

const goToGroupManage = () => {
  router.push({ name: 'GroupManage' });
};

const resetCreateGroupForm = () => {
  if (createGroupFormRef.value) {
    createGroupFormRef.value.resetFields();
  }
  createGroupForm.groupName = '';
  createGroupForm.groupDescription = '';
};

const handleCreateGroup = async () => {
  if (!createGroupFormRef.value) return;
  await createGroupFormRef.value.validate(async (valid) => {
    if (valid) {
      creatingGroup.value = true;
      try {
        const payload = {
            groupName: createGroupForm.groupName,
            groupDescription: createGroupForm.groupDescription,
        };
        const response = await api.group.createGroup(payload);

        if (response && response.code === 0) {
            ElMessage.success('群组创建成功');
            createGroupDialogVisible.value = false;
            fetchMyGroups();
            if (activeTab.value === 'discoverGroups') {
                searchQuery.value = '';
                discoveredGroups.value = [];
                initialDiscoverLoad.value = true;
            }
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

const applyToJoinGroup = async (groupId) => {
  ElMessageBox.confirm('确定要申请加入该群组吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info',
  }).then(async () => {
    const loading = ElLoading.service({ text: '正在处理...' });
    try {
      const response = await api.group.applyToJoinGroup({
        groupId,
        message: '请允许我加入群组'
      });
      if (response.code === 0) {
         ElMessage.success('操作成功！请等待群主或管理员审核或查看群组列表。');
         fetchMyGroups();
      } else {
         ElMessage.error(response.message || '操作失败');
      }
    } catch (error) {
      console.error(`申请加入群组 ${groupId} 失败:`, error);
      ElMessage.error(error.response?.data?.message || error.message || '操作失败');
    } finally {
      loading.close();
    }
  }).catch(() => {
    // User cancelled
  });
};

const isMemberOf = group => ['OWNER', 'ADMIN', 'MEMBER'].includes(group.currentUserRole);
const navigateToGroupDetail = (groupId) => {
  router.push(`/group/${groupId}`);
};

const navigateToGroupChat = async (selectedGroup) => {
  try {
    let groupData = selectedGroup;
    if (!groupData.sharedChatId) {
      const response = await api.group.getGroupDetails(selectedGroup.id);
      if (response.code !== 0 || !response.data) throw new Error(response.message || '获取群组信息失败');
      groupData = response.data;
    }
    if (groupData.currentUserRole === 'NON_MEMBER') throw new Error('您不是该群组成员，无法进入群聊');
    if (!groupData.sharedChatId) throw new Error('群聊信息不完整，请稍后重试');
    chatStore.setActiveChat({
      id: groupData.sharedChatId,
      sharedChatId: groupData.sharedChatId,
      groupId: selectedGroup.id,
      name: groupData.groupName,
      avatar: groupData.groupAvatarUrl || '',
      type: 'GROUP'
    });
    router.push({ path: '/chat', query: {
      chatId: String(groupData.sharedChatId), type: 'GROUP', groupId: String(selectedGroup.id)
    } });
  } catch (error) {
    ElMessage.error(error.message || '进入群聊失败');
  }
};


onMounted(() => {
  fetchMyGroups();
  // Optionally, load some initial discovered groups or popular groups if desired
  // searchPublicGroups(); // Example: if you want to show some groups on initial load of "Discover"
});
</script>

<style scoped>
.groups-container {
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

.header-actions {
  display: flex;
  gap: 10px;
}

.page-header h1 {
  font-size: 1.8em;
  color: #303133;
}

.group-tabs {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.el-tabs__content {
    overflow-y: auto;
    /* Set a fixed height or ensure parent has height for calc to work effectively */
    /* height: calc(100% - 55px); */ /* Adjust 55px based on actual tab header height */
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
.group-description {
    font-size: 0.85em;
    color: #909399;
    margin-top: 8px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
    min-height: 34px;
}

.card-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    padding-top: 10px;
    border-top: 1px solid #ebeef5;
    margin-top:10px;
}

.empty-state, .loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px; /* Ensure it takes some space */
  color: #909399;
}

.discover-groups-content {
  padding: 10px;
}
</style>
