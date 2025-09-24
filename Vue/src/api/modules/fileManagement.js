import axiosInstance from '../axiosInstance';

const fileManagementApi = {
  // 上传文件
  uploadFile(file, isPublic = false) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('isPublic', isPublic);
    
    return axiosInstance.post('/api/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },

  // 删除文件
  deleteFile(fileId) {
    return axiosInstance.delete(`/api/files/${fileId}`);
  },

  // 获取用户文件列表
  getUserFiles(page = 1, size = 10) {
    return axiosInstance.get('/api/files/my', {
      params: { page, size }
    });
  },

  // 获取文件详情
  getFileDetails(fileId) {
    return axiosInstance.get(`/api/files/${fileId}`);
  },

  // 生成文件下载URL
  getDownloadUrl(fileId) {
    return `/api/files/download/${fileId}`;
  }
};

export default fileManagementApi;