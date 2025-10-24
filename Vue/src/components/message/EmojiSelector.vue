<template>
  <div class="emoji-selector" v-if="visible" :style="position">
    <div class="emoji-header">
      <h4>选择表情</h4>
      <button class="close-button" @click="close">×</button>
    </div>

    <div class="emoji-search">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="搜索表情..."
        class="search-input"
        ref="searchInput"
      />
    </div>

    <div class="emoji-categories">
      <div
        v-for="category in categories"
        :key="category.name"
        class="category-section"
      >
        <h5 class="category-title">{{ category.name }}</h5>
        <div class="emoji-grid">
          <div
            v-for="emoji in filterEmojis(category.emojis)"
            :key="emoji.name"
            :title="emoji.name"
            class="emoji-item"
            @click="selectEmoji(emoji)"
          >
            {{ emoji.icon }}
          </div>
        </div>
      </div>
    </div>

    <div v-if="filteredEmojis.length === 0" class="no-results">
      没有找到匹配的表情
    </div>
  </div>
</template>

<script>
import { ref, computed, watch, nextTick } from 'vue'

// 表情数据分类
const emojiCategories = [
  {
    name: '常用表情',
    emojis: [
      { icon: '😀', name: 'grinning face', keywords: ['happy', 'smile'] },
      { icon: '😃', name: 'grinning face with big eyes', keywords: ['happy', 'smile'] },
      { icon: '😄', name: 'grinning face with smiling eyes', keywords: ['happy', 'smile'] },
      { icon: '😁', name: 'beaming face with smiling eyes', keywords: ['happy', 'smile'] },
      { icon: '😅', name: 'grinning face with sweat', keywords: ['sweat', 'smile'] },
      { icon: '😂', name: 'face with tears of joy', keywords: ['laugh', 'happy'] },
      { icon: '🤣', name: 'rolling on the floor laughing', keywords: ['laugh', 'happy'] },
      { icon: '😊', name: 'smiling face with smiling eyes', keywords: ['happy', 'smile'] },
      { icon: '😇', name: 'smiling face with halo', keywords: ['angel', 'innocent'] },
      { icon: '🙂', name: 'slightly smiling face', keywords: ['smile', 'happy'] },
      { icon: '😉', name: 'winking face', keywords: ['wink', 'happy'] },
      { icon: '😌', name: 'relieved face', keywords: ['relaxed', 'peaceful'] },
      { icon: '😍', name: 'heart eyes face', keywords: ['love', 'heart'] },
      { icon: '🥰', name: 'smiling face with hearts', keywords: ['love', 'happy'] },
      { icon: '😘', name: 'face blowing a kiss', keywords: ['kiss', 'love'] }
    ]
  },
  {
    name: '手势',
    emojis: [
      { icon: '👍', name: 'thumbs up', keywords: ['good', 'ok', 'yes'] },
      { icon: '👎', name: 'thumbs down', keywords: ['bad', 'no'] },
      { icon: '👌', name: 'OK hand', keywords: ['ok', 'yes'] },
      { icon: '✌️', name: 'victory hand', keywords: ['peace', 'victory'] },
      { icon: '🤞', name: 'crossed fingers', keywords: ['luck', 'hope'] },
      { icon: '🤟', name: 'love-you gesture', keywords: ['love', 'heart'] },
      { icon: '🤘', name: 'sign of the horns', keywords: ['rock', 'metal'] },
      { icon: '🤙', name: 'call me hand', keywords: ['call', 'phone'] },
      { icon: '👋', name: 'waving hand', keywords: ['wave', 'hello', 'goodbye'] },
      { icon: '🤏', name: 'pinching hand', keywords: ['small', 'pinch'] },
      { icon: '✋', name: 'raised hand', keywords: ['stop', 'hand'] },
      { icon: '🤚', name: 'raised back of hand', keywords: ['stop', 'back'] },
      { icon: '🖐️', name: 'hand with fingers splayed', keywords: ['hand', 'five'] },
      { icon: '🖖', name: 'vulcan salute', keywords: ['star trek', 'spock'] },
      { icon: '👏', name: 'clapping hands', keywords: ['clap', 'applause'] }
    ]
  },
  {
    name: '情感',
    emojis: [
      { icon: '❤️', name: 'red heart', keywords: ['love', 'heart'] },
      { icon: '🧡', name: 'orange heart', keywords: ['love', 'heart'] },
      { icon: '💛', name: 'yellow heart', keywords: ['love', 'heart'] },
      { icon: '💚', name: 'green heart', keywords: ['love', 'heart'] },
      { icon: '💙', name: 'blue heart', keywords: ['love', 'heart'] },
      { icon: '💜', name: 'purple heart', keywords: ['love', 'heart'] },
      { icon: '🖤', name: 'black heart', keywords: ['love', 'heart'] },
      { icon: '🤍', name: 'white heart', keywords: ['love', 'heart'] },
      { icon: '💔', name: 'broken heart', keywords: ['broken', 'sad'] },
      { icon: '❣️', name: 'exclamation heart', keywords: ['love', 'heart'] },
      { icon: '💕', name: 'two hearts', keywords: ['love', 'heart'] },
      { icon: '💞', name: 'revolving hearts', keywords: ['love', 'heart'] },
      { icon: '💓', name: 'beating heart', keywords: ['love', 'heart'] },
      { icon: '💗', name: 'growing heart', keywords: ['love', 'heart'] },
      { icon: '💖', name: 'sparkling heart', keywords: ['love', 'heart'] }
    ]
  },
  {
    name: '动物',
    emojis: [
      { icon: '🐶', name: 'dog face', keywords: ['dog', 'animal'] },
      { icon: '🐱', name: 'cat face', keywords: ['cat', 'animal'] },
      { icon: '🐭', name: 'mouse face', keywords: ['mouse', 'animal'] },
      { icon: '🐹', name: 'hamster face', keywords: ['hamster', 'animal'] },
      { icon: '🐰', name: 'rabbit face', keywords: ['rabbit', 'animal'] },
      { icon: '🦊', name: 'fox face', keywords: ['fox', 'animal'] },
      { icon: '🐻', name: 'bear face', keywords: ['bear', 'animal'] },
      { icon: '🐼', name: 'panda face', keywords: ['panda', 'animal'] },
      { icon: '🐨', name: 'koala face', keywords: ['koala', 'animal'] },
      { icon: '🐯', name: 'tiger face', keywords: ['tiger', 'animal'] },
      { icon: '🦁', name: 'lion face', keywords: ['lion', 'animal'] },
      { icon: '🐮', name: 'cow face', keywords: ['cow', 'animal'] },
      { icon: '🐷', name: 'pig face', keywords: ['pig', 'animal'] },
      { icon: '🐸', name: 'frog face', keywords: ['frog', 'animal'] },
      { icon: '🐵', name: 'monkey face', keywords: ['monkey', 'animal'] }
    ]
  },
  {
    name: '食物',
    emojis: [
      { icon: '🍎', name: 'red apple', keywords: ['apple', 'fruit'] },
      { icon: '🍊', name: 'tangerine', keywords: ['orange', 'fruit'] },
      { icon: '🍋', name: 'lemon', keywords: ['lemon', 'fruit'] },
      { icon: '🍌', name: 'banana', keywords: ['banana', 'fruit'] },
      { icon: '🍉', name: 'watermelon', keywords: ['watermelon', 'fruit'] },
      { icon: '🍇', name: 'grapes', keywords: ['grape', 'fruit'] },
      { icon: '🍓', name: 'strawberry', keywords: ['strawberry', 'fruit'] },
      { icon: '🫐', name: 'blueberries', keywords: ['blueberry', 'fruit'] },
      { icon: '🍈', name: 'melon', keywords: ['melon', 'fruit'] },
      { icon: '🍒', name: 'cherries', keywords: ['cherry', 'fruit'] },
      { icon: '🍑', name: 'peach', keywords: ['peach', 'fruit'] },
      { icon: '🥭', name: 'mango', keywords: ['mango', 'fruit'] },
      { icon: '🍍', name: 'pineapple', keywords: ['pineapple', 'fruit'] },
      { icon: '🥥', name: 'coconut', keywords: ['coconut', 'fruit'] },
      { icon: '🥝', name: 'kiwi fruit', keywords: ['kiwi', 'fruit'] }
    ]
  }
]

