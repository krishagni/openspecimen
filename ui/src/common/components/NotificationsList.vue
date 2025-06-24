<template>
  <ul class="os-notifs-list">
    <li class="title">
      <span v-t="'common.notifs.title'">Notifications</span>
    </li>
    <li class="info" v-if="loading || notifs.length == 0">
      <span v-if="loading">
        <span v-t="'common.notifs.loading'">Loading notifications. Please wait for a moment...</span>
      </span>
      <span v-else-if="notifs.length == 0">
        <span v-t="'common.notifs.no_notifs'">No notifications to show.</span>
      </span>
    </li>
    <li class="notif" v-for="(notif, idx) of notifs" :key="idx" @click="visitNotif(notif)">
      <div class="notif-unread" v-if="notif.status == 'UNREAD'"> </div>
      <div class="notif-type">
        <os-icon name="plus" v-if="notif.operation == 'CREATE'" />
        <os-icon name="edit" v-else-if="notif.operation == 'UPDATE'" />
        <os-icon name="trash" v-else-if="notif.operation == 'DELETE'" />
        <os-icon name="download" v-else-if="notif.operation == 'EXPORT'" />
        <os-icon name="exclamation" v-else-if="notif.operation == 'ALERT'" />
        <os-avatar :label="notif.operation.charAt(0)" v-else />
      </div>
      <div class="notif-content">
        <div class="notif-message">
          <a :href="notif.href" :target="notif.newTab ? '_blank' : '_self'" @click="stopPropagation">
            <span>{{notif.message}}</span>
          </a>
        </div>
        <div class="notif-meta">
          <span>{{$filters.username(notif.createdBy)}}</span>
          <span>&nbsp;|&nbsp;</span>
          <span>{{$filters.dateTime(notif.creationTime)}}</span>
        </div>
      </div>
    </li>
    <li class="footer" v-if="!loading && notifs.length > 0">
      <a :href="$ui.ngServer + '#/notifications'">
        <span v-t="'common.notifs.view_all'">View All Notifications</span>
      </a>
    </li>
  </ul>
</template>

<script>

import notifSvc from '@/common/services/Notif.js';
import urlResolver from '@/common/services/UrlResolver.js';

export default {
  data() {
    return {
      loading: true,

      notifs: []
    }
  },

  created() {
    this.loading = true;
    this.notifs = [];
    notifSvc.getNotifications(0, 10).then(
      notifs => {
        this.loading = false;
        this.notifs = notifs;
        notifs.forEach(
          (notif) => {
            notif.href = notif.urlKey;
            notif.newTab = true;
            if (notif.urlKey.indexOf('http://') != 0 && notif.urlKey.indexOf('https://') != 0) {
              notif.href = urlResolver.getUrl(notif.urlKey, notif.entityId);
              notif.newTab = false;
            }
          }
        );
      }
    );
  },

  methods: {
    visitNotif: function(notif) {
      if (notif.href) {
        window.open(notif.href, notif.newTab ? '_blank' : '_self');
      }
    },

    stopPropagation: function(event) {
      event.stopPropagation();
    }
  }
}

</script>

<style>
.os-notifs-list {
  list-style: none;
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

.os-notifs-list .notif {
  padding: 1rem;
  margin-left: 2rem;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  flex-direction: row;
  cursor: pointer;
  position: relative;
}

.os-notifs-list .notif-unread {
  position: absolute;
  top: 2px;
  bottom: 2px;
  left: -3.0rem;
  border-left: 4px solid #3071a9;
}

.os-notifs-list .notif-type {
  height: 2rem;
  width: 2rem;
  background: #d5d5d5;
  text-align: center;
  line-height: 2rem;
  margin-left: -3.0rem;
  display: flex;
  justify-content: center;
}

.os-notifs-list .notif-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 0rem 1rem;
  margin-top: -0.2rem;
}

.os-notifs-list .notif-message a {
  color: inherit;
}

.os-notifs-list .notif-meta {
  font-size: 12px;
  color: #666;
}

.os-notifs-list .footer {
  padding-top: 0.5rem;
  margin-top: -1px;
  border-top: 1px solid #e6e6e6;
  font-weight: bold;
}
</style>
