# 校园空间 HTTP / 服务合同

本文件是实现合同；所有接口位于 `/api/campus`，要求登录，成功使用 `ApiResponse` 的 `data`。ID 为数据库整数，前端比较时转字符串。列表统一 `{list,total,page,size}`（page 从 0 开始，size 1–100，窗口不超过 10000）；非法枚举/分页为400，无权限403，不存在404，状态/版本冲突409。内容是纯文本，前端禁止 v-html。

校园接口的创建、更新、审核、加入和审计时间返回带 `Z` 的 ISO-8601 UTC 时间点，保留毫秒；浏览器按本地时区显示。校园 DATETIME 由 SQL `CURRENT_TIMESTAMP` 写入，读取时先通过数据库 `UNIX_TIMESTAMP` 转为毫秒，避免 JDBC 配置和浏览器时区相异造成8小时偏差。数据库会话时区在写入和读取间须保持一致；不直接改变既有存储的时区约定。现有统一通知模块的时间合同保持原状。

## 学校和身份

| 方法与路径 | 请求 / 返回 data |
| --- | --- |
| GET /schools | q?, page?, size?, mine? → 学校分页；停用学校仅站点/该校管理员可见 |
| POST /schools | `{name,description,preModeration}` → School；仅站点 ADMIN，创建者成为初始校园 ADMIN |
| GET /schools/{schoolId} | School |
| PUT /schools/{schoolId} | `{name,description,preModeration,active,version}` → School；name/active 仅站点 ADMIN 可改，校园 ADMIN 可改简介/预审 |
| GET /schools/{schoolId}/applications/mine | 当前用户历史申请分页 |
| POST /schools/{schoolId}/applications | `{realName,studentNumber,department,enrollmentYear,statement}` → Application |
| DELETE /schools/{schoolId}/applications/{id} | 撤回本人 PENDING → `{success:true}` |
| GET /schools/{schoolId}/applications | status? 默认 PENDING → Application 分页；仅管理者 |
| PUT /schools/{schoolId}/applications/{id}/review | `{decision:'APPROVE'或'REJECT',reason,version}` → Application；禁止自审 |
| DELETE /schools/{schoolId}/membership | 本人退出 → `{success:true}`；最后一名有效校园 ADMIN 不能退出 |
| GET /schools/{schoolId}/members | q?,status? → Member 分页；仅管理者 |
| PUT /schools/{schoolId}/members/{userId} | `{status:'VERIFIED'或'SUSPENDED',role:'MEMBER'或'ADMIN',reason,version}` → Member；仅站点 ADMIN 可改角色，校园 ADMIN 不可修改自身或另一 ADMIN；被暂停后仅管理员恢复 |
| GET /schools/{schoolId}/audit | Audit 分页；仅管理者 |

School：`id,name,description,active,preModeration,version,createdAt,memberCount,membership:{status,role,version}|null,latestApplication:{id,status,reason,version}|null,capabilities:{canRead,canPost,canModerate,canManageMembers,canManageSchool,isSiteAdmin}`。canManageSchool 可管理停用学校；其余能力要求学校启用。认证是站内人工审核，没有对接学校教务系统。

Application：`id,schoolId,userId,username,realName,studentNumber,department,enrollmentYear,statement,status,reason,version,createdAt,reviewedAt`；仅申请人/管理者输出，不能包含密码、邮箱或其他用户字段。限制 name 80、studentNumber 40、department 100、statement 1000、reason 500 字符，年份 1900–当前年+1。状态 PENDING/APPROVED/REJECTED/CANCELLED。

学校停用后普通用户读取学校元数据为403，但本人的申请历史和撤回接口仍可用；校园页面在元数据被拒绝时显示仅本人的申请记录入口。收到当前403/404后清除已加载的受限数据和管理弹窗，并取消其余在途读取；图片上传/删除拒绝和图片读取401/403也会通知所在编辑器或详情页清除内容，迟到图片不能回填。409保留编辑输入，临时服务故障允许重试。

Member：`userId,username,nickname,avatar,status,role,version,joinedAt`；状态 VERIFIED/LEFT/SUSPENDED、角色 MEMBER/ADMIN。没有申请隐私信息。Audit：`id,actorId,action,targetType,targetId,details,createdAt`，details 只放处理原因/非敏感变更摘要。

