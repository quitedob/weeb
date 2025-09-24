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

    <!-- 音视频聊天弹窗（占位示例） -->
    <div v-if="videoInfo.videoVisible">
      <div class="mask"></div>
      <div class="video-modal">
        <h3 class="video-modal__title">音视频聊天(占位)</h3>
        <p class="video-modal__info">目标：{{ videoInfo.videoTargetInfo }}</p>
        <p class="video-modal__info">是否仅音频：{{ videoInfo.videoIsOnlyAudio }}</p>
        <button class="video-modal__close" @click="videoInfo.videoVisible = false">关闭</button>
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
                  :key="msg.id"
                  class="msg-item"
                  :style="{
                  justifyContent: msg.fromId === userInfoStore.userId ? 'flex-end' : 'flex-start'
                }"
              >
                <div class="chat-message-container">
                  <!-- 如果消息已撤回则显示提示，否则显示消息内容 -->
                  <div class="bubble">
                    {{ msg.isRecalled === 1 ? '消息已撤回' : msg.msgContent }}
                  </div>
                  <!-- 撤回按钮：仅对当前用户自己发送的且消息未撤回时显示 -->
                  <button v-if="msg.fromId === userInfoStore.userId && msg.isRecalled !== 1" @click="recallMessage(msg.id)">
                    撤回
                  </button>
                </div>
              </div>
              <!-- 正在发送提示 -->
              <div v-if="isSendLoading" class="sending-indicator">
                <strong>发送中...</strong>
              </div>
              <!-- 新消息计数，点击滚动到底部 -->
              <div v-if="currentNewMsgCount > 0" class="new-msg-count" @click="scrollToBottom">
                ▼ {{ currentNewMsgCount }} 条新消息
              </div>
            </div>
            <!-- 聊天输入区 -->
            <div class="chat-input-area">
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
                <!-- 消息输入框 -->
                <div class="chat-msg-input">
                  <input
                      v-model="msgContent"
                      type="text"
                      placeholder="请输入消息"
                      class="chat-text-input"
                      @keyup.enter="handlerSubmitMsg"
                  />
                </div>
              </div>
              <!-- 发送按钮：当输入为空时禁用，并显示灰色 -->
              <button
                  class="publish-button"
                  :disabled="!msgContent.trim()"
                  @click="handlerSubmitMsg"
              >
                发送
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
import { ref, computed, onMounted, watch } from 'vue'
// 导入 axios 用于发送 HTTP 请求
import { instance } from '../api/axiosInstance'
// 导入表情包数据（请确保路径正确）
import emojis from '@constant/emoji/emoji.js'
// 导入 Vue Router 用于页面跳转
import { useRouter } from 'vue-router'

const router = useRouter()

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
  emojiPosition.value.x = rect.left
  emojiPosition.value.y = rect.top - popupHeight
  inputAreaWidth.value = rect.width
  // 切换表情弹窗显示状态
  isEmojiVisible.value = !isEmojiVisible.value
}

/* ---------------------- 聊天逻辑 ---------------------- */
// 文件传输信息对象（占位示例）
const fileInfo = ref({
  fileVisible: false,
  fileTargetInfo: null,
  fileName: ''
})
// 音视频聊天信息对象（占位示例）
const videoInfo = ref({
  videoVisible: false,
  videoTargetInfo: null,
  videoIsSend: false,
  videoIsOnlyAudio: false
})
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

