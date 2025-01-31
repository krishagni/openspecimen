<template>
  <router-view :cp="cp" :event="event" v-if="cp && event" :key="viewKey"
    @cpe-saved="event = $event" />
</template>

<script>
import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['cp', 'eventId'],

  data() {
    return {
      event: null
    }
  },

  created() {
    this._loadEvent();
  },

  mounted() {
    this._gotoEventsList();
  },

  computed: {
    viewKey: function() {
      const {cp, event} = this;
      return (cp && cp.id > 0 ? cp.id : -1) + '_' + (event && event.id > 0 ? event.id : '-1');
    }
  },

  watch: {
    '$route': function() {
      this._gotoEventsList();
    },

    eventId: function() {
      this._loadEvent();
    }
  },

  methods: {
    _gotoEventsList: function() {
      const {name} = routerSvc.getCurrentRoute();
      if (name == 'CpDetail.Events') {
        routerSvc.goto('CpDetail.Events.List');
      }
    },

    _loadEvent: function() {
      if (this.eventId > 0) {
        cpSvc.getCpe(this.eventId).then(event => this.event = event);
      } else {
        this.event = {};
      }
    }
  }
}
</script>
