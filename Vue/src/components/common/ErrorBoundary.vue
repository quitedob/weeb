<template>
  <div class="error-boundary">
    <div v-if="hasError" class="error-container">
      <el-result
        icon="error"
        :title="errorTitle"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-space>
            <el-button type="primary" @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重新加载
            </el-button>
            <el-button @click="handleGoBack">
              <el-icon><Back /></el-icon>
              返回上一页
            </el-button>
          </el-space>
        </template>
      </el-result>
      
      <el-collapse v-if="showDetails" class="error-details">
        <el-collapse-item title="错误详情" name="1">
          <div class="error-stack">
            <pre>{{ errorStack }}</pre>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
    
    <slot v-else></slot>
  </div>
</template>

<script setup>
import { ref, onErrorCaptured, computed } from 'vue';
import { useRouter } from 'vue-router';
import { Refresh, Back } from '@element-plus/icons-vue';

const props = defineProps({
  fallbackTitle: {
    type: String,
    default: '页面出错了'
  },
  fallbackMessage: {
    type: String,
    default: '抱歉，页面遇到了一些问题'
  },
  showDetails: {
    type: Boolean,
    default: import.meta.env.DEV // 仅在开发环境显示详情
  },
  onError: {
    type: Function,
    default: null
  }
});

const emit = defineEmits(['error', 'reset']);

const router = useRouter();
const hasError = ref(false);
const error = ref(null);
const errorInfo = ref(null);

const errorTitle = computed(() => {
  return error.value?.message || props.fallbackTitle;
});

const errorMessage = computed(() => {
  return errorInfo.value?.componentName 
    ? `组件 ${errorInfo.value.componentName} 发生错误`
    : props.fallbackMessage;
});

const errorStack = computed(() => {
  if (!error.value) return '';
  return error.value.stack || error.value.toString();
});

// 捕获子组件错误
onErrorCaptured((err, instance, info) => {
  console.error('ErrorBoundary 捕获到错误:', err);
  console.error('错误信息:', info);
  console.error('组件实例:', instance);
  
  hasError.value = true;
  error.value = err;
  errorInfo.value = {
    componentName: instance?.$options?.name || instance?.$options?.__name || 'Unknown',
    info: info
  };
  
  // 调用自定义错误处理函数
  if (props.onError) {
    props.onError(err, instance, info);
  }
  
  // 触发错误事件
  emit('error', { error: err, instance, info });
  
  // 阻止错误继续向上传播
  return false;
});

const handleReset = () => {
  hasError.value = false;
  error.value = null;
  errorInfo.value = null;
  emit('reset');
};

const handleGoBack = () => {
  router.back();
};
</script>

<style scoped>
.error-boundary {
  width: 100%;
  height: 100%;
}

.error-container {
  padding: 40px 20px;
  max-width: 800px;
  margin: 0 auto;
}

.error-details {
  margin-top: 20px;
}

.error-stack {
  background-color: #f5f5f5;
  padding: 16px;
  border-radius: 4px;
  overflow-x: auto;
}

.error-stack pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  color: #e74c3c;
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* 暗色模式支持 */
@media (prefers-color-scheme: dark) {
  .error-stack {
    background-color: #2c2c2c;
  }
  
  .error-stack pre {
    color: #ff6b6b;
  }
}
</style>
