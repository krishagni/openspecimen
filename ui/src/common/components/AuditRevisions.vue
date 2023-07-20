<template>
  <os-dialog ref="revsDialog">
    <template #header>
      <span v-t="'audit.revs_list'">Revisions</span>
    </template>

    <template #content>
      <table class="os-table" v-if="ctx.revisions && ctx.revisions.length > 0">
        <thead>
          <tr>
            <th v-t="'audit.updated_on'">Updated On</th>
            <th v-t="'audit.updated_by'">Updated By</th>
            <th v-if="showRevDownload">&nbsp;</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(rev, idx) of ctx.revisions" :key="idx">
            <td>{{$filters.dateTime(rev.changedOn)}}</td>
            <td>{{$filters.username(rev.changedBy)}}</td>
            <td v-if="showRevDownload">
              <os-button size="small" left-icon="download" v-os-tooltip.bottom="$t('audit.download_revision')"
                @click="downloadRevision(rev)" />
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else>
        <span v-t="'audit.no_revs'">No revisions to show!</span>
      </div>
    </template>

    <template #footer>
      <os-button primary :label="$t('common.buttons.done')" @click="closeRevs" />
    </template>
  </os-dialog>
</template>

<script>

import auditLogSvc from '@/common/services/AuditLogs.js';

export default {
  props: ['show-rev-download'],

  emits: ['download-revision'],

  data() {
    return {
      ctx: {
        revisions: []
      }
    };
  },

  methods: {
    showRevs: function(objects) {
      auditLogSvc.getRevisions(objects).then(
        (revisions) => {
          this.ctx.revisions = revisions;
          this.$refs.revsDialog.open();
        }
      );
    },

    closeRevs: function() {
      this.$refs.revsDialog.close();
    },

    downloadRevision: function(rev) {
      this.$emit('downloadRevision', {revision: rev});
    }
  }
}
</script>
