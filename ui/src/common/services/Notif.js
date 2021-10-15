
import http from '@/common/services/HttpClient.js';

class Notifications {
  async getNotifications(startAt, maxNotifs) {
    return http.get('user-notifications', {startAt: startAt, maxResults: maxNotifs});
  }

  async getUnreadCount() {
    return http.get('user-notifications/unread-count');
  }

  async markAsRead(time) {
    return http.put('user-notifications/mark-as-read', {notifsBefore: time});
  }
}

export default new Notifications();
