<template>
  <!-- 主聊天容器 -->
  <div class="chat-container">
    <!-- 文件传输弹窗（占位示例） -->
    <div v-if="fileInfo.fileVisible">
      <!-- 遮罩层 -->
      <div class="mask"></div>
      <!-- 文件传输弹窗 -->
      <div class="file-transfer-modal">
        <h3>文件传输(占位)</h3>
        <p>目标：{{ fileInfo.fileTargetInfo }}</p>
        <p>文件：{{ fileInfo.fileName }}</p>
        <!-- 点击关闭按钮隐藏弹窗 -->
        <button @click="fileInfo.fileVisible = false">关闭</button>
      </div>
    </div>

  
    <!-- 用户信息修改弹窗（占位示例） -->
    <div v-if="modifyUserInfoIsOpen">
      <div class="mask"></div>
      <div class="modify-user-modal">
        <h3>用户信息修改(占位)</h3>
        <p>
          用户名:
          <!-- 双向绑定用户名称 -->
          <input v-model="userInfoStore.userName" />
        </p>
        <button @click="modifyUserInfoIsOpen = false">关闭</button>
      </div>
    </div>

    <!-- 表情弹窗 -->
    <div
        v-if="isEmojiVisible"
        class="emoji-popup"
        :style="{
        top: emojiPosition.y + 'px',
        left: emojiPosition.x + 'px',
        width: inputAreaWidth + 'px'
      }"
    >
      <h4 class="emoji-title">表情弹窗</h4>
      <!-- 表情搜索框容器 -->
      <div class="emoji-search-container">
        <input
            v-model="emojiSearchValue"
            type="text"
            placeholder="搜索表情"
            class="emoji-search-input"
        />
      </div>
      <!-- 表情网格展示区域 -->
      <div class="emoji-grid">
        <div
            v-for="(item, idx) in paginatedEmojis"
            :key="idx"
            :title="item.name"
            class="emoji-item"
            @click="insertEmoji(item.icon)"
        >
          {{ item.icon }}
        </div>
      </div>
      <!-- 表情分页控制 -->
      <div class="emoji-pagination">
        <button
            class="emoji-pagination-button"
            @click="prevPage"
            :disabled="currentEmojiPage === 1"
        >
          上一页
        </button>
        <span class="emoji-pagination-info">第 {{ currentEmojiPage }} / {{ totalPages }} 页</span>
        <button
            class="emoji-pagination-button"
            @click="nextPage"
            :disabled="currentEmojiPage === totalPages"
        >
          下一页
        </button>
      </div>
      <!-- 表情包切换按钮 -->
      <div class="emoji-package-container">
        <button
            v-for="(pkg, pkgIndex) in filteredEmojisList"
            :key="pkgIndex"
            @click="switchPackage(pkgIndex)"
            :class="['emoji-package-button', { active: currentPackageIndex === pkgIndex }]"
        >
          {{ pkg.name }}
        </button>
      </div>
      <!-- 关闭表情弹窗按钮 -->
      <button class="emoji-close-button" @click="closeEmojiPopup">关闭</button>
    </div>

    <!-- 聊天背景及主要聊天区域 -->
    <div class="chat-bg">
      <div class="chat-box">
        <!-- 左侧菜单（聊天列表） -->
        <div class="box-left" :class="{ 'show-left': showLeft }">
          <div class="chat-list-title">
            <div>消息列表</div>
            <div class="close-btn" @click="showLeft = false">×</div>
          </div>
          <!-- 群聊项 -->
          <div
              class="chat-list-item group-chat"
              @click="() => { targetId = '1'; closeMask(); }"
          >
            <div class="chat-avatar"></div>
            <div class="chat-item-content">
              <div class="chat-content-name">{{ groupChat.targetInfo?.name }}</div>
              <div class="chat-content-msg">{{ groupChat.lastMessage }}</div>
            </div>
          </div>
          <!-- 私聊标题 -->
          <div v-if="privateChatList.length > 0" class="private-chat-title">
            私聊
          </div>
          <!-- 私聊列表 -->
          <div class="chat-list-content">
            <div
                v-for="item in privateChatList"
                :key="item.id"
                :class="['chat-list-item', targetId === item.targetId ? 'active-chat' : '']"
                @click="() => { targetId = item.targetId; currentSelectTarget = item; closeMask(); }"
            >
              <div class="chat-avatar-small"></div>
              <div class="chat-item-content">
                <div class="chat-content-name">{{ item.targetInfo.name }}</div>
                <div class="chat-content-msg">{{ item.lastMessage }}</div>
              </div>
              <!-- 移除私聊项按钮 -->
              <button
                  v-if="targetId === item.targetId"
                  class="delete-chat-button"
                  @click="onDeleteChatList(item.id)"
              >
                移除
              </button>
            </div>
          </div>
          <!-- 广告示例 -->
          <div class="ad-container">
            <img
                src="/ad.png"
                alt="广告"
                class="ad-image"
                @click="handlerCardClick({ key: 'ad' })"
            />
          </div>
        </div>

        <!-- 移动端遮罩，点击关闭抽屉 -->
        <div class="mask" v-if="showLeft || showRight" @click="closeMask"></div>

        <!-- 中间部分（聊天消息展示及输入区域） -->
        <div class="box-middle">
          <div class="middle-top">
            <!-- 左侧菜单按钮 -->
            <div class="menu-btn" @click="showLeft = true">≡</div>
            <!-- 显示当前聊天对象名称（群聊或私聊） -->
            <template v-if="targetId === '1'">
              {{ groupChat.targetInfo?.name }}
            </template>
            <template v-else>
              {{ currentSelectTarget?.targetInfo?.name }}
            </template>
            <!-- 右侧菜单按钮 -->
            <div class="menu-btn" @click="showRight = true">⚙</div>
          </div>
          <!-- 聊天消息展示区及输入区 -->
          <div class="middle-content">
            <!-- 消息展示区 -->
            <div class="chat-show-area" ref="chatShowAreaRef">
              <div
                  v-for="msg in msgRecord"
                  :key="msg.id || msg.tempId"
                  class="msg-item"
                  :style="{
                  justifyContent: msg.fromId === userInfoStore.userId ? 'flex-end' : 'flex-start'
                }"
              >
                <div class="chat-message-container">
                  <!-- 如果消息已撤回则显示提示，否则显示消息内容 -->
                  <div class="bubble" :class="{ 'sent-message': msg.fromId === userInfoStore.userId }">
                    <div v-if="msg.isRecalled === 1">消息已撤回</div>
                    <div v-else-if="msg.messageType === 2 && msg.fileData" class="file-message">
                      <div class="file-message-content" @click="downloadFile(msg.fileData)">
                        <div class="file-icon">📄</div>
                        <div class="file-details">
                          <div class="file-name">{{ msg.fileData.fileName }}</div>
                          <div class="file-size">{{ formatFileSize(msg.fileData.fileSize) }}</div>
                        </div>
                        <div class="download-hint">点击下载</div>
                      </div>
                    </div>
                    <div v-else>{{ msg.msgContent }}</div>
                  </div>
                  <!-- 消息状态和操作按钮容器 -->
                  <div class="message-actions">
                    <!-- 消息状态指示器 -->
                    <div v-if="msg.fromId === userInfoStore.userId" class="message-status">
                      <span v-if="msg.status === 'sending'" class="status-sending">发送中...</span>
                      <span v-else-if="msg.status === 'sent'" class="status-sent">已发送</span>
                      <span v-else-if="msg.status === 'delivered'" class="status-delivered">已送达</span>
                      <span v-else-if="msg.status === 'read'" class="status-read">已读</span>
                    </div>
                    <!-- 时间戳 -->
                    <div class="message-time">
                      {{ formatMessageTime(msg.timestamp) }}
                    </div>
                    <!-- 撤回按钮：仅对当前用户自己发送的且消息未撤回时显示 -->
                    <button v-if="msg.fromId === userInfoStore.userId && msg.isRecalled !== 1" class="recall-btn" @click="handleRecallMessage(msg.id || msg.tempId)">
                      撤回
                    </button>
                  </div>
                </div>
              </div>
              <!-- 正在发送提示 -->
              <div v-if="isSendLoading" class="sending-indicator">
                <strong>发送中...</strong>
              </div>

              <!-- 打字指示器 -->
              <div v-if="chatStore.isTypingInCurrentChat" class="typing-indicator">
                <div class="typing-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <span class="typing-text">{{ getTypingUsersText() }}</span>
              </div>

              <!-- 新消息计数，点击滚动到底部 -->
              <div v-if="currentNewMsgCount > 0" class="new-msg-count" @click="scrollToBottom">
                ▼ {{ currentNewMsgCount }} 条新消息
              </div>
            </div>
            <!-- 聊天输入区 -->
            <div
              class="chat-input-area"
              @dragover.prevent="handleDragOver"
              @dragleave.prevent="handleDragLeave"
              @drop.prevent="handleFileDrop"
              :class="{ 'drag-over': isDragOver }"
            >
              <div class="chat-input-container" ref="inputAreaRef">
                <!-- 引用消息显示区域 -->
                <div v-if="msgStore.referenceMsg" class="reference-msg">
                  <div class="reference-msg-content">
                    {{ msgStore.referenceMsg.fromId }}: {{ msgStore.referenceMsg.msgContent }}
                  </div>
                  <button @click="msgStore.referenceMsg = null">X</button>
                </div>
                <!-- 表情按钮 -->
                <div class="emoji-button" @click="handlerSetEmojiBoxPosition">
                  😊
                </div>
                <!-- 文件上传按钮 -->
                <div class="file-button" @click="triggerFileUpload" title="发送文件">
                  📎
                </div>
                <!-- 隐藏的文件输入框 -->
                <input
                    ref="fileInputRef"
                    type="file"
                    style="display: none"
                    @change="handleFileSelect"
                    :disabled="fileUploadState.isUploading"
                />
                <!-- 消息输入框 -->
                <div class="chat-msg-input">
                  <input
                      v-model="msgContent"
                      type="text"
                      placeholder="请输入消息"
                      class="chat-text-input"
                      @keyup.enter="handlerSubmitMsg"
                      @input="handleTypingInput"
                  />
                </div>
              </div>
              <!-- 文件预览区域 -->
              <div v-if="fileUploadState.selectedFile" class="file-preview-container">
                <div class="file-preview">
                  <div class="file-info">
                    <span class="file-name">{{ fileUploadState.selectedFile.name }}</span>
                    <span class="file-size">({{ formatFileSize(fileUploadState.selectedFile.size) }})</span>
                  </div>
                  <div class="file-actions">
                    <button @click="clearFileSelection" class="remove-file-btn" title="移除文件">
                      ❌
                    </button>
                  </div>
                </div>
                <!-- 上传进度条 -->
                <div v-if="fileUploadState.isUploading" class="upload-progress">
                  <div class="progress-bar">
                    <div
                      class="progress-fill"
                      :style="{ width: fileUploadState.uploadProgress + '%' }"
                    ></div>
                  </div>
                  <span class="progress-text">{{ fileUploadState.uploadProgress }}%</span>
                </div>
              </div>
              <!-- 发送按钮：当输入为空时禁用，并显示灰色 -->
              <button
                  class="publish-button"
                  :disabled="(!msgContent.trim() && !fileUploadState.selectedFile) || fileUploadState.isUploading"
                  @click="handlerSubmitMsg"
              >
                {{ fileUploadState.isUploading ? '发送中...' : '发送' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 右侧菜单 -->
        <div class="box-right" :class="{ 'show-right': showRight }">
          <div class="right-top">
            <div class="user-info">
              <!-- 点击头像可打开用户信息修改弹窗 -->
              <div class="avatar2" @click="modifyUserInfoIsOpen = true"></div>
              <div class="user-name">{{ userInfoStore.userName }}</div>
            </div>
            <div class="right-btn-group">
              <button @click="toggleDark">切换主题</button>
              <button @click="handlerLogout">退出</button>
            </div>
          </div>
          <div class="right-content">
            <div class="user-list-header">
              <!-- 在线人数使用通过接口获取的在线用户数据 -->
              <div class="online-count">在线人数 ({{ onlineCount }})</div>
              <!-- 用户搜索输入框 -->
              <input
                  v-model="userSearchValue"
                  type="text"
                  placeholder="搜索用户"
                  class="user-search-input"
              />
            </div>
            <!-- 在线用户列表 -->
            <div class="online-list">
              <div
                  v-for="(item, index) in userListFiltered"
                  :key="item.id"
                  class="online-list-item"
                  :class="{ odd: index % 2 === 0 }"
              >
                <div class="online-item-content">
                  <div class="avatar1"></div>
                  <!-- 注意：后端返回的用户字段为 username -->
                  <div class="online-username">{{ item.username }}</div>
                </div>
                <div class="online-item-operation">
                  <button
                      v-if="item.id !== userInfoStore.userId"
                      @click="() => { onCreatePrivateChat(item.id, item.username); closeMask(); }"
                  >
                    私聊
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- 右侧菜单结束 -->
      </div>
    </div>
  </div>
</template>

<script setup>
/* ---------------------- 导入 Vue 响应式 API 以及其他依赖 ---------------------- */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
// 导入 API 模块
import { getUserList, getUserMap, getOnlineUsers } from '../api/modules/user'
import { sendMessage, getChatRecord, recallMessage } from '../api/modules/message'
import fileManagementApi from '../api/modules/fileManagement'
// 导入工具函数
import { generateUUID } from '@/utils/uuid'
// 导入表情包数据（请确保路径正确）
import emojis from '@constant/emoji/emoji.js'
// 导入 Vue Router 用于页面跳转
import { useRouter } from 'vue-router'
// 导入 ChatStore 用于WebSocket连接管理
import { useChatStore } from '@/stores/chatStore'
// 导入 AuthStore 用于获取用户信息
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const chatStore = useChatStore()
const authStore = useAuthStore()

/* ---------------------- 左侧/右侧抽屉控制 ---------------------- */
// 左侧菜单显示状态
const showLeft = ref(false)
// 右侧菜单显示状态
const showRight = ref(false)

/* ---------------------- 用户信息修改弹窗 ---------------------- */
const modifyUserInfoIsOpen = ref(false)

/* ---------------------- 表情弹窗逻辑 ---------------------- */
// 控制表情弹窗显示状态
const isEmojiVisible = ref(false)
// 表情弹窗的显示位置（x, y 坐标）
const emojiPosition = ref({ x: 0, y: 0 })
// 获取输入区域的宽度（用于设置表情弹窗宽度）
const inputAreaRef = ref(null)
const inputAreaWidth = ref(300)

// 表情包数据（从外部文件引入）
const emojisList = emojis
// 当前选中的表情包索引
const currentPackageIndex = ref(0)
// 每页显示的表情数量
const pageSize = 30
// 当前表情分页页码
const currentEmojiPage = ref(1)
// 表情搜索关键词
const emojiSearchValue = ref('')

// 根据搜索关键词过滤表情包数据
const filteredEmojisList = computed(() => {
  const searchVal = emojiSearchValue.value.trim().toLowerCase()
  if (!searchVal) return emojisList
  return emojisList.map(pkg => {
    const filteredList = pkg.list.filter(item => {
      return (
          item.name.toLowerCase().includes(searchVal) ||
          (item.icon || '').includes(searchVal)
      )
    })
    return { ...pkg, list: filteredList }
  })
})

// 当前选中表情包
const currentPackage = computed(() => {
  if (
      currentPackageIndex.value < 0 ||
      currentPackageIndex.value >= filteredEmojisList.value.length
  ) {
    return { name: '', list: [] }
  }
  return filteredEmojisList.value[currentPackageIndex.value]
})
// 当前表情总数
const totalEmojis = computed(() => currentPackage.value.list.length)
// 计算总页数
const totalPages = computed(() => Math.ceil(totalEmojis.value / pageSize))
// 获取当前页显示的表情列表
const paginatedEmojis = computed(() => {
  const startIndex = (currentEmojiPage.value - 1) * pageSize
  return currentPackage.value.list.slice(startIndex, startIndex + pageSize)
})

// 切换表情包，同时重置分页
function switchPackage(index) {
  currentPackageIndex.value = index
  currentEmojiPage.value = 1
}

// 分页：上一页
function prevPage() {
  if (currentEmojiPage.value > 1) currentEmojiPage.value--
}
// 分页：下一页
function nextPage() {
  if (currentEmojiPage.value < totalPages.value) currentEmojiPage.value++
}

// 插入表情到消息输入框
function insertEmoji(emojiIcon) {
  msgContent.value += emojiIcon
  isEmojiVisible.value = false
}

// 关闭表情弹窗并清空搜索框
function closeEmojiPopup() {
  isEmojiVisible.value = false
  emojiSearchValue.value = ''
}

// 设置表情弹窗显示的位置及宽度，并切换显示状态
function handlerSetEmojiBoxPosition(e) {
  if (!inputAreaRef.value) return
  // 获取输入区域位置及宽度
  const rect = inputAreaRef.value.getBoundingClientRect()
  const popupHeight = 400 // 弹窗高度预设值
  const popupWidth = 320 // 弹窗宽度预设值
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight

  // 计算合适的x位置，确保不超出视窗边界
  let x = rect.left
  if (x + popupWidth > viewportWidth) {
    x = viewportWidth - popupWidth - 10 // 留10px边距
  }
  if (x < 10) {
    x = 10 // 留10px边距
  }

  // 计算合适的y位置，确保不超出视窗边界
  let y = rect.top - popupHeight
  if (y < 10) {
    // 如果上方空间不够，显示在输入框下方
    y = rect.bottom + 10
  }

  emojiPosition.value.x = x
  emojiPosition.value.y = y
  inputAreaWidth.value = Math.min(rect.width, popupWidth)
  // 切换表情弹窗显示状态
  isEmojiVisible.value = !isEmojiVisible.value
}

/* ---------------------- 聊天逻辑 ---------------------- */
// 文件上传相关状态
const fileUploadState = ref({
  isUploading: false,
  uploadProgress: 0,
  selectedFile: null,
  filePreview: null
})
// 拖拽上传状态
const isDragOver = ref(false)
// 消息输入框内容
const msgContent = ref('')
// 消息发送中状态
const isSendLoading = ref(false)
// 新消息计数（示例）
const currentNewMsgCount = ref(0)

// 群聊示例数据
const groupChat = ref({
  targetInfo: { name: '群聊示例' },
  lastMessage: '这里是最后一条群聊消息'
})
// 私聊列表示例数据（初始为空，点击私聊后创建）
const privateChatList = ref([])
// 当前聊天对象 ID，'1' 表示群聊
const targetId = ref('1')
// 当前选中的私聊对象
const currentSelectTarget = ref(null)
// 消息记录（初始化为空，后续通过接口加载）
const msgRecord = ref([])

// 用户信息存储
const userInfoStore = computed(() => ({
  userId: authStore.currentUser?.id || 1,
  userName: authStore.currentUser?.username || '自己',
  referenceMsg: null
}))

// 消息引用存储（示例数据）
const msgStore = {
  referenceMsg: null
}

// 文件输入框引用
const fileInputRef = ref(null)

// 打字相关变量
let typingTimeout = null
const isTyping = ref(false)

/* ---------------------- 用户相关数据 ---------------------- */
// 用户列表（通过接口获取）
const userList = ref([])
// 用户 Map（通过接口获取）
const userMap = ref({})
// 在线用户列表（通过接口获取）
const onlineUsers = ref([])
// 用户搜索关键词
const userSearchValue = ref('')

// 根据搜索关键词过滤用户列表（使用 username 字段）
const userListFiltered = computed(() => {
  if (!userSearchValue.value.trim()) return userList.value
  return userList.value.filter(item => item.username.includes(userSearchValue.value))
})
// 在线用户数量（使用在线用户接口返回的数据）
const onlineCount = computed(() => onlineUsers.value.length)

/* ---------------------- 接口调用函数 ---------------------- */
/**
 * 获取用户列表接口
 */
async function fetchUserList() {
  try {
    const res = await getUserList()
    if (res.code === 0) {
      userList.value = res.data || []
    }
  } catch (error) {
    console.error("获取用户列表出错:", error)
  }
}

/**
 * 获取用户 Map 接口
 */
async function fetchUserMap() {
  try {
    const res = await getUserMap()
    if (res.code === 0) {
      userMap.value = res.data || {}
    }
  } catch (error) {
    console.error("获取用户 Map 出错:", error)
  }
}

/**
 * 获取在线用户接口
 */
async function fetchOnlineUsers() {
  try {
    const res = await getOnlineUsers()
    if (res.code === 0) {
      onlineUsers.value = res.data || []
    }
  } catch (error) {
    console.error("获取在线用户出错:", error)
  }
}

/**
 * 获取聊天记录接口
 * 调用后端 /api/v1/message/record 接口获取当前聊天对象的消息记录
 */
async function fetchChatRecord() {
  try {
    const payload = {
      targetId: targetId.value,
      index: 0,
      num: 50
    }
    const res = await getChatRecord(targetId.value, 0, 50)
    if (res.code === 0) {
      msgRecord.value = res.data || []
    }
  } catch (error) {
    console.error("获取聊天记录出错:", error)
  }
}

/**
 * 触发文件选择
 */
function triggerFileUpload() {
  fileInputRef.value?.click()
}

/**
 * 处理文件选择
 */
function handleFileSelect(event) {
  const file = event.target.files[0]
  if (!file) return

  // 验证文件
  if (!validateFile(file)) {
    event.target.value = '' // 清空输入
    return
  }

  fileUploadState.value.selectedFile = file
  fileUploadState.value.filePreview = URL.createObjectURL(file)
}

/**
 * 验证文件
 */
function validateFile(file) {
  // 检查文件大小（限制为10MB）
  const maxSize = 10 * 1024 * 1024 // 10MB
  if (file.size > maxSize) {
    alert('文件大小不能超过10MB')
    return false
  }

  // 检查文件类型（可根据需要扩展）
  const allowedTypes = ['image/*', 'application/pdf', 'text/*', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  const isAllowed = allowedTypes.some(type => {
    if (type.endsWith('/*')) {
      return file.type.startsWith(type.slice(0, -1))
    }
    return file.type === type
  })

  if (!isAllowed) {
    alert('不支持的文件类型')
    return false
  }

  return true
}

/**
 * 处理拖拽悬停
 */
function handleDragOver(event) {
  event.preventDefault()
  isDragOver.value = true
}

/**
 * 处理拖拽离开
 */
function handleDragLeave(event) {
  event.preventDefault()
  isDragOver.value = false
}

/**
 * 处理文件拖放
 */
function handleFileDrop(event) {
  event.preventDefault()
  isDragOver.value = false

  const files = event.dataTransfer.files
  if (files.length === 0) return

  // 只处理第一个文件
  const file = files[0]

  // 验证文件
  if (!validateFile(file)) {
    return
  }

  fileUploadState.value.selectedFile = file
  fileUploadState.value.filePreview = URL.createObjectURL(file)
}

/**
 * 清除文件选择
 */
function clearFileSelection() {
  fileUploadState.value.selectedFile = null
  fileUploadState.value.filePreview = null
  fileUploadState.value.uploadProgress = 0
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

/**
 * 格式化文件大小
 */
function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 下载文件
 */
function downloadFile(fileData) {
  if (!fileData || !fileData.fileId) return

  const downloadUrl = fileManagementApi.getDownloadUrl(fileData.fileId)

  // 创建临时链接并触发下载
  const link = document.createElement('a')
  link.href = downloadUrl
  link.download = fileData.fileName
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)

  console.log('下载文件:', fileData.fileName)
}

/**
 * 上传文件
 */
async function uploadFile(file) {
  try {
    fileUploadState.value.isUploading = true
    fileUploadState.value.uploadProgress = 0

    const response = await fileManagementApi.uploadFile(file, false)

    if (response.code === 0 && response.data) {
      return response.data
    } else {
      throw new Error(response.message || '文件上传失败')
    }
  } catch (error) {
    console.error('文件上传出错:', error)
    throw error
  } finally {
    fileUploadState.value.isUploading = false
    fileUploadState.value.uploadProgress = 0
  }
}

/**
 * 发送消息接口（支持文本和文件）
 * 优先使用WebSocket发送，失败时降级到HTTP请求
 */
async function handlerSubmitMsg() {
  // 检查是否有内容可发送
  const hasText = msgContent.value.trim()
  const hasFile = fileUploadState.value.selectedFile

  if (!hasText && !hasFile) return

  isSendLoading.value = true

  const isGroupChat = targetId.value === '1'
  const targetUserId = isGroupChat ? null : targetId.value

  try {
    // 如果有文件，先上传文件
    let fileData = null
    if (hasFile) {
      fileData = await uploadFile(fileUploadState.value.selectedFile)
    }

    // 准备消息内容
    let messageContent = msgContent.value.trim()
    let messageType = 1 // 默认文本消息

    if (fileData) {
      // 构建文件消息内容
      messageContent = JSON.stringify({
        fileId: fileData.id,
        fileName: fileData.originalName,
        fileSize: fileData.size,
        fileUrl: fileManagementApi.getDownloadUrl(fileData.id),
        fileType: fileData.contentType
      })
      messageType = 2 // 文件消息类型
    }

    // 生成唯一的临时ID用于消息关联
    const tempId = generateUUID()

    // 优先使用WebSocket发送消息
    if (chatStore.isConnected) {
      await chatStore.sendMessage(
        messageContent,
        targetId.value,
        isGroupChat ? 'GROUP' : 'PRIVATE',
        messageType
      )

      // 添加消息到本地记录，初始状态为发送中
      const localMessage = {
        tempId: tempId, // 临时UUID，用于精确关联
        id: null, // 真实ID待服务器返回
        fromId: userInfoStore.value.userId,
        content: messageContent,
        isRecalled: 0,
        msgContent: fileData ? `[文件] ${fileData.originalName}` : messageContent,
        timestamp: new Date(),
        isFromMe: true,
        status: 'sending', // 消息状态：sending, sent, delivered, read
        messageType: messageType,
        fileData: fileData // 保存文件信息用于渲染
      }
      msgRecord.value.push(localMessage)
    } else {
      // WebSocket未连接时降级到HTTP请求
      const messageData = {
        tempId: tempId, // 临时UUID用于精确关联
        targetId: targetUserId,
        groupId: isGroupChat ? 1 : null,
        content: messageContent,
        messageType: messageType
      }
      const response = await sendMessage(messageData)
      if (response.code === 0 && response.data) {
        // 合并服务器返回的数据与本地临时数据
        const serverMessage = response.data
        serverMessage.tempId = tempId
        serverMessage.status = 'sent'
        serverMessage.fileData = fileData
        serverMessage.msgContent = fileData ? `[文件] ${fileData.originalName}` : messageContent
        msgRecord.value.push(serverMessage)
      } else {
        throw new Error(response.message || "发送消息失败")
      }
    }

    // 清空输入
    msgContent.value = ""
    clearFileSelection()
    scrollToBottom()

  } catch (error) {
    console.error("发送消息出错:", error)
    // 尝试HTTP备用方案
    try {
      // 重新准备数据（如果文件上传失败，需要重新上传）
      let fileData = null
      if (hasFile && !fileUploadState.value.selectedFile) {
        // 如果文件已经被清除，让用户重新选择
        alert('文件发送失败，请重新选择文件')
        return
      } else if (hasFile) {
        fileData = await uploadFile(fileUploadState.value.selectedFile)
      }

      let messageContent = msgContent.value.trim()
      let messageType = 1

      if (fileData) {
        messageContent = JSON.stringify({
          fileId: fileData.id,
          fileName: fileData.originalName,
          fileSize: fileData.size,
          fileUrl: fileManagementApi.getDownloadUrl(fileData.id),
          fileType: fileData.contentType
        })
        messageType = 2
      }

      const tempId = generateUUID()
      const messageData = {
        tempId: tempId,
        targetId: targetUserId,
        groupId: isGroupChat ? 1 : null,
        content: messageContent,
        messageType: messageType
      }
      const response = await sendMessage(messageData)
      if (response.code === 0 && response.data) {
        const serverMessage = response.data
        serverMessage.tempId = tempId
        serverMessage.status = 'sent'
        serverMessage.fileData = fileData
        serverMessage.msgContent = fileData ? `[文件] ${fileData.originalName}` : messageContent
        msgRecord.value.push(serverMessage)
        console.log("消息已通过HTTP发送")
      } else {
        throw new Error(response.message || "HTTP发送消息也失败")
      }
    } catch (httpError) {
      console.error("HTTP发送消息也出错:", httpError)
      // 显示友好的错误提示
      if (chatStore.isConnected) {
        alert("消息发送失败，正在尝试重新连接...")
      } else {
        alert("消息发送失败，请检查网络连接！")
      }
    }
  } finally {
    isSendLoading.value = false
  }
}

/**
 * 撤回消息接口
 * 调用后端 /api/v1/message/recall 接口撤回指定消息
 */
async function handleRecallMessage(msgId) {
  try {
    const res = await recallMessage(msgId)
    if (res.code === 0 && res.data) {
      const updatedMsg = res.data
      const idx = msgRecord.value.findIndex(m => m.id === msgId || m.tempId === msgId)
      if (idx !== -1) {
        msgRecord.value[idx] = updatedMsg
      }
    } else {
      alert(res.message || "撤回消息失败")
    }
  } catch (error) {
    console.error("撤回消息出错:", error)
    alert("撤回消息出错，请稍后再试！")
  }
}

/**
 * 创建私聊：如果私聊不存在则创建新会话
 * 通过传入 userId 和 username 构造私聊信息
 */
function onCreatePrivateChat(userId, username) {
  const exist = privateChatList.value.find(item => item.targetId === userId.toString())
  if (!exist) {
    privateChatList.value.push({
      id: userId.toString(),
      targetId: userId.toString(),
      targetInfo: { id: userId, name: username },
      lastMessage: ''
    })
  }
  targetId.value = userId.toString()
  currentSelectTarget.value = privateChatList.value.find(item => item.targetId === userId.toString())
}

/**
 * 处理打字输入，发送打字指示器
 */
function handleTypingInput() {
  if (!isTyping.value) {
    isTyping.value = true
    chatStore.sendTypingIndicator(targetId.value, true)
  }

  // 清除之前的定时器
  if (typingTimeout) {
    clearTimeout(typingTimeout)
  }

  // 设置新的定时器，3秒后停止打字指示器
  typingTimeout = setTimeout(() => {
    isTyping.value = false
    chatStore.sendTypingIndicator(targetId.value, false)
  }, 3000)
}

/**
 * 获取正在打字的用户文本
 */
function getTypingUsersText() {
  const typingUsers = chatStore.isTyping[targetId.value] || {}
  const typingUserNames = Object.keys(typingUsers).filter(userId => typingUsers[userId] && userId !== userInfoStore.value.userId)

  if (typingUserNames.length === 0) return ''

  if (typingUserNames.length === 1) {
    const user = userList.value.find(u => u.id.toString() === typingUserNames[0])
    return user ? `${user.username} 正在输入...` : '正在输入...'
  }

  return `${typingUserNames.length} 人正在输入...`
}

/**
 * 格式化消息时间
 */
function formatMessageTime(timestamp) {
  if (!timestamp) return ''

  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date

  // 如果是今天的消息，显示时间
  if (diff < 24 * 60 * 60 * 1000 && date.getDate() === now.getDate()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  // 如果是昨天，显示昨天+时间
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.getDate() === yesterday.getDate() && date.getMonth() === yesterday.getMonth() && date.getFullYear() === yesterday.getFullYear()) {
    return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  // 否则显示月日+时间
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' ' +
         date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

/* ---------------------- 辅助函数 ---------------------- */
// 关闭左侧和右侧抽屉（用于移动端点击遮罩关闭菜单）
function closeMask() {
  showLeft.value = false
  showRight.value = false
}

// 模拟滚动到底部，重置新消息计数
function scrollToBottom() {
  currentNewMsgCount.value = 0
}

/**
 * 处理广告点击事件（示例）
 */
function handlerCardClick(card) {
  console.log('点击了广告：', card)
}

// 切换主题（示例逻辑）
function toggleDark() {
  console.log('切换主题(示例)')
}

// 登出处理函数：直接使用authStore.logout()
function handlerLogout() {
  // 断开WebSocket连接
  chatStore.disconnectWebSocket()

  // 使用authStore的logout方法，它会处理后端通知和状态清理
  authStore.logout()

  // 跳转到登录页面
  router.push('/login')
}

// 点击外部关闭表情弹窗
function handleClickOutside(event) {
  if (isEmojiVisible.value && !event.target.closest('.emoji-popup') && !event.target.closest('.emoji-button')) {
    closeEmojiPopup()
  }
}

/* ---------------------- 生命周期钩子 ---------------------- */
onMounted(() => {
  // 获取输入区域宽度（用于设置表情弹窗宽度）
  if (inputAreaRef.value) {
    inputAreaWidth.value = inputAreaRef.value.getBoundingClientRect().width
  }

  // 添加点击外部关闭事件监听
  document.addEventListener('click', handleClickOutside)

  // 连接WebSocket
  console.log('Chat页面：连接WebSocket...')
  chatStore.connectWebSocket()

  // 监听WebSocket连接状态变化
  const unwatchConnection = watch(() => chatStore.isConnected, (isConnected) => {
    if (isConnected) {
      console.log('Chat页面：WebSocket连接成功')
      // 可以在这里添加连接成功的UI提示
    } else {
      console.log('Chat页面：WebSocket连接断开')
    }
  })

  // 监听WebSocket消息，更新消息状态
  const unwatchMessages = watch(() => chatStore.messagesForCurrentChat, (newMessages) => {
    if (newMessages && newMessages.length > 0) {
      // 更新本地消息记录，同步消息状态
      newMessages.forEach(newMsg => {
        if (newMsg.isFromMe) {
          // 首先尝试通过临时ID进行精确匹配
          let localMsg = msgRecord.value.find(m => m.tempId && m.tempId === newMsg.tempId)

          // 如果没有找到临时ID匹配，回退到ID匹配（兼容旧数据）
          if (!localMsg && newMsg.id) {
            localMsg = msgRecord.value.find(m => m.id === newMsg.id)
          }

          if (localMsg) {
            // 更新消息状态和真实ID
            if (newMsg.status) {
              localMsg.status = newMsg.status
            }
            if (newMsg.id) {
              localMsg.id = newMsg.id // 更新为真实ID
            }
            // 移除临时ID（可选）
            if (newMsg.tempId && localMsg.tempId === newMsg.tempId) {
              delete localMsg.tempId
            }
          }
        }
      })
    }
  }, { deep: true })

  // 初始化：获取用户列表、用户 Map、在线用户和当前聊天记录
  fetchUserList()
  fetchUserMap()
  fetchOnlineUsers()
  fetchChatRecord()
})

// 组件卸载时清理
onUnmounted(() => {
  console.log('Chat页面：断开WebSocket连接')
  chatStore.disconnectWebSocket()
  // 移除点击外部关闭事件监听
  document.removeEventListener('click', handleClickOutside)
})

// 监听 targetId 变化，切换聊天时加载对应的聊天记录
watch(targetId, (newVal, oldVal) => {
  if (newVal !== oldVal) {
    fetchChatRecord()
  }
})
</script>


<style lang="less" scoped>
/* ===================== 主容器及背景 ===================== */
.chat-container {
  margin-bottom: 40px;
  width: 100%;
  height: 100%;
  position: absolute;
  background: var(--screen-bg-color);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.chat-bg {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background-image: var(--screen-grid-bg-color);
  background-size: 50px 50px;
}

/* ===================== 遮罩层 ===================== */
.mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.1);
  z-index: 10;
}

/* ===================== 文件传输弹窗 ===================== */
.file-transfer-modal {
  position: fixed;
  top: 100px;
  left: 50%;
  transform: translateX(-50%);
  background: #ffffff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 999;
}
.file-transfer-modal h3 {
  margin-top: 0;
}
.file-transfer-modal button {
  margin-top: 20px;
  padding: 8px 16px;
  background-color: #007BFF;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.file-transfer-modal button:hover {
  background-color: #0056b3;
}


/* ===================== 用户信息修改弹窗 ===================== */
.modify-user-modal {
  position: fixed;
  top: 300px;
  left: 50%;
  transform: translateX(-50%);
  background: #ffffff;
  padding: 20px;
  z-index: 999;
  border-radius: 8px;
}

/* ===================== 表情弹窗 ===================== */
.emoji-popup {
  position: fixed;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 12px;
  z-index: 1000;
  max-height: 420px;
  max-width: 320px;
  overflow-y: auto;
  border-radius: 12px;
  border: 2px solid rgba(0, 123, 255, 0.3);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  animation: emoji-popup-enter 0.2s ease-out;
}

@keyframes emoji-popup-enter {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.emoji-title {
  margin: 0 0 12px;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid rgba(0, 123, 255, 0.2);
  padding-bottom: 8px;
}

.emoji-search-container {
  margin-bottom: 12px;
}

.emoji-search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid rgba(0, 123, 255, 0.3);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.8);
  outline: none;
  font-size: 14px;
  transition: all 0.3s ease;
}

.emoji-search-input:focus {
  border-color: rgba(0, 123, 255, 0.6);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.2);
}

.emoji-search-input::placeholder {
  color: #999;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 6px;
  max-height: 250px;
  overflow-y: auto;
  padding: 4px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.5);
}

.emoji-grid::-webkit-scrollbar {
  width: 4px;
}

.emoji-grid::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.emoji-grid::-webkit-scrollbar-thumb {
  background: rgba(0, 123, 255, 0.5);
  border-radius: 2px;
}

.emoji-item {
  cursor: pointer;
  text-align: center;
  line-height: 32px;
  border-radius: 6px;
  font-size: 20px;
  transition: all 0.2s ease;
  user-select: none;
}

.emoji-item:hover {
  background: rgba(0, 123, 255, 0.1);
  transform: scale(1.1);
}

.emoji-item:active {
  transform: scale(0.95);
}

.emoji-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 12px;
  gap: 8px;
}

