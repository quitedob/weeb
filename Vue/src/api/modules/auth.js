// File path: /Vue/src/api/modules/auth.js
import { instance } from '../axiosInstance'; // Corrected import

export default {
  login(data) {
    return instance.post('/login', data); // Path relative to baseURL in instance
  },
  register(data) {
    return instance.post('/register', data);
  },
  getCaptcha() {
    return instance.get('/captcha');
  },
  // Adding a logout function as it's commonly needed and was in AsideMenu.vue
  // Assuming backend has a POST /api/logout endpoint for logout
  logout() {
    // Logout typically might not return significant data beyond success/failure
    // handled by interceptors.
    return instance.post('/logout');
  }
};
