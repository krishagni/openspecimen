
<template>
  <os-dialog ref="auditTrailDialog">
    <template #header>
      <span v-t="'audit.trail'"></span>
    </template>
    <template #content>
      <os-tabs ref="auditTrailTabs" @tab-changed="onTabChange">
        <os-tab>
          <template #header>
            <span v-t="'common.overview'">Overview</span>
          </template>

          <os-message type="info" v-if="ctx.loadingTrail">
            <span v-t="'audit.loading_trail'">Loading...</span>
          </os-message>
          <ul class="os-key-values os-one-col" v-else-if="ctx.auditTrail.createdBy || ctx.auditTrail.lastUpdatedBy">
            <li class="item" v-if="ctx.auditTrail.createdBy">
              <strong class="key key-sm" v-t="'audit.entered_by'">Entered By</strong>
              <span class="value value-md">{{$filters.username(ctx.auditTrail.createdBy)}}</span>
            </li>
            <li class="item" v-if="ctx.auditTrail.createdOn">
              <strong class="key key-sm" v-t="'audit.entered_on'">Entered On</strong>
              <span class="value value-md">{{$filters.dateTime(ctx.auditTrail.createdOn)}}</span>
            </li>
            <li class="item" v-if="ctx.auditTrail.lastUpdatedBy">
              <strong class="key key-sm" v-t="'audit.updated_by'">Updated By</strong>
              <span class="value value-md">{{$filters.username(ctx.auditTrail.lastUpdatedBy)}}</span>
            </li>
            <li class="item" v-if="ctx.auditTrail.lastUpdatedOn">
              <strong class="key key-sm" v-t="'audit.updated_on'">Updated On</strong>
              <span class="value value-md">{{$filters.dateTime(ctx.auditTrail.lastUpdatedOn)}}</span>
            </li>
            <li class="item">
              <strong class="key key-sm" v-t="'audit.no_of_revs'">Number of Revisions</strong>
              <span class="value value-md">
                <span>{{ctx.auditTrail.revisionsCount}}</span>
                <a @click="showRevs" v-if="ctx.auditTrail.revisionsCount > 0">
                  <span>&nbsp;</span>
                  <span v-t="'audit.view_revisions'">(View All)</span>
                </a>
              </span>
            </li>
          </ul>
          <os-message type="error" v-else>
            <span v-t="'audit.no_trail'">No trail to show!</span>
          </os-message>
        </os-tab>

        <os-tab>
          <template #header>
            <span v-t="'audit.revs_list'">Revisions</span>
          </template>

          <os-message type="info" v-if="ctx.loadingRevisions">
            <span v-t="'audit.loading_revisions'"></span>
          </os-message>
          <table class="os-table" v-else-if="ctx.revisions && ctx.revisions.length > 0">
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
          <os-message type="error" v-else>
            <span v-t="'audit.no_revs'">No revisions to show!</span>
          </os-message>
        </os-tab>
      </os-tabs>
    </template>
    <template #footer>
      <os-button primary :label="$t('common.buttons.done')" @click="close" />
    </template>
  </os-dialog>
</template>

<script>

import auditLogSvc from '@/common/services/AuditLogs.js';

export default {
  props: ['objects'],

  data() {
    return {
      ctx: {
        loadingTrail: true,

        loadingRevisions: false,

        auditTrail: {},

        revisions: null
      }
    };
  },

  methods: {
    open: function() {
      this.$refs.auditTrailDialog.open();
      this.ctx = { loadingTrail: true, auditTrail: {}, loadingRevisions: false, revisions: null },
      this._loadAuditTrail().then(() => this.ctx.loadingTrail = false);
    },

    close: function() {
      this.$refs.auditTrailDialog.close();
    },

    showRevs: function() {
      this.$refs.auditTrailTabs.activate(1);

      this.ctx.loadingRevisions = true;
      this._loadRevisions().then(() => this.ctx.loadingRevisions = false);
    },

    onTabChange: function({activeTab}) {
      if (activeTab == 1) {
        this.showRevs();
      }
    },

    _loadAuditTrail: async function() {
      const ctx = this.ctx;

      ctx.auditTrail = {};
      if (!this.objects || this.objects.length <= 0) {
        return;
      }

      const infoList = await auditLogSvc.getSummary(this.objects);

      let trail = {};
      infoList.forEach(
        (info, idx) => {
          if (idx == 0) {
            trail = info;
          } else {
            if (trail.createdOn < info.createdOn) {
              trail.createdBy = info.createdBy;
              trail.createdOn = info.createdOn;
            }

            if (info.lastUpdatedOn > trail.lastUpdatedOn) {
              trail.lastUpdatedBy = info.lastUpdatedBy;
              trail.lastUpdatedOn = info.lastUpdatedOn;
            }

            trail.revisionsCount += info.revisionsCount;
          }
        }
      );

      ctx.auditTrail = trail;
    },

    _loadRevisions: async function() {
      if (!this.objects || this.objects.length <= 0 || this.ctx.revisions) {
        return this.ctx.revisions;
      }

      this.ctx.revisions = await auditLogSvc.getRevisions(this.objects);
      return this.ctx.revisions;
    }
  }
}
</script>
