
<template>
  <div class="os-notifs" v-os-tooltip.bottom="$t('common.notifs.title')">
    <button @click="toggleOverlay">
      <os-icon name="bell" v-os-badge.danger="unreadNotifCount" v-if="unreadNotifCount > 0" />
      <os-icon name="bell" v-else />
    </button>

    <os-overlay class="os-notifs-overlay" ref="overlay"
      @click="toggleOverlay" @show="notifsDisplayed" @hide="notifsHidden">
      <div class="content">
        <os-user-notifs />
      </div>
    </os-overlay>
  </div>
</template>

<script>

import NotificationsList from '@/common/components/NotificationsList';

import notifSvc from '@/common/services/Notif.js';
import ui from '@/global.js';

export default {
  components: {
    'os-user-notifs': NotificationsList
  },

  data() {
    return {
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
</style>
