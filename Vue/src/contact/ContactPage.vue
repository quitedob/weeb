<template>
  <div class="contact-page-container">
    <div class="page-header">
      <h1>联系人管理</h1>
      <el-button type="primary" @click="openAddContactDialog">
        <el-icon><Plus /></el-icon> 添加联系人
      </el-button>
    </div>

    <el-tabs v-model="activeTab" class="contact-tabs">
      <el-tab-pane label="我的联系人" name="contacts">
        <div v-if="loadingContacts" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="contacts.length === 0" class="empty-state">
          <el-empty description="您还没有任何联系人，快添加一个吧！" />
        </div>
        <div v-else class="contact-list">
          <el-card v-for="contact in contacts" :key="contact.id" shadow="hover" class="contact-card">
            <template #header>
              <div class="card-header">
                <div class="user-info">
                  <div class="user-avatar">
                    <img v-if="contact.avatar" :src="contact.avatar" :alt="contact.username" />
                    <div v-else class="avatar-placeholder">
                      {{ contact.username?.charAt(0)?.toUpperCase() || 'U' }}
                    </div>
                  </div>
                  <div class="user-details">
                    <span class="username">{{ contact.username }}</span>
                    <span class="nickname">{{ contact.nickname || '暂无昵称' }}</span>
                  </div>
                </div>
                <el-tag :type="getContactStatusType(contact.status)">
                  {{ getContactStatusText(contact.status) }}
                </el-tag>
              </div>
            </template>
            <div class="contact-info">
              <p>用户ID: {{ contact.id }}</p>
              <p>邮箱: {{ contact.userEmail || '未设置' }}</p>
              <p>添加时间: {{ formatDate(contact.createdAt) }}</p>
            </div>
            <template #footer>
              <div class="card-footer">
                <el-button type="primary" text @click="startChat(contact)">发送消息</el-button>
                <el-button type="info" text @click="viewUserProfile(contact.id)">查看资料</el-button>
                <el-button type="danger" text @click="confirmRemoveContact(contact.id)">删除联系人</el-button>
              </div>
            </template>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="好友申请" name="applications">
        <div v-if="loadingApplications" class="loading-state">
          <el-skeleton :rows="3" animated />
        </div>
        <div v-else-if="applications.length === 0" class="empty-state">
          <el-empty description="暂无好友申请" />
        </div>
        <div v-else class="application-list">
          <el-card v-for="application in applications" :key="application.id" shadow="hover" class="application-card">
            <template #header>
              <div class="card-header">
                <div class="user-info">
                  <div class="user-avatar">
                    <img v-if="application.avatar" :src="application.avatar" :alt="application.username" />
                    <div v-else class="avatar-placeholder">
                      {{ application.username?.charAt(0)?.toUpperCase() || 'U' }}
                    </div>
                  </div>
                  <div class="user-details">
                    <span class="username">{{ application.username }}</span>
                    <span class="nickname">{{ application.nickname || '暂无昵称' }}</span>
                  </div>
                </div>
              </div>
            </template>
            <div class="application-info">
              <p><strong>申请理由:</strong> {{ application.remarks || '无' }}</p>
              <p>申请时间: {{ formatDate(application.createdAt) }}</p>
            </div>
            <template #footer>
              <div class="card-footer">
                <el-button type="success" @click="acceptApplication(application.id)">接受</el-button>
                <el-button type="danger" @click="declineApplication(application.id)">拒绝</el-button>
              </div>
            </template>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加联系人对话框 -->
    <el-dialog v-model="addContactDialogVisible" title="添加联系人" width="500px" @close="resetAddContactForm">
      <el-form ref="addContactFormRef" :model="addContactForm" :rules="addContactRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addContactForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="申请理由" prop="remarks">
          <el-input v-model="addContactForm.remarks" type="textarea" placeholder="（选填）请输入申请理由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addContactDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddContact" :loading="addingContact">
          发送申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/authStore';
import { useChatStore } from '@/stores/chatStore';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import contactApi from '@/api/modules/contact';

const router = useRouter();
const authStore = useAuthStore();
const chatStore = useChatStore();

const activeTab = ref('contacts');
const contacts = ref([]);
const applications = ref([]);
const loadingContacts = ref(false);
const loadingApplications = ref(false);

const addContactDialogVisible = ref(false);
const addingContact = ref(false);
const addContactFormRef = ref(null);
const addContactForm = reactive({
  username: '',
  remarks: ''
});

const addContactRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ]
};

// 获取联系人列表
const fetchContacts = async () => {
  loadingContacts.value = true;
  try {
    const response = await contactApi.getContacts('ACCEPTED');
    if (response.code === 200 && response.data) {
      contacts.value = response.data;
    } else {
      contacts.value = [];
      ElMessage.error(response.message || '获取联系人失败');
    }
  } catch (error) {
    console.error('获取联系人失败:', error);
    ElMessage.error('获取联系人失败');
    contacts.value = [];
  } finally {
    loadingContacts.value = false;
  }
};

