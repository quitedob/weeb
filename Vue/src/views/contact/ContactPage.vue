<template>
  <div class="contact-page-container">
    <div class="page-header">
      <h1>联系人管理</h1>
      <AppleButton variant="primary" @click="openAddContactDialog">
        <i class="icon-plus"></i> 添加联系人
      </AppleButton>
    </div>

    <SimpleTabs
      v-model="activeTab"
      :tabs="[
        { name: 'contacts', label: '我的联系人' },
        { name: 'requests', label: '好友申请' },
        { name: 'search', label: '查找用户' }
      ]"
    >
      <!-- 我的联系人 -->
      <div v-show="activeTab === 'contacts'">
        <div v-if="loadingContacts" class="loading-state">
          <div class="loading-text">正在加载联系人...</div>
        </div>
        <div v-else-if="contacts.length === 0" class="empty-state">
          <div class="empty-icon">👥</div>
          <div class="empty-text">您还没有任何联系人，快添加一个吧！</div>
          <AppleButton variant="primary" @click="openAddContactDialog">
            添加第一个联系人
          </AppleButton>
        </div>
        <div v-else class="contact-list">
          <AppleCard
            v-for="contact in contacts"
            :key="contact.contactId"
            class="contact-card"
            hover
          >
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
                  <span class="nickname">{{ contact.nickname || contact.username }}</span>
                </div>
              </div>
              <div class="card-actions">
                <AppleButton variant="primary" size="small" @click="startChat(contact)">
                  <i class="icon-message"></i> 发消息
                </AppleButton>
                <AppleButton variant="ghost" size="small" @click="deleteContact(contact)">
                  <i class="icon-trash"></i> 删除
                </AppleButton>
              </div>
            </div>
            <div v-if="contact.bio" class="contact-bio">
              {{ contact.bio }}
            </div>
          </AppleCard>
        </div>
      </div>

      <!-- 好友申请 -->
      <div v-show="activeTab === 'requests'">
        <div v-if="loadingRequests" class="loading-state">
          <div class="loading-text">正在加载好友申请...</div>
        </div>
        <div v-else-if="friendRequests.length === 0" class="empty-state">
          <div class="empty-icon">📭</div>
          <div class="empty-text">暂无好友申请</div>
        </div>
        <div v-else class="request-list">
          <AppleCard
            v-for="request in friendRequests"
            :key="request.contactId"
            class="request-card"
            hover
          >
            <div class="card-header">
              <div class="user-info">
                <div class="user-avatar">
                  <img v-if="request.avatar" :src="request.avatar" :alt="request.username" />
                  <div v-else class="avatar-placeholder">
                    {{ request.username?.charAt(0)?.toUpperCase() || 'U' }}
                  </div>
                </div>
                <div class="user-details">
                  <span class="username">{{ request.username }}</span>
                  <span class="request-time">{{ formatTime(request.createdAt) }}</span>
                </div>
              </div>
              <div class="card-actions">
                <AppleButton variant="success" size="small" @click="acceptRequest(request)">
                  <i class="icon-check"></i> 接受
                </AppleButton>
                <AppleButton variant="danger" size="small" @click="rejectRequest(request)">
                  <i class="icon-close"></i> 拒绝
                </AppleButton>
              </div>
            </div>
            <div v-if="request.message" class="request-message">
              <strong>申请消息：</strong>{{ request.message }}
            </div>
          </AppleCard>
        </div>
      </div>

      <!-- 查找用户 -->
      <div v-show="activeTab === 'search'">
        <div class="search-section">
          <AppleCard class="search-card">
            <div class="search-header">
              <h3>查找用户</h3>
            </div>
            <div class="search-form">
              <div class="form-item">
                <AppleInput
                  v-model="searchQuery"
                  placeholder="输入用户名或邮箱搜索..."
                  clearable
                  @keyup.enter="searchUsers"
                >
                  <template #suffix>
                    <AppleButton variant="ghost" size="small" @click="searchUsers" :loading="searching">
                      <i class="icon-search"></i>
                    </AppleButton>
                  </template>
                </AppleInput>
              </div>
            </div>
          </AppleCard>

          <!-- 搜索结果 -->
          <div v-if="searchResults.length > 0" class="search-results">
            <h3>搜索结果</h3>
            <AppleCard
              v-for="user in searchResults"
              :key="user.id"
              class="user-card"
              hover
            >
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
                    <span class="user-nickname">{{ user.nickname || '暂无昵称' }}</span>
                  </div>
                </div>
                <div class="card-actions">
                  <AppleButton variant="primary" size="small" @click="sendFriendRequest(user)">
                    <i class="icon-user-plus"></i> 添加好友
                  </AppleButton>
                </div>
              </div>
              <div v-if="user.bio" class="user-bio">
                {{ user.bio }}
              </div>
            </AppleCard>
          </div>

          <div v-else-if="searched && !searching" class="no-results">
            <div class="empty-icon">🔍</div>
            <div class="empty-text">未找到匹配的用户</div>
          </div>
        </div>
      </div>
    </SimpleTabs>

    <!-- 添加联系人弹窗 -->
    <AppleModal
      v-model="showAddDialog"
      title="添加联系人"
      width="500px"
      :show-footer="true"
    >
      <div class="add-contact-form">
        <div class="form-item">
          <label>用户名或邮箱：</label>
          <AppleInput
            v-model="addForm.username"
            placeholder="请输入要添加的用户名或邮箱"
            clearable
          />
        </div>
        <div class="form-item">
          <label>申请消息（可选）：</label>
          <AppleInput
            v-model="addForm.message"
            type="textarea"
            :rows="3"
            placeholder="介绍一下自己吧..."
          />
        </div>
      </div>
      <template #footer>
        <AppleButton variant="ghost" @click="showAddDialog = false">
          取消
        </AppleButton>
        <AppleButton
          variant="primary"
          @click="sendFriendRequestByUsername"
          :loading="adding"
        >
          发送申请
        </AppleButton>
      </template>
    </AppleModal>
  </div>
