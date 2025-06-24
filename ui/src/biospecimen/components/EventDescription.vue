<template>
  <span class="os-event-description">
    <span class="status" v-show="showStatus">
      <os-specimen-status-icon :status="input.activityStatus" />
    </span>
    <span v-if="!href">{{description}}</span>
    <a v-else :href="href">
      <span>{{description}}</span>
    </a>
  </span>
</template>

<script>

import i18n from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';
export default {
  props: ['visit', 'event', 'showLink', 'showStatus'],

  computed: {
    input: function() {
      return this.visit || this.event || {};
    },

    description: function() {
      const input = this.input;

      let result = '';
      if (input.eventLabel) {
        if (input.eventPoint != null && input.eventPoint != undefined) {
          result += ((input.eventPoint < 0) ? '-' : '') + 'T' + Math.abs(input.eventPoint);
          result += input.eventPointUnit.charAt(0) + ': ';
        }

        result += input.eventLabel;
        if (input.eventCode || input.code) {
          result += ' (' + (input.eventCode || input.code) + ')';
        }
      } else {
        result = i18n.msg('visits.unplanned_visit');
      }

      return result;
    },

    href: function() {
      if (this.showLink != true && this.showLink != 'true') {
        return '';
      }

      let url = '';
      if (this.visit) {
        const {cpId, eventId, cprId, id} = this.visit;
        const route = routerSvc.getCurrentRoute();
        const params = { cpId, cprId, visitId: id || -1};

        if (route.name && route.name.indexOf('ParticipantsListItem') >= 0) {
          url = routerSvc.getUrl('ParticipantsListItemVisitDetail.Overview', params, {eventId});
        } else {
          url = routerSvc.getUrl('VisitDetail.Overview', params, {eventId});
        }
      } else if (this.event) {
        url = routerSvc.getUrl('CpDetail.Events.List', {cpId: this.event.cpId}, {eventId: this.event.id});
      }

      return url;
    }
  }
}
</script>

<style scoped>
.os-event-description {
  display: flex;
  flex-direction: row;
  align-items: center;
}

.os-event-description .status {
  margin-right: 0.5rem;
  width: 1rem;
}
</style>
