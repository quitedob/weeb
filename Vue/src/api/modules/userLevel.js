import axiosInstance from '@/api/axiosInstance'

// 用户等级相关API
export const userLevelApi = {
  // 获取用户当前等级信息
  getUserLevelInfo() {
    return axiosInstance.get('/api/user/level/info')
  },

  // 获取用户等级历史
  getLevelHistory(params) {
    return axiosInstance.get('/api/user/level/history', { params })
  },

  // 获取升级进度
  getUpgradeProgress() {
    return axiosInstance.get('/api/user/level/upgrade-progress')
  },

  // 获取用户统计数据
  getUserStats() {
    return axiosInstance.get('/api/user/level/stats')
  },

  // 检查是否可以升级
  checkUpgrade() {
    return axiosInstance.post('/api/user/level/check-upgrade')
  },

  // 获取等级权益说明
  getLevelBenefits() {
    return axiosInstance.get('/api/user/level/benefits')
  },

  // 获取等级排行榜
  getLevelLeaderboard(params) {
    return axiosInstance.get('/api/user/level/leaderboard', { params })
  }
}

// 管理员用户等级管理API
export const adminUserLevelApi = {
  // 设置用户等级
  setUserLevel(userId, level, reason) {
    return axiosInstance.post(`/api/admin/user/${userId}/level`, {
      level,
      reason
    })
  },

  // 批量设置用户等级
  batchSetUserLevel(userIds, level, reason) {
    return axiosInstance.post('/api/admin/user/batch-level', {
      userIds,
      level,
      reason
    })
  },

  // 获取用户等级统计
  getLevelStatistics() {
    return axiosInstance.get('/api/admin/user/level/statistics')
  },

  // 获取所有用户的等级历史
  getAllUserLevelHistory(params) {
    return axiosInstance.get('/api/admin/user/level/all-history', { params })
  },

  // 手动触发用户等级检查
  triggerLevelCheck(userId) {
    return axiosInstance.post(`/api/admin/user/${userId}/level-check`)
  }
}