</template>

<script setup>
import appleConfirm from '@/utils/appleConfirm';
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showMessage } from '@/utils/message' // 导入消息工具

import contactApi from '@/api/modules/contact'
import chatApi from '@/api/modules/chat'
import AppleButton from '@/components/common/AppleButton.vue'
import AppleInput from '@/components/common/AppleInput.vue'
import AppleCard from '@/components/common/AppleCard.vue'
import AppleModal from '@/components/common/AppleModal.vue'
import SimpleTabs from '@/components/common/SimpleTabs.vue'

const router = useRouter()

// 响应式数据
const activeTab = ref('contacts')
const contacts = ref([])
const friendRequests = ref([])
const searchResults = ref([])
const loadingContacts = ref(false)
const loadingRequests = ref(false)
const searching = ref(false)
const searched = ref(false)
const searchQuery = ref('')

// 添加联系人弹窗
const showAddDialog = ref(false)
const adding = ref(false)
const addForm = ref({
  username: '',
  message: ''
})

// 方法
const loadContacts = async () => {
  // ✅ 防止重复加载
  if (loadingContacts.value) {
    return
  }
  
  loadingContacts.value = true
  try {
    const response = await contactApi.getContacts('ACCEPTED')
    if (response && response.code === 0) {
      // ✅ 后端已经处理去重，但保留前端去重作为防御性编程
      const uniqueContacts = new Map()
      ;(response.data || []).forEach(contact => {
        // 使用用户ID去重，因为同一个用户不应该出现多次
        const userId = contact.id
        if (!uniqueContacts.has(userId)) {
          uniqueContacts.set(userId, {
            ...contact,
            contactTime: contact.contactTime || contact.createdAt
          })
        }
      })
      contacts.value = Array.from(uniqueContacts.values())
      console.log('✅ 联系人列表加载成功:', contacts.value.length, '个联系人')
    } else {
      showMessage.error(response?.message || '获取联系人列表失败')
    }
  } catch (error) {
    console.error('加载联系人失败:', error)
    showMessage.error('加载联系人失败')
  } finally {
    loadingContacts.value = false
  }
}

