<template>
  <os-panel>
    <template #header>
      <span v-t="'cps.revisions'">Revisions</span>
    </template>

    <div class="os-cp-revisions">
      <os-list-view
        :data="ctx.revisions"
        :schema="listSchema"
        :loading="ctx.loading"
        :showRowActions="true"
        ref="listView">

        <template #rowActions="{rowObject: {revision}}">
          <os-button-group>
            <os-button size="small" left-icon="eye" @click="viewDetails(revision)"
              v-os-tooltip.bottom="$t('cps.view_rev_details')" />
            <os-button size="small" left-icon="download" @click="downloadRev(revision)"
              v-os-tooltip.bottom="$t('cps.download_rev_details')" />
          </os-button-group>
        </template>

        <template #footer>
          <os-pager :start-at="ctx.startAt" :have-more="ctx.haveMoreRevs"
            @previous="previousPage" @next="nextPage" />
        </template>
      </os-list-view>
    </div>
  </os-panel>

  <os-dialog ref="detailDialog">
    <template #header>
      <span v-t="{path: 'cps.revision_detail', args: ctx.revision}">View Revision Detail</span>
    </template>

    <template #content>
      <os-overview :schema="revSchema.fields" :object="{revision: ctx.revision}" :columns="1" />
    </template>

    <template #footer>
      <os-button text    :label="$t('common.buttons.close')"    @click="closeDetailDialog" />
      <os-button primary :label="$t('common.buttons.download')" @click="downloadRev(ctx.revision)" />
    </template>
  </os-dialog>
</template>

<script>

import listSchema from '@/biospecimen/schemas/cps/revisions-list.js';
import revSchema  from '@/biospecimen/schemas/cps/revision.js';

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';

const MAX_REVS = 25;

export default {
  props: ['cp'],

  data() {
    return {
      ctx: {
        revisions: [],

        startAt: 0,

        loading: false
      },

      listSchema,

      revSchema
    }
  },

  created() {
    this.loadRevisions(0);
  },

  methods: {
    loadRevisions: async function(startAt) {
      const opts = { startAt: startAt || 0, maxResults: MAX_REVS + 1 };

      this.ctx.loading = true;
      const revisions = await cpSvc.getRevisions(this.cp, opts);
      if (revisions.length > MAX_REVS) {
        this.ctx.haveMoreRevs = true;
        revisions.splice(revisions.length - 1, 1);
      } else {
        this.ctx.haveMoreRevs = false;
      }

      this.ctx.revisions = revisions.map(revision => ({revision}));
      this.ctx.loading = false;
      this.ctx.startAt = startAt;
    },

    previousPage: function() {
      this.loadRevisions(this.ctx.startAt - MAX_REVS);
    },  
      
    nextPage: function() {
      this.loadRevisions(this.ctx.startAt + MAX_REVS);
    },

    viewDetails: function(revision) {
      this.ctx.revision = revision;
      this.$refs.detailDialog.open();
    },

    closeDetailDialog: function() {
      this.$refs.detailDialog.close();
    },

    downloadRev: function(revision) {
      cpSvc.downloadRevisionJson(this.cp, revision.id);
    }
  }
}
</script>