export default {
  name: 'EmojiSelector',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    position: {
      type: Object,
      default: () => ({
        top: '0px',
        left: '0px'
      })
    }
  },
  emits: ['select', 'close'],
  setup(props, { emit }) {
    const searchQuery = ref('')
    const searchInput = ref(null)
    const categories = ref(emojiCategories)

    // 过滤表情
    const filterEmojis = (emojis) => {
      if (!searchQuery.value.trim()) {
        return emojis
      }

      const query = searchQuery.value.toLowerCase()
      return emojis.filter(emoji =>
        emoji.name.toLowerCase().includes(query) ||
        emoji.keywords.some(keyword => keyword.toLowerCase().includes(query)) ||
        emoji.icon.includes(query)
      )
    }

    // 获取所有过滤后的表情
    const filteredEmojis = computed(() => {
      if (!searchQuery.value.trim()) {
        return []
      }

      let allEmojis = []
      categories.value.forEach(category => {
        allEmojis = allEmojis.concat(filterEmojis(category.emojis))
      })
      return allEmojis
    })

    // 选择表情
    const selectEmoji = (emoji) => {
      emit('select', emoji.icon)
      close()
    }

    // 关闭选择器
    const close = () => {
      emit('close')
      searchQuery.value = ''
    }

    // 监听可见性变化，自动聚焦搜索框
    watch(() => props.visible, (newVal) => {
      if (newVal) {
        nextTick(() => {
          searchInput.value?.focus()
        })
      }
    })

    return {
      searchQuery,
      searchInput,
      categories,
      filteredEmojis,
      filterEmojis,
      selectEmoji,
      close
    }
  }
}
</script>

<style scoped>
.emoji-selector {
  position: fixed;
  background: white;
  border: 1px solid #ddd;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  width: 350px;
  max-height: 400px;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.emoji-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
  background: #f8f9fa;
}

.emoji-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.close-button {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #666;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.close-button:hover {
  background: #e9ecef;
}

.emoji-search {
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: #007bff;
}

.emoji-categories {
  max-height: 250px;
  overflow-y: auto;
  padding: 10px 0;
}

.category-section {
  margin-bottom: 15px;
}

.category-title {
  margin: 0 0 8px 0;
  padding: 0 20px;
  font-size: 13px;
  font-weight: 600;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
  padding: 0 20px;
}

.emoji-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s;
  font-size: 18px;
}

.emoji-item:hover {
  background: #f0f0f0;
}

.no-results {
  text-align: center;
  padding: 20px;
  color: #666;
  font-style: italic;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .emoji-selector {
    width: 90vw;
    max-width: 320px;
  }

  .emoji-grid {
    grid-template-columns: repeat(7, 1fr);
  }

  .emoji-item {
    width: 28px;
    height: 28px;
    font-size: 16px;
  }
}

/* 滚动条样式 */
.emoji-categories::-webkit-scrollbar {
  width: 6px;
}

.emoji-categories::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.emoji-categories::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.emoji-categories::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>