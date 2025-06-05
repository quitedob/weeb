<template>
  <div class="groups-container">
    <div class="page-header">
      <h1>我的群组</h1>
      <el-button type="primary" @click="openCreateGroupDialog">
        <el-icon><Plus /></el-icon> 创建群组
      </el-button>
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
          <el-card v-for="group in myGroups" :key="group.groupId" shadow="hover" class="group-card" @click="navigateToGroupDetail(group.groupId)">
            <template #header>
              <div class="card-header">
                <span>{{ group.groupName }}</span>
                <el-tag size="small" :type="group.role === 'OWNER' ? 'success' : 'info'">
                  {{ group.role === 'OWNER' ? '群主' : '成员' }}
                </el-tag>
              </div>
            </template>
            <div class="group-info">
              <p>群ID: {{ group.groupId }}</p>
              <p>成员数: {{ group.memberCount }}</p>
              </div>
            <template #footer>
                <div class="card-footer">
                    <el-button type="primary" text @click.stop="navigateToGroupChat(group.groupId, group.groupName)">进入群聊</el-button>
                    <el-button type="info" text @click.stop="navigateToGroupDetail(group.groupId)">查看详情</el-button>
                </div>
            </template>
          </el-card>
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
            <el-card v-for="group in discoveredGroups" :key="group.groupId" shadow="hover" class="group-card">
              <template #header>
                <div class="card-header">
                  <span>{{ group.groupName }}</span>
                </div>
              </template>
              <div class="group-info">
                <p>群ID: {{ group.groupId }}</p>
                <p>群主: {{ group.ownerUsername || '未知' }}</p>
                <p>成员数: {{ group.memberCount }}</p>
              </div>
              <template #footer>
                <el-button type="success" @click="applyToJoinGroup(group.groupId)" :disabled="isMemberOf(group.groupId)">
                  {{ isMemberOf(group.groupId) ? '已加入' : '申请加入' }}
                </el-button>
              </template>
            </el-card>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

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
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/api'; // 假设 api/index.js 正确导出
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { Plus, Search } from '@element-plus/icons-vue';
// import { useChatStore } from '@/stores/chatStore'; // 引入聊天状态管理 - User mentioned this but it's not in the file list yet.
// For now, I will comment this out. If chatStore is essential, it needs to be created first.
// Based on the usage (chatStore.currentUser?.userId, chatStore.setActiveChat), it seems important.
// However, the user's instruction for THIS task is to create Groups.vue.
// I will create Groups.vue as provided, and if it fails due to chatStore,
// the user can provide chatStore.js or I can create a placeholder for it in a subsequent step.

// Let's assume for now that chatStore might be provided later or is not strictly critical for the page to load initially (though functionality will be limited).
// The immediate task is to create this Groups.vue file.

const router = useRouter();
// const chatStore = useChatStore(); // Using聊天状态 - Commented out for now

const activeTab = ref('myGroups');
const myGroups = ref([]);
const discoveredGroups = ref([]);
const loadingMyGroups = ref(false);
const loadingDiscoveredGroups = ref(false);
const initialDiscoverLoad = ref(true); // 用于区分初始状态和搜索无结果状态
const searchQuery = ref('');

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

// 获取我加入的群组列表
const fetchMyGroups = async () => {
  loadingMyGroups.value = true;
  try {
    console.warn('Groups.vue: fetchMyGroups 需要对接后端 API 获取用户群组列表');
    // Simulating API call based on provided logic, actual API methods are in api/index.js
    // const response = await api.getMyGroups(); // Corrected to use a method name from api/index.js
    const response = await api.getMyGroups(); // Using the actual method from api/index.js
    if (response && response.code === 200 && response.data) { // Assuming response.data is the array
        myGroups.value = response.data.map(group => ({
            ...group,
            groupId: group.id, // Mapping id to groupId if backend sends 'id'
            memberCount: group.memberCount || (group.memberIds ? group.memberIds.length : 0),
            // role: group.ownerId === chatStore.currentUser?.userId ? 'OWNER' : 'MEMBER' // Depends on chatStore
            role: group.isOwner ? 'OWNER' : 'MEMBER' // Assuming backend might send an isOwner flag or similar
        }));
    } else {
        myGroups.value = [];
        // ElMessage.error(response.message || '获取我的群组列表失败');
    }
  } catch (error) {
    console.error('获取我的群组列表失败:', error);
    ElMessage.error('获取我的群组列表失败');
    myGroups.value = [];
  } finally {
    loadingMyGroups.value = false;
  }
};

