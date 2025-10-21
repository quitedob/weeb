# API Documentation

This document provides a comprehensive overview of all available API endpoints in the Weeb application.

## Authentication Endpoints

### POST `/api/login`
- **Description**: User login
- **Request Body**: `LoginVo`
  ```json
  {
    "username": "string",
    "password": "string",
    "rememberMe": "boolean"
  }
  ```
- **Response**: `ApiResponse<LoginResultVo>`
  ```json
  {
    "code": 0,
    "message": "success",
    "data": {
      "token": "jwt_token_string",
      "userInfo": {
        "id": 1,
        "username": "string",
        "nickname": "string",
        "avatar": "string"
      }
    }
  }
  ```

### POST `/api/register`
- **Description**: User registration
- **Request Body**: `RegisterVo`
- **Response**: `ApiResponse<Object>`

### POST `/api/logout`
- **Description**: User logout
- **Response**: `ApiResponse<Object>`

### GET `/api/user/info`
- **Description**: Get current user information
- **Response**: `ApiResponse<User>`

### PUT `/api/user/info`
- **Description**: Update user information
- **Request Body**: `UpdateUserVo`
- **Response**: `ApiResponse<Object>`

## User Management Endpoints

### GET `/api/user/profile/{userId}`
- **Description**: Get user profile with statistics
- **Response**: `ApiResponse<UserWithStats>`

### GET `/api/user/list`
- **Description**: Get paginated user list
- **Query Parameters**:
  - `page`: int (default: 1)
  - `pageSize`: int (default: 20)
  - `keyword`: string (optional)
- **Response**: `ApiResponse<PageResult<UserWithStats>>`

### POST `/api/user/{userId}/ban`
- **Description**: Ban user
- **Response**: `ApiResponse<Object>`

### POST `/api/user/{userId}/unban`
- **Description**: Unban user
- **Response**: `ApiResponse<Object>`

### POST `/api/user/{userId}/reset-password`
- **Description**: Reset user password (admin only)
- **Request Body**: `ResetPasswordVo`
- **Response**: `ApiResponse<Object>`

## Chat & Messaging Endpoints

### GET `/api/message/record`
- **Description**: Get chat message history
- **Query Parameters**:
  - `targetId`: long (chat ID)
  - `index`: int (offset)
  - `num`: int (limit)
- **Response**: `ApiResponse<List<Message>>`

### POST `/api/message/send`
- **Description**: Send message
- **Request Body**: `SendMessageVo`
- **Response**: `ApiResponse<Object>`

### GET `/api/chat/sessions`
- **Description**: Get recent chat sessions
- **Response**: `ApiResponse<List<ChatSession>>`

## Article Endpoints

### GET `/api/article/list`
- **Description**: Get paginated articles
- **Query Parameters**:
  - `page`: int (default: 1)
  - `pageSize`: int (default: 20)
  - `category`: string (optional)
  - `keyword`: string (optional)
- **Response**: `ApiResponse<PageResult<Article>>`

### GET `/api/article/{articleId}`
- **Description**: Get article details
- **Response**: `ApiResponse<Article>`

### POST `/api/article/create`
- **Description**: Create new article
- **Request Body**: `CreateArticleVo`
- **Response**: `ApiResponse<Object>`

### PUT `/api/article/{articleId}`
- **Description**: Update article
- **Request Body**: `UpdateArticleVo`
- **Response**: `ApiResponse<Object>`

### DELETE `/api/article/{articleId}`
- **Description**: Delete article
- **Response**: `ApiResponse<Object>`

## Group Endpoints

### GET `/api/group/list`
- **Description**: Get group list
- **Response**: `ApiResponse<List<Group>>`

### GET `/api/group/{groupId}`
- **Description**: Get group details
- **Response**: `ApiResponse<Group>`

### POST `/api/group/create`
- **Description**: Create new group
- **Request Body**: `CreateGroupVo`
- **Response**: `ApiResponse<Object>`

### POST `/api/group/{groupId}/join`
- **Description**: Join group
- **Response**: `ApiResponse<Object>`

## Search Endpoints

### GET `/api/search`
- **Description**: Global search
- **Query Parameters**:
  - `q`: string (search query)
  - `type`: string (optional: "user", "group", "article")
- **Response**: `ApiResponse<SearchResult>`

## Notification Endpoints

### GET `/api/notification/list`
- **Description**: Get user notifications
- **Query Parameters**:
  - `page`: int (default: 1)
  - `pageSize`: int (default: 20)
- **Response**: `ApiResponse<PageResult<Notification>>`

### POST `/api/notification/{notificationId}/read`
- **Description**: Mark notification as read
- **Response**: `ApiResponse<Object>`

### POST `/api/notification/read-all`
- **Description**: Mark all notifications as read
- **Response**: `ApiResponse<Object>`

## System Endpoints

### GET `/api/admin/stats`
- **Description**: Get system statistics (admin only)
- **Response**: `ApiResponse<SystemStats>`

### GET `/api/admin/users`
- **Description**: Get user management data (admin only)
- **Query Parameters**:
  - `page`: int (default: 1)
  - `pageSize`: int (default: 20)
  - `keyword`: string (optional)
- **Response**: `ApiResponse<PageResult<UserWithStats>>`

## Error Handling

### Standard Response Format
All API responses follow the `ApiResponse<T>` format:
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### Common Error Codes
- `0`: Success
- `-1`: System error
- `1001`: Invalid parameters
- `1002`: Authentication failed
- `1003`: Permission denied
- `1004`: Resource not found
- `1005`: Resource already exists
- `1006`: Operation failed

### HTTP Status Codes
- `200`: Success
- `400`: Bad request
- `401`: Unauthorized
- `403`: Forbidden
- `404`: Not found
- `429`: Too many requests
- `500`: Internal server error

## Rate Limiting
- Authentication endpoints: 10 requests per minute
- General endpoints: 100 requests per minute
- File upload endpoints: 5 requests per minute

## WebSocket Endpoints

### WebSocket Connection
- **URL**: `ws://localhost:8081/ws`
- **Authentication**: Send auth message with JWT token

### Message Types
- `auth`: Authentication
- `chat_message`: Chat message
- `heartbeat`: Keep-alive
- `status_change`: User online status
- `typing`: Typing indicator
- `message_sent`: Message sent confirmation
- `message_delivered`: Message delivered confirmation
- `message_read`: Message read confirmation

## File Upload

### POST `/api/upload`
- **Description**: Upload file
- **Content-Type**: `multipart/form-data`
- **Request Body**: File data
- **Response**: `ApiResponse<UploadResult>`
  ```json
  {
    "code": 0,
    "message": "success",
    "data": {
      "url": "string",
      "filename": "string",
      "size": "number"
    }
  }
  ```

## Testing

### Development Environment
- **Base URL**: `http://localhost:8080`
- **WebSocket URL**: `ws://localhost:8081/ws`

### Production Environment
- **Base URL**: Set via `VITE_API_BASE_URL` environment variable
- **WebSocket URL**: Derived from base URL

## Security
- All authenticated endpoints require JWT token in Authorization header
- Token expiration: 24 hours (configurable)
- Password requirements: Minimum 8 characters with mixed case and numbers
- File upload restrictions: Maximum 10MB per file, allowed types: images, documents