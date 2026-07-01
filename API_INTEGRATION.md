# 前后端 API 对接文档 - 用户注销功能

> **Base URL**: `http://localhost:7510`（本地测试）
> **最后更新**: 2026-07-01

---

## 目录

1. [用户管理接口](#1-用户管理接口)
2. [登录接口](#2-登录接口)
3. [已登录用户拦截](#3-已登录用户拦截)
4. [前端实现示例](#4-前端实现示例)
5. [测试流程](#5-测试流程)

---

## 1. 用户管理接口

> **权限**: 所有用户管理接口都需要 Admin 权限（由前端传递 `X-Admin-Key` 请求头）

### 1.1 获取用户列表

| 项目 | 内容 |
|------|--------|
| **URL** | `/api/admin/users` |
| **Method** | `GET` |
| **描述** | 获取所有用户列表（支持分页和搜索） |

#### 请求参数（Query String）

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `q` | String | 否 | 搜索关键词（按姓名/手机号搜索） |
| `page` | int | 否 | 页码，默认 `1` |
| `size` | int | 否 | 每页数量，默认 `20` |

#### 请求头

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `X-Admin-Key` | String | **是** | Admin 密钥（默认值：`admin-secret-key`） |

#### 请求示例

```bash
# 获取所有用户
curl http://localhost:7510/api/admin/users

# 分页获取
curl "http://localhost:7510/api/admin/users?page=1&size=10"

# 搜索用户
curl "http://localhost:7510/api/admin/users?q=13800138000"
```

#### 响应示例（200 OK）

```json
{
  "total": 3,
  "items": [
    {
      "id": "user001",
      "name": "张伟杰",
      "phone": "13800138000",
      "wechatOpenId": "openid_001",
      "status": "NORMAL"
    },
    {
      "id": "user002",
      "name": "李沐宸",
      "phone": "13800138001",
      "wechatOpenId": "openid_002",
      "status": "DEACTIVATED"
    }
  ]
}
```

---

### 1.2 注销用户

| 项目 | 内容 |
|------|--------|
| **URL** | `/api/admin/users/{userId}` |
| **Method** | `DELETE` |
| **描述** | 注销指定用户（状态改为 `DEACTIVATED`） |

#### 路径参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `userId` | String | **是** | 用户ID |

#### 请求头

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `X-Admin-Key` | String | **是** | Admin 密钥 |

#### 请求示例

```bash
curl -X DELETE \
  -H "X-Admin-Key: admin-secret-key" \
  http://localhost:7510/api/admin/users/user001
```

#### 响应示例

✅ **成功（200 OK）**
```json
{
  "success": true,
  "message": "User deactivated successfully"
}
```

❌ **失败（400 Bad Request）**
```json
{
  "success": false,
  "message": "Failed to deactivate user. User may not exist or already be deactivated."
}
```

---

## 2. 登录接口

### 2.1 用户登录

| 项目 | 内容 |
|------|--------|
| **URL** | `/api/v1/portal/login` |
| **Method** | `POST` |
| **描述** | 用户登录（支持手机号+验证码、微信登录） |

#### 请求体（JSON）

**手机号登录**
```json
{
  "loginType": "smsLogin",
  "params": {
    "phone_number": "13800138000",
    "sms": "123456"
  }
}
```

**微信登录**
```json
{
  "loginType": "wechatLogin",
  "params": {
    "wechat_openid": "openid_001"
  }
}
```

#### 响应示例

✅ **登录成功（200 OK）**
```json
{
  "success": true,
  "message": "Login Successful."
}
```

❌ **登录失败 - 账户已注销（200 OK，但 success=false）**
```json
{
  "success": false,
  "message": "Your account has been deactivated"
}
```

❌ **登录失败 - 验证码错误（400 Bad Request）**
```json
{
  "success": false,
  "message": "Wrong smsCode."
}
```

---

## 3. 已登录用户拦截

### 3.1 后端拦截器

后端已添加全局拦截器：
- **拦截所有** `/api/**` 请求（除了登录接口）
- 从请求头 `X-User-Id` 中获取当前用户ID
- 如果用户状态为 `DEACTIVATED`，返回 `403` + 错误码 `USER_DEACTIVATED`

#### 响应格式（403 Forbidden）

```json
{
  "success": false,
  "message": "Your account has been deactivated",
  "code": "USER_DEACTIVATED"
}
```

---

### 3.2 前端处理方案

#### 方案A：全局 fetch 拦截器（推荐）

在**前端页面**中添加以下代码：

```html
<script>
  // ================================
  //  全局 fetch 拦截器
  // ================================
  
  const originalFetch = window.fetch;
  
  window.fetch = function(url, options = {}) {
    // 1. 从 localStorage 获取当前用户ID（登录后保存的）
    const currentUserId = localStorage.getItem('currentUserId');
    
    // 2. 添加 X-User-Id Header
    if (!options.headers) {
      options.headers = {};
    }
    if (currentUserId && !options.headers['X-User-Id']) {
      options.headers['X-User-Id'] = currentUserId;
    }
    
    // 3. 发起请求
    return originalFetch(url, options)
      .then(response => {
        // 4. 检查响应状态
        if (response.status === 403) {
          return response.json().then(data => {
            // 5. 检查是否是用户被注销的错误
            if (data && data.code === 'USER_DEACTIVATED') {
              // 6. 弹出提示框
              showDeactivatedAlert();
              throw new Error('USER_DEACTIVATED');
            }
            return response;
          });
        }
        return response;
      });
  };

  // ================================
  //  弹出注销提示框并跳转登录页
  // ================================
  function showDeactivatedAlert() {
    if (confirm('您的账户已被管理员注销，即将退出登录。\n点击"确定"返回登录页面。')) {
      // 清空登录状态
      localStorage.removeItem('currentUserId');
      localStorage.removeItem('userInfo');
      // 跳转登录页
      window.location.href = '/login.html';
    }
  }

  // ================================
  //  登录成功后，保存 userId
  // ================================
  function onLoginSuccess(userId) {
    localStorage.setItem('currentUserId', userId);
  }
</script>
```

---

#### 方案B：使用 axios 拦截器（如果前端用 axios）

```javascript
// 响应拦截器
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 403) {
      const data = error.response.data;
      if (data && data.code === 'USER_DEACTIVATED') {
        // 弹出提示框
        if (confirm('您的账户已被管理员注销，即将退出登录。')) {
          // 清除登录信息
          localStorage.removeItem('currentUserId');
          localStorage.removeItem('userInfo');
          // 跳转到登录页面
          window.location.href = '/login.html';
        }
      }
    }
    return Promise.reject(error);
  }
);
```

---

## 4. 前端实现示例

### 4.1 管理员用户管理页面

#### 功能需求
1. **显示用户列表**: 调用 `GET /api/admin/users` 获取用户列表（需要添加 `X-Admin-Key` 请求头）
2. **显示用户状态**: 根据 `status` 字段显示不同颜色/文字
   - `NORMAL` → 绿色："正常"
   - `FROZEN` → 黄色："已冻结"
   - `DEACTIVATED` → 红色："已注销"
3. **注销按钮**: 只对 `NORMAL` 和 `FROZEN` 状态的用户显示"注销"按钮

#### 注销操作流程
1. 管理员点击"注销"按钮
2. **前端弹出二次确认弹窗**:
   ```
   确认注销
   确定要注销该用户吗？注销后该用户将无法登录。
   [取消]  [确定]
   ```
3. 管理员点击"确定"
4. 前端调用 `DELETE /api/admin/users/{userId}`（需要添加 `X-Admin-Key` 请求头）
5. 根据响应显示成功/失败消息

---

### 4.2 登录页面 - 账户已注销提示

#### 场景1: 用户尝试登录（手机号或微信）
- 后端返回: `{"success": false, "message": "Your account has been deactivated"}`
- **前端处理**:
  - 在登录页面显示错误消息："您的账户已注销"
  - 不建议使用 alert，建议使用页面内提示元素

#### 示例（伪代码）
```javascript
async function handleLogin(loginData) {
  const response = await fetch('/api/v1/portal/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(loginData)
  });
  
  const result = await response.json();
  
  if (result.success) {
    // 登录成功，跳转到首页
    router.push('/home');
  } else {
    // 登录失败
    if (result.message === "Your account has been deactivated") {
      // 显示中文提示
      showErrorMessage("您的账户已注销");
    } else {
      showErrorMessage(result.message);
    }
  }
}
```

---

### 4.3 已登录用户被注销后的处理

#### 场景
用户在浏览器保持登录状态时，管理员在后台注销了该用户的账户。

#### 后端行为
- 用户后续的所有 API 请求都会返回 **403 Forbidden**
- 响应体: `{"success": false, "message": "Your account has been deactivated", "code": "USER_DEACTIVATED"}`

#### 前端处理
1. **全局 HTTP 拦截器**: 捕获所有 403 响应
2. **检测特定消息**: 如果 `message` 包含 "deactivated" 或 `code` === "USER_DEACTIVATED"
3. **弹出提示弹窗**:
   ```
   账户已注销
   您的账户已被管理员注销，即将退出登录。
   [确定]
   ```
4. **点击确定后**:
   - 清除本地存储的登录信息（token、userInfo 等）
   - 跳转到登录页面

#### 示例（Vue.js）
```javascript
// axios 响应拦截器
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response.status === 403) {
      const data = error.response.data;
      if (data.code === "USER_DEACTIVATED") {
        // 弹出提示
        ElMessageBox.alert('您的账户已被管理员注销，即将退出登录。', '提示', {
          confirmButtonText: '确定',
          callback: () => {
            // 清除登录信息
            store.dispatch('logout');
            router.push('/login');
          }
        });
      }
    }
    return Promise.reject(error);
  }
);
```

---

## 5. 测试流程

### 5.1 本地测试环境搭建

```bash
# 1. 启动 Redis
redis-server --daemonize yes

# 2. 配置环境变量（可选）
export ADMIN_SECRET_KEY="test-admin-key-123"
export REDIS_HOST=localhost

# 3. 启动 Spring Boot 应用
cd /Users/macbook/CodeBuddy/QZT
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

### 5.2 添加测试数据

```bash
# 添加正常用户
redis-cli HMSET user:user001 phone "13800138000" wechatOpenId "openid_001" status "NORMAL" name "Test User 1"
redis-cli SET user:phone:13800138000 "user001"
redis-cli SET user:wechat:openid_001 "user001"

# 添加另一个用户
redis-cli HMSET user:user002 phone "13800138001" wechatOpenId "openid_002" status "NORMAL" name "Test User 2"
redis-cli SET user:phone:13800138001 "user002"
redis-cli SET user:wechat:openid_002 "user002"
```

---

### 5.3 测试 API 接口

#### 测试1: 查看用户列表
```bash
curl http://localhost:7510/api/admin/users
```

**预期结果**:
```json
{
  "total": 2,
  "items": [
    {"id":"user002", "name":"Test User 2", "phone":"13800138001", "status":"NORMAL", ...},
    {"id":"user001", "name":"Test User 1", "phone":"13800138000", "status":"NORMAL", ...}
  ]
}
```

---

#### 测试2: 注销用户
```bash
curl -X DELETE http://localhost:7510/api/admin/users/user001
```

**预期结果**:
```json
{"success": true, "message": "User deactivated successfully"}
```

---

#### 测试3: 验证用户状态已更改
```bash
curl http://localhost:7510/api/admin/users
```

**预期结果**: user001 的 `status` 变为 `DEACTIVATED`

---

#### 测试4: 注销用户尝试手机号登录
```bash
# 先存储验证码
redis-cli SET 13800138000 "123456"

# 尝试登录
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"loginType":"smsLogin", "params":{"phone_number":"13800138000", "sms":"123456"}}' \
  http://localhost:7510/api/v1/portal/login
```

**预期结果**:
```json
{"success": false, "message": "Your account has been deactivated"}
```

---

#### 测试5: 注销用户尝试微信登录
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"loginType":"wechatLogin", "params":{"wechat_openid":"openid_001"}}' \
  http://localhost:7510/api/v1/portal/login
```

**预期结果**:
```json
{"success": false, "message": "Your account has been deactivated"}
```

---

#### 测试6: 正常用户登录
```bash
# 存储验证码
redis-cli SET 13800138001 "654321"

# 尝试登录
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"loginType":"smsLogin", "params":{"phone_number":"13800138001", "sms":"654321"}}' \
  http://localhost:7510/api/v1/portal/login
```

**预期结果**:
```json
{"success": true, "message": "Login Successful."}
```

---

### 5.4 前端对接测试

1. 打开前端页面 `index.html`
2. 页面加载时自动调用 `GET /api/admin/users` 获取用户列表
3. 点击"注销"按钮，弹出确认弹窗
4. 勾选确认框，点击"确认注销"
5. 前端调用 `DELETE /api/admin/users/{userId}`
6. 刷新用户列表，验证状态已更新

---

## 6. 部署说明

### 6.1 环境变量配置

在生产环境中，必须配置以下环境变量：

| 环境变量 | 说明 | 默认值 |
|------------|------|---------|
| `REDIS_HOST` | Redis 服务器地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASSWORD` | Redis 密码 | 空 |
| `DATABASE_URL` | 数据库 URL | `jdbc:mysql://localhost:3306/` |
| `DATABASE_USERNAME` | 数据库用户名 | `root` |
| `DATABASE_PASSWORD` | 数据库密码 | 空 |
| `ADMIN_SECRET_KEY` | Admin 密钥 | `admin-secret-key` |

---

### 6.2 部署步骤

1. 配置环境变量（参考 `.env.example` 文件）
2. 确保 Redis 服务已启动
3. 确保数据库服务已启动
4. 启动 Spring Boot 应用：
   ```bash
   java -jar Qintelipass-0.0.1-SNAPSHOT.jar
   ```

---

## 7. 注意事项

1. **二次确认弹窗**: 必须在前端实现，后端接口是幂等的（多次调用不会出错）
2. **错误消息国际化**: 后端返回英文消息，前端需转换为中文显示
3. **已登录用户的实时检测**: 建议使用 WebSocket 或轮询机制，实时检测用户状态变化
4. **测试环境**: 使用本地 Redis + H2 数据库，确保 Redis 服务已启动
5. **⚠️ Admin 权限验证**: 
   - 查看用户列表和注销用户接口需要 Admin 权限
   - 必须在请求头中添加 `X-Admin-Key`
   - 如果返回 403 Forbidden，检查 Admin Key 是否正确
   - 本地测试默认 Admin Key: `admin-secret-key`
6. **⚠️ 已登录用户拦截**:
   - 前端必须在每次请求时传递 `X-User-Id` Header
   - 前端必须实现全局拦截器，处理 `USER_DEACTIVATED` 错误码

---

## 8. 联系方式

如有接口对接问题，请联系后端开发人员。

**最后更新**: 2026-07-01（添加已登录用户拦截说明）