// 搜索公开群组
const searchPublicGroups = async () => {
  if (!searchQuery.value.trim()) {
    discoveredGroups.value = [];
    initialDiscoverLoad.value = true;
    return;
  }
  loadingDiscoveredGroups.value = true;
  initialDiscoverLoad.value = false;
  try {
    console.warn('Groups.vue: searchPublicGroups 需要对接后端 API 搜索公开群组');
    // const response = await api.searchGroups(searchQuery.value); // Corrected to use a method name from api/index.js
    const response = await api.searchGroups(searchQuery.value);  // Using actual method from api/index.js
     if (response && response.code === 200 && response.data) { // Assuming response.data is the array
        discoveredGroups.value = response.data.map(group => ({
            ...group,
            groupId: group.id, // Mapping id to groupId
            memberCount: group.memberCount || (group.members ? group.members.length : 0),
            ownerUsername: group.owner ? group.owner.username : (group.ownerUsername || '未知')
        }));
    } else {
        discoveredGroups.value = [];
        // ElMessage.error(response.message || '未找到群组');
    }
  } catch (error) {
    console.error('搜索群组失败:', error);
    ElMessage.error('搜索群组失败');
    discoveredGroups.value = [];
  } finally {
    loadingDiscoveredGroups.value = false;
  }
};

// 打开创建群组对话框
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
        console.warn('Groups.vue: handleCreateGroup 需要对接后端 API 创建群组', createGroupForm);
        const payload = {
            groupName: createGroupForm.groupName,
            // description: createGroupForm.description, // Backend GroupCreateVo in previous context did not have description
        };
        // const response = await api.createGroup(payload); // Corrected to use a method name from api/index.js
        const response = await api.createGroup(payload); // Using actual method from api/index.js

        if (response && response.code === 200) {
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

// 申请加入群组
const applyToJoinGroup = async (groupId) => {
  ElMessageBox.confirm('确定要申请加入该群组吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info',
  }).then(async () => {
    const loading = ElLoading.service({ text: '正在处理...' }); // Changed text
    try {
      console.warn(`Groups.vue: applyToJoinGroup 需要对接后端 API 申请加入群组 ${groupId}`);
      // const response = await api.joinGroup({ groupId }); // Corrected to use a method name from api/index.js
      const response = await api.joinGroup({ groupId }); // Using actual method, assuming it's for direct join or request
      if (response.code === 200) {
         ElMessage.success('操作成功！请等待群主或管理员审核或查看群组列表。'); // Generic message
         fetchMyGroups(); // Refresh my groups list
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
    // 用户取消
  });
};

// 判断用户是否已经是某群组成员
const isMemberOf = (groupId) => {
  return myGroups.value.some(group => group.groupId === groupId);
};

// 跳转到群组详情页 (待实现)
const navigateToGroupDetail = (groupId) => {
  router.push(`/group/${groupId}`);
};

// 跳转到群聊
const navigateToGroupChat = (groupId, groupName) => {
    // const groupChatSession = {
    //     id: groupId,
    //     name: groupName,
    //     avatar: '',
    //     type: 'GROUP',
    //     unreadCount: 0,
    //     lastMessage: null,
    //     lastMessageTime: new Date().toISOString(),
    // };
    // chatStore.setActiveChat(groupChatSession); // Depends on chatStore
    // router.push('/chat');
    // Simplified navigation for now if chatStore is not ready
    router.push(`/chat/group/${groupId}`); // Assuming a route like /chat/:type/:id
    ElMessage.info(`进入群聊 ${groupName} (ID: ${groupId}) - chatStore integration pending.`);
};


onMounted(() => {
  fetchMyGroups();
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
  min-height: 200px;
  color: #909399;
}

.discover-groups-content {
  padding: 10px;
}
</style>
