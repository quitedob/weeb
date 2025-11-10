<template>
  <div class="group-detail-container el-card" v-if="!loading && group">
    <el-page-header @back="goBack" class="page-header">
      <template #content>
        <span class="text-large font-600 mr-3"> {{ group.groupName || '群组详情' }} </span>
      </template>
      <template #extra v-if="isOwner">
        <div class="page-header-extra">
          <el-button type="danger" @click="confirmDisbandGroup" plain><el-icon><Delete /></el-icon>解散群组</el-button>
        </div>
      </template>
    </el-page-header>

    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="群信息" name="info">
        <el-descriptions :column="1" border class="group-info-desc">
          <el-descriptions-item label="群名称">{{ group.groupName }}</el-descriptions-item>
          <el-descriptions-item label="群ID">{{ group.groupId }}</el-descriptions-item>
          <el-descriptions-item label="群主">
            {{ ownerInfo ? ownerInfo.username : group.ownerId }}
            <el-tag v-if="ownerInfo" size="small" style="margin-left: 5px;">{{ group.ownerId }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(group.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="群简介">{{ group.description || '暂无简介' }}</el-descriptions-item>
        </el-descriptions>
        <div class="actions-bar top-actions" v-if="isOwner || isAdmin">
             <el-button type="primary" @click="openEditGroupDialog"><el-icon><Edit /></el-icon>编辑群信息</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane label="申请管理" name="applications" v-if="isOwner || isAdmin">
        <div class="actions-bar">
          <el-button type="primary" @click="fetchApplications('pending')">
            <el-icon><Refresh /></el-icon>刷新待审批
          </el-button>
          <el-button @click="fetchApplications('all')">查看全部申请</el-button>
        </div>
        
        <div v-if="loadingApplications" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="applications.length === 0" class="empty-state">
          <el-empty description="暂无申请记录" />
        </div>
        <el-table v-else :data="applications" style="width: 100%" class="applications-table">
          <el-table-column label="申请人" width="150">
            <template #default="scope">
              <div class="user-info">
                <el-avatar :size="30" :src="scope.row.userAvatar">
                  {{ scope.row.username?.substring(0,1) }}
                </el-avatar>
                <span style="margin-left: 8px;">{{ scope.row.username || `用户${scope.row.userId}` }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="申请留言" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="getApplicationStatusType(scope.row.status)">
                {{ getApplicationStatusText(scope.row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" v-if="applicationStatus === 'pending'">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === 'PENDING'"
                size="small"
                type="success"
                @click="handleApproveApplication(scope.row)"
              >
                通过
              </el-button>
              <el-button
                v-if="scope.row.status === 'PENDING'"
                size="small"
                type="danger"
                @click="handleRejectApplication(scope.row)"
              >
                拒绝
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="`成员 (${members.length})`" name="members">
        <div class="actions-bar">
          <el-button v-if="isOwner || isAdmin" type="success" @click="openInviteDialog"><el-icon><Plus /></el-icon>邀请成员</el-button>
        </div>
        <el-table :data="members" style="width: 100%" class="members-table" empty-text="暂无成员">
          <el-table-column label="头像" width="80">
            <template #default="scope">
              <el-avatar :size="40" :src="scope.row.avatar || defaultAvatar">{{ scope.row.username?.substring(0,1) }}</el-avatar>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="昵称" />
          <el-table-column prop="userId" label="用户ID" />
          <el-table-column label="角色">
            <template #default="scope">
              <el-tag :type="getRoleTagType(scope.row.userId)">{{ getRoleText(scope.row.userId) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" v-if="isOwner || isAdmin">
            <template #default="scope">
              <el-button
                size="small"
                type="danger"
                @click="confirmKickMember(scope.row)"
                :disabled="scope.row.userId === group.ownerId || scope.row.userId === currentUser?.userId"
                v-if="isOwner || (isAdmin && scope.row.userId !== group.ownerId)"
                plain
              >
                踢出
              </el-button>
              </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <div class="actions-bar bottom-actions">
        <el-button type="primary" @click="navigateToGroupChat(group.id, group.groupName)" v-if="group"><el-icon><Search /></el-icon>进入群聊</el-button>
        <el-button type="warning" @click="confirmLeaveGroup" v-if="!isOwner" plain><el-icon><Remove /></el-icon>退出群组</el-button>
    </div>

    <el-dialog v-model="editGroupDialogVisible" title="编辑群信息" width="500px">
      <el-form ref="editGroupFormRef" :model="editGroupForm" label-width="80px">
        <el-form-item label="群名称" prop="groupName">
          <el-input v-model="editGroupForm.groupName" />
        </el-form-item>
        <el-form-item label="群简介" prop="description">
          <el-input v-model="editGroupForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editGroupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateGroup" :loading="isUpdatingGroup">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="inviteDialogVisible" title="邀请新成员" width="500px">
        <el-select
            v-model="usersToInvite"
            multiple
            filterable
            remote
            reserve-keyword
            placeholder="搜索用户昵称或ID邀请"
            :remote-method="searchUsersToInvite"
            :loading="searchingUsers"
            style="width: 100%"
        >
            <el-option
            v-for="item in searchableUsers"
            :key="item.id || item.userId"
            :label="`${item.username || item.nickname || 'Unknown'} (ID: ${item.id || item.userId})`"
            :value="item.id || item.userId"
            :disabled="isAlreadyMember(item.id || item.userId)"
            />
        </el-select>
      <template #footer>
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleInviteMembers" :loading="isInviting">确定邀请</el-button>
      </template>
    </el-dialog>
  </div>
  <div v-else-if="loading" class="loading-placeholder">
    <el-skeleton :rows="5" animated />
  </div>
  <div v-else class="empty-placeholder">
     <el-empty description="未找到群组信息或加载失败">
        <el-button type="primary" @click="goBack">返回上一页</el-button>
     </el-empty>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/api';
import { useAuthStore } from '@/stores/authStore';
import { useChatStore } from '@/stores/chatStore';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { Edit, User as UserIcon, Delete, Plus, ArrowLeft, Search, Remove, Refresh} from '@element-plus/icons-vue';
// 使用在线默认头像，避免文件缺失问题
const defaultAvatar = 'https://via.placeholder.com/40x40/cccccc/666666?text=用户';

// 申请管理相关状态
const applications = ref([]);
const loadingApplications = ref(false);
const applicationStatus = ref('pending'); // 'pending' or 'all'

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const chatStore = useChatStore();

const groupId = ref(route.params.groupId);
const group = ref(null);
const members = ref([]);
const ownerInfo = ref(null);
const loading = ref(true);
const activeTab = ref('info');

const currentUser = computed(() => authStore.currentUser);

// ✅ 修复：基于后端返回的currentUserRole判断权限
const userRole = computed(() => group.value?.currentUserRole || 'NON_MEMBER');
const isOwner = computed(() => userRole.value === 'OWNER');
const isAdmin = computed(() => ['OWNER', 'ADMIN'].includes(userRole.value));
const canManage = computed(() => ['OWNER', 'ADMIN'].includes(userRole.value));


const editGroupDialogVisible = ref(false);
const editGroupFormRef = ref(null);
const editGroupForm = reactive({ groupName: '', description: '' });
const isUpdatingGroup = ref(false);

const inviteDialogVisible = ref(false);
const usersToInvite = ref([]);
const searchableUsers = ref([]);
const searchingUsers = ref(false);
const isInviting = ref(false);

const fetchGroupDetails = async () => {
  loading.value = true;
  try {
    const detailsRes = await api.group.getGroupDetails(groupId.value);
    if (detailsRes.code === 0 && detailsRes.data) {
      group.value = detailsRes.data; // Assuming data is the group object
      // Backend sends 'id' for group, but component uses 'groupId' internally sometimes.
      // Let's ensure group.value.groupId is consistent if used, or stick to group.value.id.
      // The template uses group.groupId. Let's map it if necessary or ensure backend sends groupId.
      // For now, assuming detailsRes.data has { id, groupName, ownerId, createTime, description }
      // And we want to use group.value.groupId, group.value.groupName etc. in template.
      // If backend sends 'id', let's map it:
      if (group.value.id && !group.value.groupId) group.value.groupId = group.value.id;


      if (group.value.ownerId) {
          try {
            // ✅ 修复：使用正确的API方法名 getUserById
            const ownerRes = await api.user.getUserById(group.value.ownerId);
            if(ownerRes.code === 0 && ownerRes.data) ownerInfo.value = ownerRes.data;
          } catch (e) { console.warn("获取群主信息失败", e)}
      }
      await fetchGroupMembers();
    } else {
      ElMessage.error(detailsRes.message || '获取群组信息失败');
      group.value = null;
    }
  } catch (error) {
    console.error('获取群组详情失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '获取群组信息失败');
    group.value = null;
  } finally {
    loading.value = false;
  }
};

const fetchGroupMembers = async () => {
  try {
    // ✅ 修复：使用正确的API方法名 getMembers
    const membersRes = await api.group.getMembers(groupId.value);
     if (membersRes.code === 0 && membersRes.data) {
      members.value = membersRes.data.map(member => ({
          ...member,
          // Ensuring template properties like userId, username, avatar are present
          // Role might need to be determined based on ownerId or a specific field from backend
      }));
    } else {
      ElMessage.error(membersRes.message || '获取群成员失败');
      members.value = [];
    }
  } catch (error) {
    console.error('获取群成员失败:', error);
    ElMessage.error(error.response?.data?.message || error.message ||'获取群成员失败');
    members.value = [];
  }
};

onMounted(() => {
  if (groupId.value) {
    fetchGroupDetails();
  } else {
      loading.value = false;
      ElMessage.error('群组ID无效');
      // router.push('/groups'); // Optionally redirect
  }
});

const goBack = () => router.go(-1);

const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  // Assuming dateString is a valid format for Date constructor (e.g., ISO 8601)
  return new Date(dateString).toLocaleString();
};

const openEditGroupDialog = () => {
  if (!group.value) return;
  editGroupForm.groupName = group.value.groupName;
  editGroupForm.description = group.value.description || '';
  editGroupDialogVisible.value = true;
};

const handleUpdateGroup = async () => {
  if (!group.value) return;
  isUpdatingGroup.value = true;
  try {
    const payload = {
        // Backend GroupUpdateVo might expect 'id' or 'groupId'
        id: group.value.id, // Assuming backend uses 'id' for update operations
        groupName: editGroupForm.groupName,
        description: editGroupForm.description,
        // ownerId should not be updatable here typically
    };
    const response = await api.group.updateGroup(payload); // api.group.updateGroup expects one object
    if (response.code === 0) {
        ElMessage.success('群信息更新成功');
        fetchGroupDetails(); // Refresh group details
        editGroupDialogVisible.value = false;
    } else {
        ElMessage.error(response.message || '更新失败');
    }
  } catch (error) {
    console.error('更新群信息失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '更新群信息失败');
  } finally {
    isUpdatingGroup.value = false;
  }
};

const openInviteDialog = () => {
  usersToInvite.value = [];
  searchableUsers.value = [];
  inviteDialogVisible.value = true;
};

const searchUsersToInvite = async (query) => {
  if (query && query.trim().length > 0) {
    searchingUsers.value = true;
    try {
      // api.search.searchUsers returns { code, message, data: { list: [users], total } }
      // Each user { id, username, nickname, avatar }
      const response = await api.search.searchUsers(query);
      if (response.code === 0 && response.data) {
          // 后端返回的是 { list: [...], total: ... } 结构
          searchableUsers.value = response.data.list || response.data;
      } else {
          searchableUsers.value = [];
      }
    } catch (error) {
      console.error('搜索用户失败:', error);
      searchableUsers.value = [];
    } finally {
      searchingUsers.value = false;
    }
  } else {
    searchableUsers.value = [];
  }
};

const isAlreadyMember = (userIdToInvite) => { // Renamed parameter for clarity
    return members.value.some(member => 
        member.userId === userIdToInvite || 
        member.id === userIdToInvite ||
        String(member.userId) === String(userIdToInvite)
    );
};

const handleInviteMembers = async () => {
  if (usersToInvite.value.length === 0) {
    ElMessage.warning('请选择要邀请的用户');
    return;
  }
  isInviting.value = true;
  try {
    const payload = { groupId: group.value.id, memberIds: usersToInvite.value }; // Use group.id and correct field name
    const response = await api.group.inviteMembers(payload);
    if (response.code === 0) {
        ElMessage.success('邀请成功');
        fetchGroupMembers(); // Refresh member list
        inviteDialogVisible.value = false;
    } else {
        ElMessage.error(response.message || '邀请失败');
    }
  } catch (error) {
    console.error('邀请成员失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '邀请成员失败');
  } finally {
    isInviting.value = false;
  }
};

const confirmKickMember = (memberToKick) => {
  // Prevent kicking oneself if logic allows (though UI might disable button)
  if (memberToKick.userId === currentUser.value?.id) { // Corrected: currentUser.value.id to currentUser.value.userId
      ElMessage.warning("不能将自己踢出群组。");
      return;
  }
  // Prevent kicking owner (UI button should be disabled too)
  if (memberToKick.userId === group.value?.ownerId) {
      ElMessage.warning("不能踢出群主。");
      return;
  }

  ElMessageBox.confirm(`确定要将成员 "${memberToKick.username}" (ID: ${memberToKick.userId}) 踢出群组吗？`, '警告', {
    confirmButtonText: '确定踢出',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const loadingInstance = ElLoading.service({ text: '正在操作...' });
    try {
      const payload = { groupId: group.value.id, userIdToKick: memberToKick.userId }; // Use group.id, and consistent param name
      const response = await api.group.kickMember(payload); // kickMember expects {groupId, userIdToKick}
       if (response.code === 0) {
            ElMessage.success('成员已踢出');
            fetchGroupMembers(); // Refresh member list
        } else {
            ElMessage.error(response.message || '操作失败');
        }
    } catch (error) {
      console.error('踢出成员失败:', error);
      ElMessage.error(error.response?.data?.message || error.message || '踢出成员失败');
    } finally {
      loadingInstance.close();
    }
  }).catch(() => { /* User cancelled */ });
};

const confirmLeaveGroup = () => {
  if (isOwner.value) {
      ElMessage.warning("群主不能直接退出群组，请先转让群主或解散群组。");
      return;
  }
  ElMessageBox.confirm('确定要退出该群组吗？退出后将无法接收群消息。', '提示', {
    confirmButtonText: '确定退出',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const loadingInstance = ElLoading.service({ text: '正在退出...' });
    try {
      const response = await api.group.leaveGroup(group.value.id); // Use group.id
      if (response.code === 0) {
            ElMessage.success('已成功退出群组');
            router.push('/groups');
        } else {
            ElMessage.error(response.message || '操作失败');
        }
    } catch (error) {
      console.error('退出群组失败:', error);
      ElMessage.error(error.response?.data?.message || error.message || '退出群组失败');
    } finally {
      loadingInstance.close();
    }
  }).catch(() => { /* User cancelled */ });
};

const confirmDisbandGroup = () => {
  if (!isOwner.value) {
      ElMessage.error("只有群主才能解散群组。");
      return;
  }
  ElMessageBox.confirm('解散群组是不可恢复的操作，所有群成员将被移除。确定要解散该群组吗？', '严重警告', {
    confirmButtonText: '确定解散',
    cancelButtonText: '取消',
    type: 'error',
  }).then(async () => {
    const loadingInstance = ElLoading.service({ text: '正在解散...' });
    try {
      const response = await api.group.disbandGroup(group.value.id); // Use group.id
        if (response.code === 0) {
            ElMessage.success('群组已成功解散');
            router.push('/groups');
        } else {
            ElMessage.error(response.message || '操作失败');
        }
    } catch (error) {
      console.error('解散群组失败:', error);
      ElMessage.error(error.response?.data?.message || error.message || '解散群组失败');
    } finally {
      loadingInstance.close();
    }
  }).catch(() => { /* User cancelled */ });
};

const getRoleText = (memberUserId) => { // Renamed parameter for clarity
  if (group.value && memberUserId === group.value.ownerId) return '群主';
  // Add logic for ADMIN if members have a role property
  const member = members.value.find(m => m.userId === memberUserId);
  if (member && member.role === 'ADMIN') return '管理员'; // Assuming 'ADMIN' string from backend
  return '成员';
};
const getRoleTagType = (memberUserId) => { // Renamed parameter for clarity
  if (group.value && memberUserId === group.value.ownerId) return 'danger';
  const member = members.value.find(m => m.userId === memberUserId);
  if (member && member.role === 'ADMIN') return 'warning';
  return 'info';
};

const navigateToGroupChat = async (gId, gName) => {
    if (!gId || !group.value) {
        ElMessage.error('群组信息不完整');
        return;
    }

    console.log('🚀 准备导航到群聊:', { groupId: gId, groupName: gName, group: group.value });

    // ✅ 修复：先确保后端创建了 chat_list 记录
    try {
        // 重新获取群组详情，触发后端自动创建 chat_list
        const response = await api.group.getGroupDetails(gId);
        console.log('📦 群组详情响应:', response);
        
        if (response.code !== 0 || !response.data) {
            ElMessage.error('获取群组信息失败');
            return;
        }
        
        const groupData = response.data;
        
        // 检查用户是否是群成员
        if (groupData.currentUserRole === 'NON_MEMBER') {
            ElMessage.error('您不是该群组成员，无法进入群聊');
            return;
        }
        
        // ✅ 使用后端返回的 sharedChatId（如果有）
        const sharedChatId = groupData.sharedChatId || gId;
        
        console.log('📋 群组信息:', {
            groupId: gId,
            sharedChatId: sharedChatId,
            groupName: gName,
            currentUserRole: groupData.currentUserRole
        });
        
        // 构造群聊会话对象
        const groupChatSession = {
            id: sharedChatId,
            sharedChatId: sharedChatId,
            groupId: gId,
            name: gName,
            avatar: groupData.groupAvatarUrl || groupData.avatar || defaultAvatar,
            type: 'GROUP',
            targetInfo: gName
        };
        
        console.log('🎯 设置活跃聊天:', groupChatSession);
        chatStore.setActiveChat(groupChatSession);
        
        // 导航到聊天页面
        router.push({
            path: '/chat',
            query: {
                chatId: sharedChatId,
                type: 'GROUP',
                groupId: gId
            }
        });
        
        ElMessage.success('正在进入群聊...');
    } catch (error) {
        console.error('❌ 导航到群聊失败:', error);
        ElMessage.error('进入群聊失败: ' + (error.message || '未知错误'));
    }
};

// 申请管理相关方法
const fetchApplications = async (status = 'pending') => {
  if (!group.value) return;
  
  loadingApplications.value = true;
  applicationStatus.value = status;
  
  try {
    const response = await api.group.getGroupApplications(group.value.id, status);
    if (response.code === 0 && response.data) {
      // 获取申请人信息
      const applicationsWithUserInfo = await Promise.all(
        response.data.map(async (app) => {
          try {
            // ✅ 修复：使用正确的API方法名 getUserById
            const userRes = await api.user.getUserById(app.userId);
            return {
              ...app,
              username: userRes.data?.username || `用户${app.userId}`,
              userAvatar: userRes.data?.avatar || defaultAvatar
            };
          } catch (error) {
            console.warn(`获取用户${app.userId}信息失败:`, error);
            return {
              ...app,
              username: `用户${app.userId}`,
              userAvatar: defaultAvatar
            };
          }
        })
      );
      applications.value = applicationsWithUserInfo;
    } else {
      applications.value = [];
      ElMessage.error(response.message || '获取申请列表失败');
    }
  } catch (error) {
    console.error('获取申请列表失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '获取申请列表失败');
    applications.value = [];
  } finally {
    loadingApplications.value = false;
  }
};

const handleApproveApplication = async (application) => {
  ElMessageBox.confirm(
    `确定要通过 "${application.username}" 的加入申请吗？`,
    '确认通过',
    {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'success',
    }
  ).then(async () => {
    const loading = ElLoading.service({ text: '正在处理...' });
    try {
      const response = await api.group.approveApplication(
        group.value.id,
        application.id,
        '申请通过'
      );
      
      if (response.code === 0) {
        ElMessage.success('已通过申请');
        fetchApplications(applicationStatus.value);
        fetchGroupMembers(); // 刷新成员列表
      } else {
        ElMessage.error(response.message || '操作失败');
      }
    } catch (error) {
      console.error('通过申请失败:', error);
      ElMessage.error(error.response?.data?.message || error.message || '操作失败');
    } finally {
      loading.close();
    }
  }).catch(() => {});
};

const handleRejectApplication = async (application) => {
  ElMessageBox.prompt(
    `确定要拒绝 "${application.username}" 的加入申请吗？`,
    '拒绝申请',
    {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入拒绝原因（选填）',
      inputType: 'textarea',
      type: 'warning',
    }
  ).then(async ({ value }) => {
    const loading = ElLoading.service({ text: '正在处理...' });
    try {
      const response = await api.group.rejectApplication(
        group.value.id,
        application.id,
        value || '申请被拒绝'
      );
      
      if (response.code === 0) {
        ElMessage.success('已拒绝申请');
        fetchApplications(applicationStatus.value);
      } else {
        ElMessage.error(response.message || '操作失败');
      }
    } catch (error) {
      console.error('拒绝申请失败:', error);
      ElMessage.error(error.response?.data?.message || error.message || '操作失败');
    } finally {
      loading.close();
    }
  }).catch(() => {});
};

const getApplicationStatusType = (status) => {
  const statusMap = {
    'PENDING': 'warning',
    'APPROVED': 'success',
    'REJECTED': 'danger'
  };
  return statusMap[status] || 'info';
};

const getApplicationStatusText = (status) => {
  const statusMap = {
    'PENDING': '待审批',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  };
  return statusMap[status] || status;
};

</script>

<style scoped>
.group-detail-container {
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
  /* box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); */ /* Match el-card shadow if not using el-card directly */
}
.page-header {
  margin-bottom: 20px;
}
.page-header .el-page-header__content { /* For older Element Plus versions */
  align-items: center;
}
.page-header :deep(.el-page-header__content) { /* For newer Element Plus versions using :deep */
  align-items: center;
}
.detail-tabs {
  margin-top: 10px;
}
.group-info-desc {
  margin-top: 15px;
}
.actions-bar {
  margin-bottom: 15px;
  margin-top: 5px;
  display: flex;
  gap: 10px;
}
.top-actions {
    margin-top: 20px;
}
.bottom-actions {
    margin-top: 30px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;
    display: flex;
    justify-content: flex-start;
}
.members-table .el-avatar {
  vertical-align: middle;
}
.loading-placeholder, .empty-placeholder {
  padding: 40px;
  text-align: center;
}
.page-header-extra {
    display: flex;
    align-items: center;
}
.applications-table .user-info {
  display: flex;
  align-items: center;
}
.loading-state, .empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  color: #909399;
}
</style>