.emoji-pagination-button {
  padding: 6px 12px;
  cursor: pointer;
  background: rgba(0, 123, 255, 0.1);
  border: 1px solid rgba(0, 123, 255, 0.3);
  border-radius: 6px;
  color: #007bff;
  font-size: 12px;
  transition: all 0.3s ease;
}

.emoji-pagination-button:hover:not(:disabled) {
  background: rgba(0, 123, 255, 0.2);
  border-color: rgba(0, 123, 255, 0.5);
}

.emoji-pagination-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.emoji-pagination-info {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.emoji-package-container {
  margin-top: 12px;
  display: flex;
  justify-content: center;
  gap: 6px;
  flex-wrap: wrap;
}

.emoji-package-button {
  padding: 6px 12px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(0, 123, 255, 0.3);
  border-radius: 6px;
  font-size: 12px;
  transition: all 0.3s ease;
  color: #333;
}

.emoji-package-button:hover {
  background: rgba(0, 123, 255, 0.1);
  border-color: rgba(0, 123, 255, 0.5);
}

.emoji-package-button.active {
  background: rgba(0, 123, 255, 0.2);
  border-color: rgba(0, 123, 255, 0.6);
  color: #007bff;
  font-weight: 600;
}

.emoji-close-button {
  margin-top: 12px;
  display: block;
  width: 100%;
  padding: 8px;
  background: linear-gradient(135deg, #ff6b6b, #f44336);
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(244, 67, 54, 0.3);
}

.emoji-close-button:hover {
  background: linear-gradient(135deg, #ff5252, #d32f2f);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(244, 67, 54, 0.4);
}

/* 响应式设计 - 移动端表情弹窗优化 */
@media screen and (max-width: 480px) {
  .emoji-popup {
    max-width: calc(100vw - 20px);
    max-height: 350px;
    padding: 10px;
  }

  .emoji-grid {
    grid-template-columns: repeat(6, 1fr);
    max-height: 200px;
  }

  .emoji-item {
    font-size: 18px;
    line-height: 28px;
  }

  .emoji-package-container {
    gap: 4px;
  }

  .emoji-package-button {
    padding: 4px 8px;
    font-size: 11px;
  }
}

/* ===================== 聊天盒子 ===================== */
.chat-box {
  width: 80%;
  height: 90%;
  display: flex;
  position: relative;
  min-width: 0;
  @media screen and (max-width: 900px) {
    width: 95%;
    height: 95%;
  }
}

/* ---------------- 左侧菜单（聊天列表） ---------------- */
.box-left {
  width: 280px;
  min-width: 280px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  margin-right: 5px;
  border-radius: 5px;
  border: 3px solid rgba(0, 123, 255, 0.5);
  display: flex;
  flex-direction: column;
  padding: 0 10px;
  @media screen and (max-width: 900px) {
    position: fixed;
    left: -280px;
    top: 0;
    bottom: 0;
    margin: 0;
    z-index: 11;
    transition: all 0.3s;
    background: rgba(255, 255, 255, 0.95);
    &.show-left {
      left: 0;
    }
  }
}
.chat-list-title {
  color: rgb(0, 123, 255);
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  user-select: none;
  position: relative;
}
.close-btn {
  cursor: pointer;
  font-size: 24px;
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  @media screen and (min-width: 900px) {
    display: none;
  }
}
.chat-list-item {
  height: 60px;
  margin-bottom: 5px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  padding: 10px;
  cursor: pointer;
  user-select: none;
  position: relative;
}
.group-chat {
  background-image: url('/group-bg.png');
  background-size: cover;
  background-repeat: no-repeat;
  background-position: center;
}
.chat-item-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex: 1;
  overflow: hidden;
}
.chat-content-name {
  font-weight: bold;
  margin-bottom: 5px;
  font-size: 14px;
  color: #000;
}
.chat-content-msg {
  font-size: 12px;
  color: #555;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.active-chat {
  background-color: rgba(0, 123, 255, 0.2);
}
.delete-chat-button {
  margin-left: 10px;
  background: #f00;
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
}
.chat-avatar-small {
  width: 40px;
  height: 40px;
  background: #ccc;
  margin-right: 10px;
  border-radius: 50%;
}
.ad-container {
  margin-bottom: 10px;
}
.ad-image {
  width: 100%;
  border-radius: 5px;
  cursor: pointer;
}

/* ---------------- 中间部分（消息展示及输入区） ---------------- */
.box-middle {
  flex: 1;
  min-width: 300px;
  margin-right: 5px;
  border-radius: 5px;
  display: flex;
  flex-direction: column;
  @media screen and (max-width: 900px) {
    margin: 0;
  }
}
.middle-top {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  margin-bottom: 5px;
  border-radius: 5px;
  border: 3px solid rgba(0, 123, 255, 0.5);
  font-size: 18px;
  font-weight: 600;
  user-select: none;
  position: relative;
}
.menu-btn {
  cursor: pointer;
  font-size: 24px;
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
}
.menu-btn:first-of-type {
  left: 10px;
}
.menu-btn:last-of-type {
  right: 10px;
}
.middle-content {
  flex: 1;
  border-radius: 5px;
  background-image: linear-gradient(130deg, rgba(255, 255, 255, 0.3), rgba(255, 255, 255, 0.5));
  backdrop-filter: blur(10px);
  border: 3px solid rgba(0, 123, 255, 0.5);
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}
.chat-show-area {
  flex: 1;
  padding: 10px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 5px;
  margin-bottom: 10px;
}
.chat-message-container {
  background: #fff;
  padding: 10px;
  border-radius: 8px;
  max-width: 60%;
}
.bubble {
  background: #f0f0f0;
  color: #333;
  padding: 10px 15px;
  border-radius: 15px;
  word-wrap: break-word;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}
.msg-item {
  font-size: 20px;
  display: flex;
  margin-bottom: 8px;
}
.sending-indicator {
  text-align: center;
}
.new-msg-count {
  position: fixed;
  right: 15px;
  bottom: 80px;
  padding: 4px 15px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  color: rgba(0, 123, 255, 1);
  font-size: 14px;
  user-select: none;
  border: 2px solid rgba(0, 123, 255, 1);
  font-weight: 600;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
}

/* ---------------- 输入区域及发送按钮 ---------------- */
.chat-input-area {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  margin: 15px 0;
  position: relative;
  transition: all 0.3s ease;
}

.chat-input-area.drag-over {
  background: rgba(64, 158, 255, 0.05);
  border: 2px dashed rgba(64, 158, 255, 0.3);
  border-radius: 10px;
}

.chat-input-area.drag-over::before {
  content: '拖拽文件到这里上传';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #409eff;
  font-size: 16px;
  font-weight: 500;
  pointer-events: none;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px 16px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}
.chat-input-container {
  width: 80%;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 10px;
  overflow: hidden;
  padding: 10px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}
.chat-msg-input {
  flex: 1;
}
.chat-text-input {
  width: 100%;
  outline: none;
  border: none;
  background: transparent;
  color: #000;
  font-size: 16px;
  padding: 5px;
}
.emoji-button {
  width: 28px;
  height: 28px;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  color: rgba(0, 0, 0, 0.5);
  user-select: none;
  position: absolute;
  right: 45px;
  top: 10px;
}

.file-button {
  width: 28px;
  height: 28px;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  color: rgba(0, 0, 0, 0.5);
  user-select: none;
  position: absolute;
  right: 10px;
  top: 10px;
  transition: color 0.3s;
}

.file-button:hover {
  color: rgba(0, 123, 255, 0.8);
}
.publish-button {
  height: 55px;
  width: 55px;
  border-radius: 10px;
  background: rgb(0, 123, 255);
  border: none;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  cursor: pointer;
  margin-left: 10px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  transition: background 0.3s, transform 0.3s;
}
.publish-button:hover {
  background: rgba(0, 123, 255, 0.8);
  transform: scale(1.05);
}
/* 发送按钮禁用状态样式 */
.publish-button:disabled {
  background: grey;
  cursor: not-allowed;
  opacity: 0.6;
}

/* ===================== 文件预览和上传样式 ===================== */
.file-preview-container {
  margin-top: 10px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 6px;
  border: 1px solid #e0e0e0;
}

.file-preview {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.file-size {
  font-size: 12px;
  color: #666;
}

.file-actions {
  margin-left: 8px;
}

.remove-file-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 2px;
  border-radius: 3px;
  transition: background 0.3s;
}

.remove-file-btn:hover {
  background: rgba(244, 67, 54, 0.1);
}

.upload-progress {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-bar {
  flex: 1;
  height: 4px;
  background: #e0e0e0;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #4caf50;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #666;
  min-width: 35px;
}

/* ===================== 文件消息样式 ===================== */
.file-message {
  width: 100%;
}

.file-message-content {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.3s;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.file-message-content:hover {
  background: rgba(255, 255, 255, 0.2);
}

.file-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.file-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.file-message .file-name {
  font-size: 14px;
  font-weight: 500;
  color: inherit;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-message .file-size {
  font-size: 12px;
  opacity: 0.8;
  color: inherit;
}

.download-hint {
  font-size: 12px;
  opacity: 0.7;
  color: inherit;
  white-space: nowrap;
}

/* 发送消息中的文件消息特殊样式 */
.bubble.sent-message .file-message-content {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}

.bubble.sent-message .file-message-content:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* ---------------- 右侧菜单 ---------------- */
.box-right {
  width: 280px;
  min-width: 280px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  margin-left: 5px;
  border-radius: 5px;
  border: 3px solid rgba(0, 123, 255, 0.5);
  display: flex;
  flex-direction: column;
  padding: 0 10px;
  @media screen and (max-width: 900px) {
    position: fixed;
    right: -280px;
    top: 0;
    bottom: 0;
    margin: 0;
    z-index: 11;
    transition: all 0.3s;
    background: rgba(255, 255, 255, 0.95);
    &.show-right {
      right: 0;
    }
  }
}
.right-top {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  margin-bottom: 5px;
  border-radius: 5px;
  border: 3px solid rgba(0, 123, 255, 0.5);
  padding: 5px;
}
.user-info {
  display: flex;
  align-items: center;
}
.avatar2 {
  width: 40px;
  height: 40px;
  background: #888;
  border-radius: 50%;
  margin-right: 5px;
  cursor: pointer;
}
.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #000;
}
.right-btn-group button {
  margin-left: 10px;
  padding: 4px 8px;
  cursor: pointer;
}
.right-content {
  flex: 1;
  min-height: 300px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  border-radius: 5px;
  border: 3px solid rgba(0, 123, 255, 0.5);
  padding: 5px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}
.user-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.user-search-input {
  border-radius: 5px;
  height: 30px;
  width: 140px;
  outline: none;
  border: 1px solid #ddd;
  padding: 0 8px;
}
.online-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 5px;
  margin-right: -5px;
}
.online-list-item {
  height: 50px;
  border-radius: 5px;
  background-image: linear-gradient(
      to right,
      rgba(0, 123, 255, 0.2),
      rgba(0, 123, 255, 0)
  );
  margin-bottom: 5px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px;
  position: relative;
}
.online-list-item.odd {
  background-image: linear-gradient(
      to left,
      rgba(0, 123, 255, 0.2),
      rgba(0, 123, 255, 0)
  );
}
.online-item-content {
  display: flex;
  align-items: center;
  position: relative;
}
.avatar1 {
  width: 40px;
  height: 40px;
  background: #aaa;
  border-radius: 50%;
}
.online-username {
  max-width: 100px;
  margin-left: 10px;
  font-weight: 600;
  color: #000;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.online-status {
  position: absolute;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  right: 0;
  bottom: 0;
  background: rgb(0, 123, 255);
  border: 2px solid #fff;
}
.online-item-operation {
  opacity: 0;
  transition: opacity 0.5s ease;
  pointer-events: none;
}
.online-list-item:hover .online-item-operation {
  opacity: 1;
  pointer-events: auto;
}
@media screen and (max-width: 900px) {
  .online-item-operation {
    opacity: 1;
    pointer-events: auto;
  }
}

/* ===================== 打字指示器样式 ===================== */
.typing-indicator {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  margin: 5px 0;
  background: rgba(0, 123, 255, 0.1);
  border-radius: 15px;
  max-width: 200px;
}
.typing-dots {
  display: flex;
  align-items: center;
  margin-right: 8px;
}
.typing-dots span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #007bff;
  margin: 0 2px;
  animation: typing-animation 1.4s infinite;
}
.typing-dots span:nth-child(1) {
  animation-delay: 0s;
}
.typing-dots span:nth-child(2) {
  animation-delay: 0.2s;
}
.typing-dots span:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes typing-animation {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}
.typing-text {
  font-size: 14px;
  color: #666;
  font-style: italic;
}

/* ===================== 消息状态样式 ===================== */
.message-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-top: 5px;
  font-size: 12px;
  color: #999;
}
.message-status {
  margin-bottom: 2px;
}
.status-sending {
  color: #ff9800;
}
.status-sent {
  color: #4caf50;
}
.status-delivered {
  color: #2196f3;
}
.status-read {
  color: #8bc34a;
}
.message-time {
  margin-bottom: 2px;
}
.recall-btn {
  background: #f44336;
  color: white;
  border: none;
  border-radius: 3px;
  padding: 2px 6px;
  cursor: pointer;
  font-size: 11px;
  transition: background 0.3s;
}
.recall-btn:hover {
  background: #d32f2f;
}

/* ===================== 消息气泡增强样式 ===================== */
.bubble.sent-message {
  background: #007bff;
  color: white;
}

/* ===================== 全局滚动条样式 ===================== */
.chat-container {
  background-color: white;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: gray #1a1a1a;
}
.chat-container::-webkit-scrollbar {
  background-color: white;
  width: 8px;
}
.chat-container::-webkit-scrollbar-track {
  background: #1a1a1a;
}
.chat-container::-webkit-scrollbar-thumb {
  background: white;
  border-radius: 4px;
}
</style>