const loadFriendRequests = async () => {
  loadingRequests.value = true
  try {
    const response = await contactApi.getFriendRequests()
    if (response && response.code === 0) {
      friendRequests.value = response.data || []
    } else {
      showMessage.error(response?.message || '获取好友申请失败')
    }
  } catch (error) {
    console.error('加载好友申请失败:', error)
    showMessage.error('加载好友申请失败')
  } finally {
    loadingRequests.value = false
  }
}

const searchUsers = async () => {
  if (!searchQuery.value.trim()) {
    showMessage.warning('请输入搜索关键词') // 使用 showMessage 显示警告
    return
  }

  searching.value = true
  try {
    const response = await contactApi.searchUsers(searchQuery.value.trim())
    if (response && response.code === 0) {
      // Handle both list format and direct array format
      const userList = response.data.list || response.data;
      searchResults.value = userList || []
    } else {
      showMessage.error('搜索失败') // 使用 showMessage 显示错误
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
    showMessage.error('搜索用户失败') // 使用 showMessage 显示错误
  } finally {
    searching.value = false
    searched.value = true
  }
}

const openAddContactDialog = () => {
  showAddDialog.value = true
  addForm.value = {
    username: '',
    message: ''
  }
}

const sendFriendRequest = async (user) => {
  // 获取当前用户ID
  const currentUserId = localStorage.getItem('userId') || sessionStorage.getItem('userId')
  
  // 防止添加自己
  if (currentUserId && user.id === parseInt(currentUserId)) {
    showMessage.warning('不能添加自己为好友')
    return
  }
  
  try {
    const response = await contactApi.sendRequest(user.id, '您好，我想添加您为好友')
    if (response && response.code === 0) {
      showMessage.success('好友申请已发送')
      // 从搜索结果中移除该用户
      searchResults.value = searchResults.value.filter(u => u.id !== user.id)
    } else {
      showMessage.error(response?.message || '发送申请失败')
    }
  } catch (error) {
    console.error('发送好友申请失败:', error)
    showMessage.error('发送好友申请失败')
  }
}

const sendFriendRequestByUsername = async () => {
  if (!addForm.value.username.trim()) {
    showMessage.warning('请输入用户名或邮箱')
    return
  }
  
  // 获取当前用户名
  const currentUsername = localStorage.getItem('username') || sessionStorage.getItem('username')
  
  // 防止添加自己
  if (currentUsername && addForm.value.username.trim().toLowerCase() === currentUsername.toLowerCase()) {
    showMessage.warning('不能添加自己为好友')
    return
  }

  adding.value = true
  try {
    const response = await contactApi.sendRequestByUsername(
      addForm.value.username.trim(),
      addForm.value.message || '您好，我想添加您为好友'
    )
    if (response && response.code === 0) {
      showMessage.success('好友申请已发送')
      showAddDialog.value = false
    } else {
      showMessage.error(response?.message || '发送申请失败')
    }
  } catch (error) {
    console.error('发送好友申请失败:', error)
    showMessage.error('发送好友申请失败')
  } finally {
    adding.value = false
  }
}

const acceptRequest = async (request) => {
  try {
    // 使用 contactId 而不是 id（id 是申请人用户ID，contactId 是联系人记录ID）
    const response = await contactApi.acceptRequest(request.contactId)
    if (response && response.code === 0) {
      showMessage.success('已接受好友申请')
      // 从申请列表中移除
      friendRequests.value = friendRequests.value.filter(r => r.contactId !== request.contactId)
      // 重新加载联系人列表
      await loadContacts()
    } else {
      showMessage.error(response?.message || '接受申请失败')
    }
  } catch (error) {
    console.error('接受好友申请失败:', error)
    showMessage.error('接受好友申请失败')
  }
}

const rejectRequest = async (request) => {
  try {
    // 使用 contactId 而不是 id（id 是申请人用户ID，contactId 是联系人记录ID）
    const response = await contactApi.rejectRequest(request.contactId)
    if (response && response.code === 0) {
      showMessage.success('已拒绝好友申请')
      // 从申请列表中移除
      friendRequests.value = friendRequests.value.filter(r => r.contactId !== request.contactId)
    } else {
      showMessage.error(response?.message || '拒绝申请失败')
    }
  } catch (error) {
    console.error('拒绝好友申请失败:', error)
    showMessage.error('拒绝好友申请失败')
  }
}

const deleteContact = async (contact) => {
  if (!await appleConfirm.confirm('删除联系人', `确定要删除联系人 ${contact.username} 吗？`)) {
    return
  }

  try {
    // 使用 contactId 而不是 id（id 是用户ID，contactId 是联系人记录ID）
    const response = await contactApi.deleteContact(contact.contactId)
    if (response && response.code === 0) {
      showMessage.success('已删除联系人')
      // 从联系人列表中移除
      contacts.value = contacts.value.filter(c => c.contactId !== contact.contactId)
    } else {
      showMessage.error(response?.message || '删除联系人失败')
    }
  } catch (error) {
    console.error('删除联系人失败:', error)
    showMessage.error('删除联系人失败')
  }
}

const startChat = async (contact) => {
  try {
    // 先创建或获取聊天会话
    const response = await chatApi.createChat({ targetId: contact.id })
    if (response && response.code === 0) {
      const chatData = response.data
      // 使用chatId跳转到聊天页面
      router.push(`/chat/private/${chatData.id || chatData.chatId}`)
    } else {
      showMessage.error(response?.message || '创建聊天失败')
    }
  } catch (error) {
    console.error('创建聊天失败:', error)
    showMessage.error('创建聊天失败')
  }
}

const formatTime = (timeString) => {
  if (!timeString) return ''
  const date = new Date(timeString)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  } else {
    return date.toLocaleDateString()
  }
}

