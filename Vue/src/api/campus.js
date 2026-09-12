import http from './axiosInstance'

const root = '/api/campus'
const school = id => `${root}/schools/${encodeURIComponent(id)}`
const post = id => `${root}/posts/${encodeURIComponent(id)}`
const request = (method, url, data, signal, params) => http({ method, url, data, signal, params })
const get = (url, params, signal) => request('get', url, undefined, signal, params)

export default {
  schools: (params, signal) => get(`${root}/schools`, params, signal),
  school: (id, signal) => get(school(id), undefined, signal),
  createSchool: (data, signal) => request('post', `${root}/schools`, data, signal),
  updateSchool: (id, data, signal) => request('put', school(id), data, signal),
  applications: (id, params, signal, mine = false) => get(`${school(id)}/applications${mine ? '/mine' : ''}`, params, signal),
  apply: (id, data, signal) => request('post', `${school(id)}/applications`, data, signal),
  cancelApplication: (id, application, signal) => request('delete', `${school(id)}/applications/${application}`, undefined, signal),
  reviewApplication: (id, application, data, signal) => request('put', `${school(id)}/applications/${application}/review`, data, signal),
  leave: (id, signal) => request('delete', `${school(id)}/membership`, undefined, signal),
  members: (id, params, signal) => get(`${school(id)}/members`, params, signal),
  updateMember: (id, user, data, signal) => request('put', `${school(id)}/members/${user}`, data, signal),
  audit: (id, params, signal) => get(`${school(id)}/audit`, params, signal),
  posts: (id, params, signal) => get(`${school(id)}/posts`, params, signal),
  post: (id, signal) => get(post(id), undefined, signal),
  createPost: (id, data, signal) => request('post', `${school(id)}/posts`, data, signal),
  updatePost: (id, data, signal) => request('put', post(id), data, signal),
  deletePost: (id, version, signal) => request('delete', post(id), undefined, signal, { version }),
  like: (id, desired, signal) => request(desired ? 'put' : 'delete', `${post(id)}/like`, undefined, signal),
  bookmark: (id, desired, signal) => request(desired ? 'put' : 'delete', `${post(id)}/bookmark`, undefined, signal),
  comments: (id, params, signal) => get(`${post(id)}/comments`, params, signal),
  comment: (id, data, signal) => request('post', `${post(id)}/comments`, data, signal),
  deleteComment: (id, comment, signal) => request('delete', `${post(id)}/comments/${comment}`, undefined, signal),
  report: (id, reason, signal) => request('post', `${post(id)}/reports`, { reason }, signal),
  moderation: (id, params, signal) => get(`${school(id)}/moderation/posts`, params, signal),
  reviewPost: (id, data, signal) => request('put', `${post(id)}/review`, data, signal),
  pin: (id, data, signal) => request('put', `${post(id)}/pin`, data, signal),
  reports: (id, params, signal) => get(`${school(id)}/reports`, params, signal),
  resolveReport: (id, report, data, signal) => request('put', `${school(id)}/reports/${report}`, data, signal),
  upload: (id, file, signal) => {
    const data = new FormData()
    data.append('file', file)
    return request('post', `${school(id)}/media`, data, signal)
  },
  media: (id, signal) => http({ url: `${root}/media/${encodeURIComponent(id)}`, responseType: 'blob', signal }),
  deleteMedia: (id, signal) => request('delete', `${root}/media/${encodeURIComponent(id)}`, undefined, signal),
}
