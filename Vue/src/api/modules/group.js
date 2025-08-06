// File path: /Vue/src/api/modules/group.js
import { instance } from '../axiosInstance'; // Corrected import

export default {
  // getUserJoinedGroups was used in Groups.vue, maps to /group/my-list
  getUserJoinedGroups() {
    return instance.get('/group/my-list');
  },

  // createGroup was used in Groups.vue. Backend GroupController @PostMapping("/create")
  // The user's api/index.js example for createGroup was instance.post('/group/create', data)
  // However, GroupController's @RequestMapping is /api/group, and create is @PostMapping("/create")
  // So, /api/group/create is correct. The instance's baseURL is /api. So path should be /group/create
  createGroup(data) {
    return instance.post('/group/create', data);
  },

  // getGroupDetails was used in GroupDetail.vue
  // Backend: GroupController needs a @GetMapping("/{groupId}")
  // Assuming GroupController has @GetMapping("/{groupId}") for this.
  // If GroupController has /api/group base, then this is /api/group/{groupId}
  getGroupDetails(groupId) {
    return instance.get(`/group/${groupId}`);
  },

  // getGroupMembers was used in GroupDetail.vue
  // Backend: GroupController needs a @GetMapping("/members/{groupId}") or similar
  // Assuming /api/group/members/{groupId}
  getGroupMembers(groupId) {
    return instance.get(`/group/members/${groupId}`);
  },

  // updateGroup was used in GroupDetail.vue
  // Backend: GroupController needs a @PutMapping or @PostMapping for update.
  // User example: instance.put('/group', groupData); implies GroupController @PutMapping("/")
  // This expects groupData to contain the groupId for identification by the backend.
  updateGroup(groupData) {
    return instance.put('/group', groupData); // Assumes backend handles PUT to /api/group for update
  },

  // inviteMembers was used in GroupDetail.vue
  // Backend: GroupController @PostMapping("/invite")
  // Path: /api/group/invite
  inviteMembers(payload) { // payload: { groupId: string, userIds: string[] }
    return instance.post('/group/invite', payload);
  },

  // kickMember was used in GroupDetail.vue
  // Backend: GroupController @PostMapping("/kick")
  // Path: /api/group/kick
  kickMember(payload) { // payload: { groupId: string, userIdToKick: string }
    return instance.post('/group/kick', payload);
  },

  // leaveGroup was used in GroupDetail.vue
  // Backend: GroupController @PostMapping("/leave/{groupId}")
  // The user's code uses DELETE /api/group/quit/{groupId}
  // Let's stick to the user's API design for leaveGroup.
  // GroupController has @PostMapping("/leave/{groupId}")
  // If user's API module is the source of truth for frontend, then backend might need adjustment or this is an alias.
  // For now, using user's definition: DELETE /api/group/quit/{groupId}
  // This implies GroupController should have a @DeleteMapping("/quit/{groupId}")
  leaveGroup(groupId) {
    return instance.delete(`/group/quit/${groupId}`);
  },

  // disbandGroup was used in GroupDetail.vue
  // User example: instance.delete('/group/{groupId}');
  // This implies GroupController should have a @DeleteMapping("/{groupId}")
  disbandGroup(groupId) {
    return instance.delete(`/group/${groupId}`);
  }

  // searchGroups is handled by search.js module as per user's overall api/index.js structure.
};