const handleContactNotification = ({ detail }) => {
  if (detail.type === 'FRIEND_REQUEST') {
    loadFriendRequests()
    showMessage.info('收到新的好友申请')
  } else {
    loadContacts()
    loadFriendRequests()
  }
}

onMounted(() => {
  loadContacts()
  loadFriendRequests()
  window.addEventListener('contact-notification', handleContactNotification)
})

import { onUnmounted } from 'vue'
onUnmounted(() => window.removeEventListener('contact-notification', handleContactNotification))
</script>

<style scoped>
.contact-page-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
  background: #f5f5f5;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  color: #1d1d1d;
}

.loading-state {
  text-align: center;
  padding: 40px;
  color: #868686;
}

.loading-text {
  font-size: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 16px;
  color: #868686;
  margin-bottom: 20px;
}

.contact-list,
.request-list,
.search-results {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.contact-card,
.request-card,
.user-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.05);
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
  flex: 1;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  background: #f0f0f0;
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
  font-size: 18px;
  font-weight: 600;
  color: #868686;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.username {
  font-weight: 600;
  color: #1d1d1d;
  font-size: 16px;
}

.nickname,
.user-nickname {
  color: #868686;
  font-size: 14px;
}

.request-time {
  font-size: 12px;
  color: #989898;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.contact-bio,
.user-bio,
.request-message {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  color: #515151;
  line-height: 1.5;
}

.request-message {
  font-size: 14px;
}

.search-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-card {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.search-header h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1d;
}

.search-form {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.form-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1d;
}

.search-results h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1d;
}

.no-results {
  text-align: center;
  padding: 40px;
}

.add-contact-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .contact-page-container {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .card-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .card-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .search-form {
    flex-direction: column;
    gap: 16px;
  }
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.contact-card,
.request-card,
.user-card {
  animation: fadeIn 0.3s ease;
}

/* 主题适配 */
.contact-page-container {
  background: var(--apple-bg-secondary);
  color: var(--apple-text-primary);
}

.page-header h1,
.search-header h3,
.search-results h3,
.form-item label {
  color: var(--apple-text-primary);
}

.contact-card,
.request-card,
.user-card,
.search-card {
  background: var(--apple-bg-primary);
  border: 1px solid var(--apple-border-secondary);
  color: var(--apple-text-primary);
}

.username {
  color: var(--apple-text-primary);
}

.nickname,
.user-nickname,
.request-time {
  color: var(--apple-text-tertiary);
}

.loading-text,
.empty-text {
  color: var(--apple-text-tertiary);
}

.contact-bio,
.user-bio,
.request-message {
  color: var(--apple-text-secondary);
  border-top: 1px solid var(--apple-border-secondary);
}
</style>
