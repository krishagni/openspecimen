<template>
  <os-page-toolbar>
    <template #default>
      <os-menu icon="download" :label="$t('common.buttons.export')" :options="exportOpts" v-if="eximAllowed" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <span>{{cpg.name}} details</span>
    </os-grid-column>
  </os-grid>
</template>

<script>
import exportSvc from '@/common/services/ExportService.js';

export default {
  props: ['cpg', 'permOpts'],

  data() {
    return {
      ctx: {
      }
    }
  },

  computed: {
    eximAllowed: function() {
      return this.permOpts && this.permOpts.eximAllowed;
    },

    updateAllowed: function() {
      return this.permOpts && this.permOpts.updateAllowed;
    },

    exportOpts: function() {
      return [
        {icon: 'calendar', caption: this.$t('cpgs.collection_protocols'),  onSelect: this.exportCps},
        {icon: 'list-alt', caption: this.$t('cpgs.cpes'),                  onSelect: this.exportCpEvents},
        {icon: 'flask',    caption: this.$t('cpgs.specimen_requirements'), onSelect: this.exportCpSrs}
      ];
    }
  },

  methods: {
    exportCps: function() {
      this._exportCpRecords('cp');
    },

    exportCpEvents: function() {
      this._exportCpRecords('cpe');
    },

    exportCpSrs: function() {
      this._exportCpRecords('sr');
    },

    _exportCpRecords: function(objectType) {
      exportSvc.exportRecords({objectType, recordIds: this.cpg.cps.map(cp => cp.id)});
    }
  }
}
</script>
