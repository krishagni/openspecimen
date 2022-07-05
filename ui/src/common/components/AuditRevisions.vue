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
          </tr>
        </thead>
        <tbody>
          <tr v-for="(rev, idx) of ctx.revisions" :key="idx">
            <td>{{$filters.dateTime(rev.changedOn)}}</td>
            <td>{{$filters.username(rev.changedBy)}}</td>
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
    }
  }
}
</script>
