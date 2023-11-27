<template>
  <span>
    <span v-if="!href">{{description}}</span>
    <a v-else :href="href">
      <span>{{description}}</span>
    </a>
  </span>
</template>

<script>

import i18n from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';
import ui from '@/global.js';

export default {
  props: ['visit', 'event', 'showLink'],

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
        const route = routerSvc.getCurrentRoute();
        const params = {
          cpId: this.visit.cpId,
          cprId: this.visit.cprId,
          visitId: this.visit.id || -1,
          eventId: this.visit.eventId
        };

        if (route.name && route.name.indexOf('ParticipantsListItem') >= 0) {
          url = routerSvc.getUrl('ParticipantsListItemVisitDetail.Overview', params);
        } else {
          url = routerSvc.getUrl('VisitDetail.Overview', params);
        }
      } else if (this.event) {
        url = ui.ngServer + '#/cps/' + this.event.cpId + '/specimen-requirements?eventId=' + this.event.id;
      }

      return url;
    }
  }
}
</script>
