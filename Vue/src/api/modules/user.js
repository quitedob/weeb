// File path: /Vue/src/api/modules/user.js
import { instance } from '../axiosInstance'; // Corrected import

export default {
  getUserInfo() {
    // Corresponds to UserController @GetMapping("/info") -> /api/user/info
    return instance.get('/user/info');
  },
  updateUserInfo(data) {
    // Corresponds to UserController @PostMapping("/update") -> /api/user/update
    return instance.post('/user/update', data);
  },
  getUserInfoById(userId) {
    // This is a common requirement, e.g., for fetching profile details of other users.
    // Assuming a backend endpoint like GET /api/user/{userId}
    // Backend UserController does not have this yet, but it's a sensible addition.
    // For now, we define it as per common practice.
    // If this specific endpoint is not on UserController, it will fail until backend is updated.
    // The GroupDetail.vue example used api.user.getUserInfoById(group.value.ownerId)
    return instance.get(`/user/${userId}`);
  }
};