// 用户信息存储（示例数据，实际应从后端获取）
const userInfoStore = {
  userId: 1, // 假设当前用户ID为 1
  userName: '自己',
  referenceMsg: null
}
// 消息引用存储（示例数据）
const msgStore = {
  referenceMsg: null
}

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
async function getUserList() {
  try {
    const res = await instance.get('/list') // 后端 AuthController 提供 /list
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
async function getUserMap() {
  try {
    const res = await instance.get('/list/map') // 后端 AuthController 提供 /list/map
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
async function getOnlineUsers() {
  try {
    const res = await instance.get('/online/web') // 后端 AuthController 提供 /online/web
    if (res.code === 0) {
      onlineUsers.value = res.data || []
    }
  } catch (error) {
    console.error("获取在线用户出错:", error)
  }
}

/**
 * 获取聊天记录接口
 * 调用后端 /api/messages/record 接口获取当前聊天对象的消息记录
 */
async function fetchChatRecord() {
  try {
    const payload = {
      targetId: targetId.value,
      index: 0,
      num: 50
    }
    const res = await instance.post('/api/messages/record', payload)
    if (res.code === 0) {
      msgRecord.value = res.data || []
    }
  } catch (error) {
    console.error("获取聊天记录出错:", error)
  }
}

/**
 * 发送消息接口
 * 使用 axios 发送 POST 请求至后端 /api/messages/send 接口，
 * 请求体中封装消息内容及类型。
 * 后端返回成功后，将返回的消息对象添加到消息记录中。
 */
async function handlerSubmitMsg() {
  if (!msgContent.value.trim()) return
  isSendLoading.value = true
  try {
    // 构造消息对象
    const messageData = {
      receiverId: targetId.value === '1' ? null : targetId.value,
      groupId: targetId.value === '1' ? 1 : null,
      content: msgContent.value,
      messageType: 1  // 文本消息
    }
    const response = await instance.post('/api/messages/send', messageData)
    if (response.code === 0 && response.data) {
      msgRecord.value.push(response.data)
    } else {
      alert(response.message || "发送消息失败！")
    }
  } catch (error) {
    console.error("发送消息出错:", error)
    alert("发送消息出错，请稍后再试！")
  } finally {
    isSendLoading.value = false
    msgContent.value = ""
    scrollToBottom()
  }
}

/**
 * 撤回消息接口
 * 调用后端 /api/messages/recall 接口撤回指定消息
 */
async function recallMessage(msgId) {
  try {
    const payload = { msgId: msgId }
    const res = await instance.post('/api/messages/recall', payload)
    if (res.code === 0 && res.data) {
      const updatedMsg = res.data
      const idx = msgRecord.value.findIndex(m => m.id === msgId)
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

// 登出处理函数：在执行登出逻辑后跳转到 '/logout'
function handlerLogout() {
  router.push('/logout')
}

/* ---------------------- 生命周期钩子 ---------------------- */
onMounted(() => {
  // 获取输入区域宽度（用于设置表情弹窗宽度）
  if (inputAreaRef.value) {
    inputAreaWidth.value = inputAreaRef.value.getBoundingClientRect().width
  }
  // 初始化：获取用户列表、用户 Map、在线用户和当前聊天记录
  getUserList()
  getUserMap()
  getOnlineUsers()
  fetchChatRecord()
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

/* ===================== 音视频聊天弹窗 ===================== */
.video-modal {
  position: fixed;
  top: 200px;
  left: 50%;
  transform: translateX(-50%);
  background: #ffffff;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  border-radius: 8px;
  width: 300px;
  z-index: 999;
}
.video-modal__title {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 1.5em;
  text-align: center;
}
.video-modal__info {
  margin: 10px 0;
  font-size: 1em;
}
.video-modal__close {
  display: block;
  width: 100%;
  padding: 10px;
  margin-top: 20px;
  background-color: #f44336;
  color: #ffffff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1em;
}
.video-modal__close:hover {
  background-color: #d32f2f;
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
  background: #fff;
  padding: 8px;
  z-index: 999;
  max-height: 400px;
  overflow-y: auto;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.emoji-title {
  margin: 0 0 8px;
  text-align: center;
}
.emoji-search-container {
  margin-bottom: 8px;
}
.emoji-search-input {
  width: 80%;
  padding: 4px;
  display: block;
  margin: 0 auto;
}
.emoji-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}
.emoji-item {
  cursor: pointer;
  text-align: center;
  line-height: 32px;
  border-radius: 4px;
}
.emoji-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 10px;
}
.emoji-pagination-button {
  margin: 0 10px;
  padding: 4px 8px;
  cursor: pointer;
}
.emoji-pagination-info {
  font-size: 14px;
}
.emoji-package-container {
  margin-top: 10px;
  display: flex;
  justify-content: center;
  gap: 8px;
}
.emoji-package-button {
  border: 1px solid #ccc;
  padding: 5px 8px;
  cursor: pointer;
  background-color: #f5f5f5;
  border-radius: 4px;
}
.emoji-package-button.active {
  background-color: #ddd;
}
.emoji-close-button {
  margin-top: 6px;
  display: block;
  width: 100%;
  padding: 6px;
  background-color: #f44336;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
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
  right: 10px;
  top: 10px;
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
