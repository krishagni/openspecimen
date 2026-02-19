<template>
  <a class="description" :href="link" :target="hrefTarget || '_self'" rel="noopener">
    <os-specimen-status-icon :specimen="object" :status="status" v-if="showStatus" />
    <os-dynamic-template v-if="tmpl" :template="tmpl" :specimen="object" v-bind="$attrs" />
    <span v-else>{{description}}</span>
    <os-tag class="pooled-specimen" :value="$t('specimens.pooled')" :rounded="true"
      v-if="showStatus && object.specimensPool && object.specimensPool.length > 0" />
  </a>
</template>

<script>

import cpSvc     from '@/biospecimen/services/CollectionProtocol.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

export default {
  props: ['model-value', 'specimen', 'show-status', 'status', 'href-target', 'no-link'],

  data() {
    return {
      tmpl: null
    }
  },

  computed: {
    inputValue: {
      get() {
        return this.modelValue;
      },

      set(value) {
        this.$emit('update:modelValue', value);
      }
    },

    object: function() {
      return this.inputValue || this.specimen || {type: 'Unknown Type'};
    },

    statusColor: function() {
      const status = this.status || this.object.availabilityStatus || this.object.status;
      switch (status) {
        case 'Distributed':
          return 'distributed';
        case 'Reserved':
          return 'reserved';
        case 'Closed':
          return 'closed';
        case 'Missed Collection':
        case 'Not Collected':
          return 'not-collected';
        case 'Available':
          return 'collected';
        case 'Pending':
        default:
          return 'pending';
      }
    },

    description: function() {
      return this._getDescription(this.object, this.$attrs);
    },

    link: function() {
      if (this.noLink) {
        return null;
      }

      const specimen = this.object;
      let url = '';
      if (specimen) {
        const params = {...specimen, specimenId: specimen.id || -1};
        const query  = {view: this.$route.query.view, eventId: specimen.eventId, reqId: specimen.reqId};
        url = routerSvc.getUrl('ParticipantsListItemSpecimenDetail.Overview', params, query);
      }

      const currentView = window.location.href;
      const path = currentView.substring(0, currentView.indexOf('/#'));
      return path + '/' + url;
    }
  },

  created() {
    cpSvc.getWorkflowProperty(this.object.cpId, 'common', 'spmnDescTmpl').then(tmpl => this.tmpl = tmpl);
  },

  methods: {
    _getDescription: function(value, attrs) {
      return util.getSpecimenDescription(value, attrs);
    }
  }
}

</script>

<style scoped>

.description {
  display: flex;
  flex-direction: row;
}

.description :deep(.status-ball) {
  flex: 0 0 0.75rem;
  margin-top: 0.25rem;
  margin-right: 0.25rem;
  margin-bottom: -0.0625rem;
}

.description .pooled-specimen {
  flex: 0 0 0.75rem;
  margin-top: -0.25rem;
  margin-left: 0.25rem;
}

.description .pooled-specimen :deep(.p-tag) {
  background: #ff69b4;
}

a.description:not([href]) {
  text-decoration: none;
  color: inherit;
}
</style>