// 获取好友申请列表
const fetchApplications = async () => {
  loadingApplications.value = true;
  try {
    const response = await contactApi.getPendingApplications();
    if (response.code === 200 && response.data) {
      applications.value = response.data;
    } else {
      applications.value = [];
      ElMessage.error(response.message || '获取好友申请失败');
    }
  } catch (error) {
    console.error('获取好友申请失败:', error);
    ElMessage.error('获取好友申请失败');
    applications.value = [];
  } finally {
    loadingApplications.value = false;
  }
};

// 打开添加联系人对话框
const openAddContactDialog = () => {
  addContactDialogVisible.value = true;
};

// 重置添加联系人表单
const resetAddContactForm = () => {
  if (addContactFormRef.value) {
    addContactFormRef.value.resetFields();
  }
  addContactForm.username = '';
  addContactForm.remarks = '';
};

// 处理添加联系人
const handleAddContact = async () => {
  if (!addContactFormRef.value) return;

  await addContactFormRef.value.validate(async (valid) => {
    if (valid) {
      addingContact.value = true;
      try {
        const response = await contactApi.applyContact({
          username: addContactForm.username,
          remarks: addContactForm.remarks
        });
        
        if (response.code === 200) {
          ElMessage.success('好友申请发送成功');
          addContactDialogVisible.value = false;
          resetAddContactForm();
        } else {
          ElMessage.error(response.message || '发送申请失败');
        }
      } catch (error) {
        console.error('发送好友申请失败:', error);
        ElMessage.error(error.response?.data?.message || error.message || '发送申请失败');
      } finally {
        addingContact.value = false;
      }
    }
  });
};

// 接受好友申请
const acceptApplication = async (contactId) => {
  const loading = ElLoading.service({ text: '正在处理...' });
  try {
    const response = await contactApi.acceptContact(contactId);
    if (response.code === 200) {
      ElMessage.success('已接受好友申请');
      fetchApplications();
      fetchContacts();
    } else {
      ElMessage.error(response.message || '操作失败');
    }
  } catch (error) {
    console.error('接受好友申请失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '操作失败');
  } finally {
    loading.close();
  }
};

// 拒绝好友申请
const declineApplication = async (contactId) => {
  ElMessageBox.confirm('确定要拒绝该好友申请吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const loading = ElLoading.service({ text: '正在处理...' });
    try {
      const response = await contactApi.declineContact(contactId);
      if (response.code === 200) {
        ElMessage.success('已拒绝好友申请');
        fetchApplications();
      } else {
        ElMessage.error(response.message || '操作失败');
      }
    } catch (error) {
      console.error('拒绝好友申请失败:', error);
      ElMessage.error(error.response?.data?.message || error.message || '操作失败');
    } finally {
      loading.close();
    }
  }).catch(() => {
    // 用户取消
  });
};

// 确认删除联系人
const confirmRemoveContact = (contactId) => {
  ElMessageBox.confirm('确定要删除该联系人吗？删除后将无法直接发送消息。', '提示', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const loading = ElLoading.service({ text: '正在删除...' });
    try {
      const response = await contactApi.blockContact(contactId);
      if (response.code === 200) {
        ElMessage.success('联系人已删除');
        fetchContacts();
      } else {
        ElMessage.error(response.message || '删除失败');
      }
    } catch (error) {
      console.error('删除联系人失败:', error);
      ElMessage.error(error.response?.data?.message || error.message || '删除失败');
    } finally {
      loading.close();
    }
  }).catch(() => {
    // 用户取消
  });
};

// 开始聊天
const startChat = (contact) => {
  const chatSession = {
    id: contact.id,
    name: contact.username,
    avatar: contact.avatar,
    type: 'PRIVATE',
    unreadCount: 0,
    lastMessage: null,
    lastMessageTime: new Date().toISOString(),
  };
  chatStore.setActiveChat(chatSession);
  router.push('/chat');
};

// 查看用户资料
const viewUserProfile = (userId) => {
  router.push(`/user/${userId}`);
};

// 获取联系人状态类型
const getContactStatusType = (status) => {
  switch (status) {
    case 'ACCEPTED': return 'success';
    case 'PENDING': return 'warning';
    case 'BLOCKED': return 'danger';
    default: return 'info';
  }
};

// 获取联系人状态文本
const getContactStatusText = (status) => {
  switch (status) {
    case 'ACCEPTED': return '已添加';
    case 'PENDING': return '待确认';
    case 'BLOCKED': return '已屏蔽';
    default: return '未知';
  }
};

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  return new Date(dateString).toLocaleString();
};

// 组件挂载时加载数据
onMounted(() => {
  if (authStore.currentUser) {
    fetchContacts();
    fetchApplications();
  } else {
    ElMessage.error('用户未登录，无法加载联系人数据');
    router.replace('/login');
  }
});
</script>

<style scoped>
.contact-page-container {
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

.contact-tabs {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.el-tabs__content {
  overflow-y: auto;
  height: calc(100% - 55px);
}

.contact-list, .application-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  padding-top: 10px;
}

.contact-card, .application-card {
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

.contact-card:hover, .application-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 15px rgba(0,0,0,0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar img {
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

.user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.username {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}

.nickname {
  font-size: 14px;
  color: #909399;
}

.contact-info, .application-info {
  margin: 15px 0;
}

.contact-info p, .application-info p {
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
</style>
