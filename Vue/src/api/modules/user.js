// File path: /Vue/src/api/modules/user.js
import { instance } from '../axiosInstance'; // Corrected import

export default {
  getUserInfo() {
    // 对齐后端：UserController @GetMapping("/info") -> /api/user/info
    return instance.get('/api/user/info');
  },
  updateUserInfo(data) {
    // 标准化：使用 PUT /api/user/info
    return instance.put('/api/user/info', data);
  },
  getUserInfoById(userId) {
    // 对齐后端新增：GET /api/user/{userId}
    return instance.get(`/api/user/${userId}`);
  }
};
