<template>
  <a class="description" :href="link" :target="hrefTarget || '_self'" rel="noopener">
    <span class="status-ball" :class="statusColor" v-if="showStatus && statusColor"></span>
    <span>{{description}}</span>
  </a>
</template>

<script>

import routerSvc from '@/common/services/Router.js';

export default {
  props: ['model-value', 'specimen', 'show-status', 'status', 'href-target'],

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
      const specimen = this.object;
      let url = '';
      if (specimen) {
        const route = routerSvc.getCurrentRoute();
        const params = {...specimen, specimenId: specimen.id || -1}
        if (route.name && route.name.indexOf('ParticipantsListItem') >= 0) {
          url = routerSvc.getUrl('ParticipantsListItemSpecimenDetail.Overview', params);
        } else {
          url = routerSvc.getUrl('SpecimenDetail.Overview', params);
        }
      }

      const currentView = window.location.href;
      const path = currentView.substring(0, currentView.indexOf('/#'));
      return path + '/' + url;
    }
  },

  methods: {
    _getDescription: function(value, attrs) {
      const ns = this.$t('pvs.not_specified');
      const specimen = value || {};
      const detailed = attrs.detailed == 'true' || attrs.detailed == true;

      let result = '';
      if (specimen.lineage == 'New' || detailed) {
        if (specimen.pathology && specimen.pathology != ns) {
          result += specimen.pathology + ' ';
        }

        result += specimen.type;

        if (specimen.specimenClass == 'Tissue' && specimen.anatomicSite && specimen.anatomicSite != ns) {
          result += ' ' + this.$t('specimens.extracted_from', {anatomicSite: specimen.anatomicSite});
        }

        if (specimen.specimenClass == 'Fluid' && specimen.collectionContainer && specimen.collectionContainer != ns) {
          result += ' ' + this.$t('specimens.collected_in', {container: specimen.collectionContainer});
        }
      } else if (specimen.lineage == 'Derived') {
        result += specimen.lineage + ' ' + specimen.type;
      } else if (specimen.lineage == 'Aliquot') {
        result += specimen.lineage;
      }

      return result;
    }
  }
}

</script>

<style scoped>

.description {
  display: flex;
  flex-direction: row;
}

.status-ball {
  flex: 0 0 0.75rem;
  height: 0.75rem;
  width: 0.75rem;
  border-radius: 50%;
  background: #a0a0a0;
  margin-top: 0.25rem;
  margin-right: 0.25rem;
  margin-bottom: -0.0625rem;
}

.status-ball.collected {
  background: #5cb85c;
}

.status-ball.not-collected {
  background: #888;
}

.status-ball.pending {
  background: #f0ad4e;
}

.status-ball.closed {
  background: #d9534f!important;
}

.status-ball.distributed {
  background: #5bc0de;
}

.status-ball.reserved {
  background: rgba(128, 0, 128, 0.7);
}
</style>

