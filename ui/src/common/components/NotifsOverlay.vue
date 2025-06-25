<template>
  <div class="os-notifs" v-os-tooltip.bottom="$t('common.notifs.title')">
    <button @click="toggleOverlay">
      <os-icon name="bell" v-os-badge.danger="unreadNotifCount" v-if="unreadNotifCount > 0" />
      <os-icon name="bell" v-else />
    </button>

    <os-overlay class="os-notifs-overlay" ref="overlay"
      @click="toggleOverlay" @show="notifsDisplayed" @hide="notifsHidden">
      <div class="content">
        <div class="os-notifs-list">
          <div class="title">
            <span v-t="'common.notifs.title'">Notifications</span>
          </div>
          <div class="info" v-if="loading || (notifs && notifs.length == 0)">
            <span v-if="loading">
              <span v-t="'common.notifs.loading'">Loading notifications. Please wait for a moment...</span>
            </span>
            <span v-else-if="notifs && notifs.length == 0">
              <span v-t="'common.notifs.no_notifs'">No notifications to show.</span>
            </span>
          </div>

          <os-notif-items :notifs="notifs" />

          <div class="footer" v-if="!loading && notifs && notifs.length > 0">
            <router-link :to="{name: 'UserNotificationsList'}">
              <span v-t="'common.notifs.view_all'">View All Notifications</span>
            </router-link>
          </div>
        </div>
      </div>
    </os-overlay>
  </div>
</template>

<script>

import notifSvc from '@/common/services/Notif.js';
import ui from '@/global.js';

import NotifItems from '@/common/components/NotifItems.vue';

export default {
  components: {
    'os-notif-items': NotifItems
  },

  data() {
    return {
      notifs: null,

      unreadNotifCount: 0
    }
  },

  mounted() {
    let self = this;
    function getUnreadNotifCount() {
      if (self.timeout) {
        clearTimeout(self.timeout);
      }

      if (!ui.currentUser) {
        return;
      }

      if (self.notifsOpen) {
        self.timeout = setTimeout(getUnreadNotifCount, 10000);
        return;
      }

      notifSvc.getUnreadCount().then(
        (result) => {
          self.unreadNotifCount = result.count;
          self.timeout = setTimeout(getUnreadNotifCount, 10000);
          if (result.count > 0) {
            self.notifs = null;
          }
        }
      );
    }

    getUnreadNotifCount();
  },

  unmounted() {
    if (this.timeout) {
      clearTimeout(this.timeout);
    }
  },

  methods: {
    toggleOverlay: function(event) {
      this.$refs.overlay.toggle(event);
    },

    notifsDisplayed: function() {
      this.notifsOpen = true;
      if (this.unreadNotifCount > 0) {
        this.notifOpenTime = new Date();
      }

      if (!this.notifs) {
        notifSvc.getNotifications(0, 10).then(
          notifs => {
            this.loading = false;
            this.notifs = notifs;
          }
        );
      }
    },

    notifsHidden: function() {
      this.notifsOpen = false;
      if (this.notifOpenTime) {
        notifSvc.markAsRead(this.notifOpenTime).then(() => this.unreadNotifCount = 0);
        this.notifOpenTime = null;
      }
    }
  }
}
</script>

<style>
.os-notifs .p-badge {
  top: 0.3rem;
}

.os-notifs-overlay {
  max-height: calc(100% - 100px);
  width: 35%;
  overflow: auto;
}

.os-notifs-list {
  padding: 0rem;
  margin: 0rem;
}

.os-notifs-list .info {
  padding: 0.5rem 0rem;
}

.os-notifs-list .title {
  font-weight: bold;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #e6e6e6;
}

.os-notifs-list .footer {
  padding-top: 0.5rem;
  margin-top: -1px;
  border-top: 1px solid #e6e6e6;
  font-weight: bold;
}
</style>