## 动态、互动、管理

| 方法与路径 | 请求 / 返回 data |
| --- | --- |
| GET /schools/{schoolId}/posts | q?,category?,sort=latest或popular,scope=feed或mine或bookmarks,status?（仅mine）,page,size → Post 分页 |
| POST /schools/{schoolId}/posts | `{title,content,category,mediaIds:[],submit:boolean}` → Post |
| GET /posts/{postId} | Post |
| PUT /posts/{postId} | `{title,content,category,mediaIds:[],submit:boolean,version}` → Post |
| DELETE /posts/{postId}?version= | 作者或管理者删除；`{success:true}` |
| PUT /posts/{postId}/like | 幂等点赞 → `{likedByMe,likeCount}` |
| DELETE /posts/{postId}/like | 幂等取消 → `{likedByMe,likeCount}` |
| PUT /posts/{postId}/bookmark | 幂等收藏 → `{bookmarkedByMe}` |
| DELETE /posts/{postId}/bookmark | 幂等取消 → `{bookmarkedByMe}` |
| GET /posts/{postId}/comments | page,size → Comment 分页；按 id ASC |
| POST /posts/{postId}/comments | `{content,replyToCommentId:null或id}` → Comment |
| DELETE /posts/{postId}/comments/{commentId} | 作者/管理者删除 → `{success:true}` |
| POST /posts/{postId}/reports | `{reason}` → `{success:true}`；禁止重复待审举报 |
| GET /schools/{schoolId}/moderation/posts | status 默认 PENDING → Post 分页 |
| PUT /posts/{postId}/review | `{decision:'APPROVE'或'REJECT',reason,version}` → Post；不能自审 |
| PUT /posts/{postId}/pin | `{pinned:boolean,version}` → Post |
| GET /schools/{schoolId}/reports | status 默认 PENDING → Report 分页 |
| PUT /schools/{schoolId}/reports/{reportId} | `{decision:'REMOVE'或'DISMISS',reason,version}` → Report |

Post：`id,schoolId,schoolName,author:{id,username,nickname,avatar},title,content,category,status,version,pinned,reviewReason,images:[Media],likeCount,commentCount,likedByMe,bookmarkedByMe,canEdit,canDelete,canModerate,createdAt,updatedAt`。列表内容可完整返回但最多100项；正文1–10000、标题1–120；草稿也要求非空便于恢复。分类 GENERAL/STUDY/LIFE/LOST_FOUND/ANNOUNCEMENT，公告仅管理者。状态 DRAFT/PENDING/PUBLISHED/REJECTED/REMOVED。新建/编辑 submit=false 为 DRAFT；submit=true 且预审开为 PENDING，否则 PUBLISHED。所有作者（含管理员）遵循相同预审，自审禁止；可由另一管理者处理。编辑 PUBLISHED 立即撤下并进入新状态。REMOVED 不能编辑/恢复，普通读取404；管理员审计保留非正文处理信息。非 PUBLISHED 仅作者/管理者可读且仍须有效校园权限。除读草稿/管理外，点赞/收藏/评论/举报仅对 PUBLISHED 生效。审核只处理当前 PENDING 版本。

Comment：`id,postId,author:{id,username,nickname,avatar},content,replyToCommentId,deleted,canDelete,createdAt`，纯文本1–1000；删除保留无正文 tombstone，让回复关系完整；commentCount 只计未删除项。Report：`id,schoolId,postId,reporterId,reason,status,decisionReason,version,createdAt,reviewedAt`；状态 PENDING/REMOVED/DISMISSED。处理自己的举报或自己的动态禁止。移除结果实际更新动态状态/版本。

## 图片

被移除动态仅在本人的 mine 列表及管理队列保留状态/处理原因：标题、正文和图片清空，不提供详情跳转；任何 GET 详情均404。

POST `/schools/{schoolId}/media`：multipart `file` → Media。GET `/media/{uuid}`：鉴权 image/png，`Cache-Control:no-store`，必须通过当前账号的认证 HTTP blob 请求显示。DELETE `/media/{uuid}`：只允许本人删除未关联图片 → success。Media：`id,url,width,height,size`，url 为 `/api/campus/media/{uuid}`。每校每人最多12张未关联图片，24小时自动清理。发帖关联最多6张、仅本人的同校图片或该动态原图；解除关联重新进入24小时回收期。删除动态后图片不可读。禁止公开静态目录访问。

