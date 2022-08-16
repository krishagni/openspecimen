<template>
  <div>
    <os-dialog ref="dialog">
      <template #header>
        <slot name="title"></slot>
      </template>

      <template #content>
        <table class="os-table os-table-borderless">
          <tbody>
            <tr>
              <td> <slot name="found"></slot> </td>
              <td> <span>{{validationResult.found.length}}</span> </td>
            </tr>
            <tr>
              <td> <slot name="notFound"></slot> </td>
              <td> <span>{{validationResult.notFound.length}}</span> </td>
            </tr>
            <tr>
              <td> <slot name="extras"></slot> </td>
              <td> <span>{{validationResult.extra.length}}</span> </td>
            </tr>
          </tbody>
        </table>
      </template>

      <template #footer>
        <os-button primary :label="$t('common.copy_to_clipboard')" @click="generateReport" v-if="isReportAvailable" />
        <os-button primary :label="$t('common.buttons.ok')" @click="dismiss" />
      </template>
    </os-dialog>

    <slot name="report">
    </slot>
  </div>
</template>

<script>

import util from '@/common/services/Util.js';
import alertSvc from '@/common/services/Alerts.js';

export default {
  props: ['report-messages'],

  data() {
    return {
      validationResult: { found: [], notFound: [], extra: [] }
    };
  },

  computed: {
    isReportAvailable: function() {
      return this.validationResult.notFound.length > 0 || this.validationResult.extra.length > 0;
    }
  },

  methods: {
    validate: function(items, itemLabels, prop) {
      this.validationResult = util.validateItems(items, itemLabels, prop);
      this.$refs.dialog.open();
    },

    generateReport: async function() {
      let rm = this.reportMessages || {};

      let report = (rm.label || 'Label') + ',' + (rm.error || 'Error') + '\n';
      for (let nf of this.validationResult.notFound) {
        report += nf + ',' + (rm.notFound || 'Not found') + '\n';
      }

      for (let extra of this.validationResult.extra) {
        report += extra + ',' + (rm.extra || 'Extra') + '\n';
      }

      try {
        await util.copyToClipboard(report)
        alertSvc.success({code: 'common.copied_to_clipboard'});
      } catch (e) {
        console.log(e);
        alertSvc.success({code: 'common.copy_to_clipboard_failed'});
      }
    },

    dismiss: function() {
      this.$refs.dialog.close();
    }
  }
}
</script>
