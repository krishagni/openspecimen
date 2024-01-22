
<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="plus" :label="$t('participants.add_specimen')" @click="addSpecimen" />

      <os-button left-icon="print" :label="$t('participants.print_specimen_labels')" @click="printLabels" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />

      <SpecimenTree :cp="ctx.cp" :specimens="ctx.specimens" v-if="ctx.cp.id > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.auditObjs" v-if="ctx.visit.id > 0" />
    </os-grid-column>
  </os-grid>
</template>

<script>

import SpecimenTree from '@/biospecimen/components/SpecimenTree.vue';

import specimenSvc from '@/biospecimen/services/Specimen.js';
import util        from '@/common/services/Util.js';
import visitSvc    from '@/biospecimen/services/Visit.js';
import wfSvc       from '@/biospecimen/services/Workflow.js';

export default {
  props: ['visit'],

  components: {
    SpecimenTree
  },

  inject: ['cpViewCtx'],

  data() {
    return {
      ctx: {
        cp: {},

        visit: {},

        dict: [],

        auditObjs: [],

        routeQuery: this.$route.query,

        specimens: []
      }
    };
  },

  async created() {
    this._setupVisit();
    this._loadSpecimens();
    this.ctx.dict = await this.cpViewCtx.getVisitDict();
  },

  watch: {
    visit: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupVisit();
        this._loadSpecimens();
      }
    }
  },

  methods: {
    addSpecimen: function() {
      wfSvc.addSpecimen(this.visit);
    },

    printLabels: function() {
      const ts = util.formatDate(new Date(), 'yyyyMMdd_HHmmss');
      const outputFilename = [this.visit.cpShortTitle, this.visit.ppid, this.visit.name, ts].join('_') + '.csv';
      specimenSvc.printLabels({visitId: this.visit.id}, outputFilename);
    },

    _setupVisit: function() {
      this.cpViewCtx.getCp().then(cp => this.ctx.cp = cp);

      const visit = this.ctx.visit = this.visit;
      if (visit.id > 0) {
        this.ctx.auditObjs = [
          {objectId: visit.id, objectName: 'visit'}
        ]
      }
    },

    _loadSpecimens: function() {
      const visit = this.ctx.visit;
      if (visit.$specimens) {
        this.ctx.specimens = visit.$specimens;
        return;
      }

      const {cprId, id, eventId} = visit;
      visitSvc.getSpecimens(cprId, eventId, id).then(
        (specimens) => {
          this.ctx.specimens = visit.$specimens = specimens;
        }
      );
    }
  }
}
</script>
