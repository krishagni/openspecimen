<template>
  <os-page>
    <os-page-head>
      <h3 v-t="'common.notifs.title'">Notifications</h3>
    </os-page-head>

    <os-page-body>
      <os-grid>
        <os-grid-column class="notifs-container" :width="12">
          <div class="notifs">
            <os-message type="info" v-if="ctx.loading">
              <span v-t="'common.notifs.loading'">Loading notifications ...</span>
            </os-message>
            <os-notif-items :notifs="ctx.notifs" v-else-if="ctx.notifs && ctx.notifs.length > 0" />
            <os-message type="info" v-else>
              <span v-t="'common.notifs.no_notifs'">No notifications to show.</span>
            </os-message>
          </div>

          <div class="pager">
            <os-pager :start-at="ctx.startAt" :have-more="ctx.haveMoreNotifs"
              @previous="previousPage" @next="nextPage" />
          </div>
        </os-grid-column>
      </os-grid>
    </os-page-body>
  </os-page>
</template>

<script>
import notifSvc from '@/common/services/Notif.js';
import routerSvc from '@/common/services/Router.js';

import NotifItems from '@/common/components/NotifItems.vue';

const MAX_NOTIFS = 25;

export default {
  props: ['pageNo'],

  components: {
    'os-notif-items': NotifItems
  },

  data() {
    return {
      ctx: {
        notifs: [],

        loading: true,

        startAt: 0,

        haveMoreNotifs: true
      }
    };
  },

  mounted() {
    const pageNo = +this.pageNo >= 0 ? +this.pageNo : 0;
    this.loadNotifs(pageNo);
  },

  watch: {
    pageNo: function(newVal, oldVal) {
      if (newVal == oldVal) {
        return;
      }

      const pageNo = +this.pageNo >= 0 ? +this.pageNo : 0;
      this.loadNotifs(pageNo);
    }
  },

  methods: {
    loadNotifs: async function(pageNo) {
      const startAt = this.ctx.startAt = pageNo * MAX_NOTIFS;

      this.ctx.loading = true;
      const notifs = await notifSvc.getNotifications(startAt, MAX_NOTIFS + 1);
      if (notifs.length > MAX_NOTIFS) {
        this.ctx.haveMoreNotifs = true;
        notifs.splice(notifs.length - 1, 1);
      } else {
        this.ctx.haveMoreNotifs = false;
      }

      this.ctx.notifs = notifs;
      this.ctx.loading = false;
    },

    previousPage: function() {
      routerSvc.goto('UserNotificationsList', {}, {pageNo: +this.pageNo - 1})
    },

    nextPage: function() {
      const pageNo = +this.pageNo >= 0 ? +this.pageNo : 0;
      routerSvc.goto('UserNotificationsList', {}, {pageNo: pageNo + 1})
    }
  }
}
</script>

<style scoped>
.notifs-container {
  display: flex;
  flex-direction: column;
}

.notifs-container .notifs {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 1.25rem;
}
</style>
