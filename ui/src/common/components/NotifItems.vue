<template>
  <div class="notif" v-for="({notif, href, newTab}, idx) of notifItems" :key="idx" @click="visitNotif(href, newTab)">
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
        <a :href="href" :target="newTab ? '_blank' : '_self'" @click="stopPropagation">
          <span>{{notif.message}}</span>
        </a>
      </div>
      <div class="notif-meta">
        <span>{{$filters.username(notif.createdBy)}}</span>
        <span>&nbsp;|&nbsp;</span>
        <span>{{$filters.dateTime(notif.creationTime)}}</span>
      </div>
    </div>
  </div>
</template>

<script>

import urlResolver from '@/common/services/UrlResolver.js';

export default {
  props: ['notifs'],

  data() {
  },

  computed: {
    notifItems: function() {
      return (this.notifs || []).map(
        notif => {
          const item = {notif};
          item.href = notif.urlKey;
          item.newTab = true;
          if (notif.urlKey.indexOf('http://') != 0 && notif.urlKey.indexOf('https://') != 0) {
            item.href = urlResolver.getUrl(notif.urlKey, notif.entityId);
            item.newTab = false;
          }

          return item
        }
      );
    }
  },

  methods: {
    visitNotif: function(href, newTab) {
      if (href) {
        window.open(href, newTab ? '_blank' : '_self');
      }
    },

    stopPropagation: function(event) {
      event.stopPropagation();
    }
  }
}

</script>

<style>
.notif {
  padding: 1rem;
  margin-left: 2rem;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  flex-direction: row;
  cursor: pointer;
  position: relative;
}

.notif-unread {
  position: absolute;
  top: 2px;
  bottom: 2px;
  left: -3.0rem;
  border-left: 4px solid #3071a9;
}

.notif-type {
  height: 2rem;
  width: 2rem;
  background: #d5d5d5;
  text-align: center;
  line-height: 2rem;
  margin-left: -3.0rem;
  display: flex;
  justify-content: center;
}

.notif-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 0rem 1rem;
  margin-top: -0.2rem;
}

.notif-message a {
  color: inherit;
}

.notif-meta {
  font-size: 12px;
  color: #666;
}
</style>