上传和重新编码结果均限制5MB，每校每人图片总额度100MB，剩余磁盘不足100MB时暂拒上传。删除动态24小时后回收其图片；清理每小时运行，按批次处理，并回收进程退出遗留超过24小时的文件。生产应将 CAMPUS_MEDIA_DIRECTORY 指向非静态的持久目录，并与数据库一同备份。

孤立文件扫描按文件名游标逐批推进，每批保留最多1000个候选、到末尾后循环，避免前1000个有效文件使后续孤立文件永久得不到清理。候选内存和每批文件/数据库检查有上限，目录名称遍历的成本仍随目录大小增长；本轮没有进行校园大规模存储容量认证。

## 通知

类型 CAMPUS_VERIFICATION（entityType=campus_school）、CAMPUS_MEMBERSHIP（campus_school）、CAMPUS_REVIEW、CAMPUS_LIKE、CAMPUS_COMMENT、CAMPUS_REPLY（后三类及审核均 campus_post）。只包含实体 ID 和现有 actor 标识，不含正文、姓名、学号。通知点击校园动态 `/campus/posts/{id}`、认证/成员 `/campus/schools/{id}`；列表/计数/推送重新检查动态权限。被移除内容不生成可点击正文通知，管理结论从对应管理页/我的发布状态读取。

afterCommit 只向有界后台执行器排队，随后在独立线程读取当前权限并推送，避免请求提交时仍持有连接又同步借第二个连接。执行器关闭/队列满或推送失败时不在请求线程重试；持久通知列表负责恢复。当前支持单应用实例。

## Java 共享接口（包 com.web.campus）

- CampusException(status:int,message:String)，由全局异常处理器返回对应 HTTP 状态。
- CampusPage<T>(List<T> list,long total,int page,int size)，静态 offset(page,size) 校验分页，静态 query(q) 校验并 trim；使用 SQL `LOCATE(LOWER(?),LOWER(column))` 实现字面检索。
- CampusAccessService（身份实现者）：Access record `(long schoolId,boolean active,boolean siteAdmin,boolean member,boolean manager,boolean preModeration)`；`inspect(long actor,long school)`、`lockSchool(long actor,long school)`、`requireRead(long actor,long school)`、`requireManage(long actor,long school)` 均返回 Access；`isSiteAdmin(long actor)`、`requireActiveUser(long actor)`、`requirePostRead(long actor,long post)`、`canReadPost(long actor,long post)`。school lock 要求活跃 Spring 事务，先 SELECT school FOR UPDATE，再查当前用户/成员权限；所有写入按 school→业务行顺序，避免旧成员权限写穿撤销。Access.manager 包括当前站点 ADMIN，inactive 只允许管理设置，不允许内容读写。
- CampusAuditService（协调者）：`record(long actor,long school,String action,String targetType,String targetId,String details)`，必须在调用者事务中。
- CampusNotificationService（协调者）：`send(long actor,long recipient,String type,String entityType,long entityId)`，必须在调用者事务中写库，afterCommit 推送并检查当前权限。
- CampusMediaService（协调者）：`replaceAttachments(long actor,long school,long post,List<String> ids)`；`List<Map<String,Object>> listForPost(long post)`；调用者持有 school lock，attachment 方法必须在活动事务中。
- 允许 JdbcTemplate 参数化 SQL 以保持本模块事务和权限查询清楚，禁止拼接用户输入。后台服务方法 @Transactional，测试应真实启用事务代理/TransactionTemplate。

## 前端路由

`/campus`、`/campus/schools/:schoolId`（同一 CampusPage），`/campus/schools/:schoolId/new`、`/campus/posts/:postId/edit`（CampusPostEditor），`/campus/posts/:postId`、`/campus/admin`、`/campus/schools/:schoolId/admin`。管理入口以服务端 capabilities 控制；/campus/admin 仅在当前用户 ADMIN 时显示，但后端总是独立校验。不要恢复无关的旧管理员路由模块。页面需要加载/失败/重试/无权限/空列表状态，保存冲突保留输入，切换账号/学校取消请求和 blob URL，所有 CSS 蓝色/中性，所有按钮必须连接真实接口。
