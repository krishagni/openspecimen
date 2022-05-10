
<template>
  <os-list-view
    :data="events"
    :schema="listSchema"
    :loading="loading"
    ref="listView"
  />
</template>

<script>

import listSchema from '@/administrative/schemas/containers/transfer-events.js';
import containerSvc from '@/administrative/services/Container.js';

export default {
  props: ['container'],

  data() {
    return {
      events: [],

      loading: false,

      listSchema
    };
  },

  created() {
    this.timer = setTimeout(() => this.loadEvents(), 250);
  },

  watch: {
    'container.id': function(newVal, oldVal) {
      if (this.timer) {
        clearTimeout(this.timer);
        this.timer = null;
      }

      if (newVal != oldVal) {
        this.loadEvents();
      }
    }
  },

  methods: {
    loadEvents: function() {
      this.loading = true;
      containerSvc.getTransferEvents(this.container).then(
        (events) => {
          this.events = events.map(event => ({ event }));
          this.loading = false;
        }
      );
    }
  }
}
</script>
