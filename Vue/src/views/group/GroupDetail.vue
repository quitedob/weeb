<template>
  <div class="group-detail-container el-card" v-if="!loading && group">
    <el-page-header @back="goBack" class="page-header">
      <template #content>
        <span class="text-large font-600 mr-3"> {{ group.groupName || '群组详情' }} </span>
      </template>
      <template #extra v-if="isOwner">
        <div class="page-header-extra">
          <el-button type="danger" @click="confirmDisbandGroup" :icon="Delete" plain>解散群组</el-button>
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
             <el-button type="primary" @click="openEditGroupDialog" :icon="Edit">编辑群信息</el-button>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="`成员 (${members.length})`" name="members">
        <div class="actions-bar">
          <el-button v-if="isOwner || isAdmin" type="success" @click="openInviteDialog" :icon="UserPlus">邀请成员</el-button>
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
        <el-button type="primary" @click="navigateToGroupChat(group.groupId, group.groupName)" :icon="ChatDotRound">进入群聊</el-button>
        <el-button type="warning" @click="confirmLeaveGroup" v-if="!isOwner" :icon="Remove" plain>退出群组</el-button>
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
            :key="item.userId"
            :label="`${item.username} (ID: ${item.userId})`"
            :value="item.userId"
            :disabled="isAlreadyMember(item.userId)"
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
import { Edit, User as UserIcon, Delete, UserPlus, ArrowLeft, ChatDotRound, Remove} from '@element-plus/icons-vue';
import defaultAvatar from '@/assets/logo.png';

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
const isOwner = computed(() => group.value && currentUser.value && group.value.ownerId === currentUser.value.id); // Corrected: currentUser.value.id to currentUser.value.userId
const isAdmin = computed(() => {
    // Placeholder: Implement admin check logic if applicable
    // e.g., check if currentUser.id is in a list of admin IDs for this group
    // For now, only owner has admin-like privileges shown in template
    if (!group.value || !currentUser.value) return false;
    const currentMember = members.value.find(m => m.userId === currentUser.value.id); // Corrected: currentUser.value.id to currentUser.value.userId
    return currentMember && currentMember.role === 'ADMIN'; // Assuming role might be 'ADMIN'
});


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
    if (detailsRes.code === 200 && detailsRes.data) {
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
            const ownerRes = await api.user.getUserInfoById(group.value.ownerId);
            if(ownerRes.code === 200 && ownerRes.data) ownerInfo.value = ownerRes.data;
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
    // Assuming api.group.getGroupMembers returns { code, message, data: [members] }
    // And each member has { userId, username, avatar, role_in_group (or derive from ownerId) }
    const membersRes = await api.group.getGroupMembers(groupId.value);
     if (membersRes.code === 200 && membersRes.data) {
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
    if (response.code === 200) {
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
      // api.search.searchUsers returns { code, message, data: [users] }
      // Each user { userId, username, avatar }
      const response = await api.search.searchUsers(query);
      if (response.code === 200 && response.data) {
          searchableUsers.value = response.data;
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
    return members.value.some(member => member.userId === userIdToInvite);
};

const handleInviteMembers = async () => {
  if (usersToInvite.value.length === 0) {
    ElMessage.warning('请选择要邀请的用户');
    return;
  }
  isInviting.value = true;
  try {
    const payload = { groupId: group.value.id, userIds: usersToInvite.value }; // Use group.id
    const response = await api.group.inviteMembers(payload);
    if (response.code === 200) {
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
      const payload = { groupId: group.value.id, kickedUserId: memberToKick.userId }; // Use group.id, and consistent param name
      const response = await api.group.kickMember(payload); // kickMember expects {groupId, kickedUserId}
       if (response.code === 200) {
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
      if (response.code === 200) {
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
        if (response.code === 200) {
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

const navigateToGroupChat = (gId, gName) => {
    const groupChatSession = {
        // Ensure properties match what ChatWindow expects or ChatStore processes
        id: gId,
        name: gName,
        avatar: group.value?.avatarUrl || group.value?.avatar || defaultAvatar, // Use group's avatar
        type: 'GROUP',
        // lastMessage, unreadCount might not be relevant here or fetched by ChatWindow/ChatStore
    };
    chatStore.setActiveChat(groupChatSession);
    router.push('/chat'); // Navigate to the main chat view, which should pick up activeChatSession
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
</style>
