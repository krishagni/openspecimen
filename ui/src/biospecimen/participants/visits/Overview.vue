
<template>
  <os-page-toolbar>
    <template #default>
      <span v-if="visit.id > 0">
        <os-button left-icon="edit" :label="$t('common.buttons.edit')" @click="editVisit" />

        <os-button left-icon="plus" :label="$t('participants.add_specimen')" @click="addSpecimen" />

        <os-button left-icon="print" :label="$t('participants.print_specimen_labels')" @click="printLabels" />

        <os-button left-icon="trash" :label="$t('common.buttons.delete')" @click="deleteVisit" />

        <os-plugin-views page="visit-detail" view="more-menu" />
      </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="8">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />

      <SpecimenTree :cp="ctx.cp" :specimens="ctx.specimens" @reload="reloadSpecimens" v-if="ctx.cp.id > 0" />
    </os-grid-column>

    <os-grid-column width="4">
      <os-audit-overview :objects="ctx.auditObjs" v-if="ctx.visit.id > 0" />
    </os-grid-column>

    <os-delete-object ref="deleteVisitDialog" :input="ctx.deleteOpts" />
  </os-grid>
</template>

<script>

import SpecimenTree from '@/biospecimen/components/SpecimenTree.vue';

import routerSvc   from '@/common/services/Router.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';
import util        from '@/common/services/Util.js';
import visitSvc    from '@/biospecimen/services/Visit.js';
import wfSvc       from '@/biospecimen/services/Workflow.js';

export default {
  props: ['cpr', 'visit'],

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

        specimens: [],
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
    editVisit: function() {
      routerSvc.goto('VisitAddEdit', {cpId: this.ctx.cp.id, cprId: this.cpr.id, visitId: this.visit.id});
    },

    addSpecimen: function() {
      wfSvc.addSpecimen(this.ctx.cp, this.visit);
    },

    printLabels: function() {
      const ts = util.formatDate(new Date(), 'yyyyMMdd_HHmmss');
      const outputFilename = [this.visit.cpShortTitle, this.visit.ppid, this.visit.name, ts].join('_') + '.csv';
      specimenSvc.printLabels({visitId: this.visit.id}, outputFilename);
    },

    deleteVisit: function() {
      this.$refs.deleteVisitDialog.execute().then(
        (resp) => {
          if (resp != 'deleted') {
            return;
          }

          const route = routerSvc.getCurrentRoute();
          if (route.name.indexOf('ParticipantsListItemVisitDetail') >= 0) {
            routerSvc.goto('ParticipantsListItemDetail.Overview', this.visit);
          } else {
            routerSvc.goto('ParticipantDetail.Overview', this.visit);
          }

          visitSvc.clearVisits(this.cpr);
        }
      );
    },

    reloadSpecimens: function() {
      const { visit } = this.ctx;
      visit.$specimens = null;
      this.ctx.specimens = [];
      this._loadSpecimens();
    },

    _setupVisit: function() {
      this.cpViewCtx.getCp().then(cp => this.ctx.cp = cp);

      const visit = this.ctx.visit = this.visit;
      if (visit.id > 0) {
        this.ctx.auditObjs = [
          {objectId: visit.id, objectName: 'visit'}
        ]

        this.ctx.deleteOpts = {
          type: this.$t('visits.visit'),
          title: visit.name,
          dependents: () => visitSvc.getDependents(visit),
          forceDelete: true,
          askReason: true,
          deleteObj: (reason) => visitSvc.deleteVisit(visit.id, true, reason)
        };
      }
    },

    _loadSpecimens: function() {
      const visit = this.ctx.visit;
      if (visit.$specimens) {
        this.ctx.specimens = visit.$specimens;
        return;
      }

      const {id, eventId} = visit;
      visitSvc.getSpecimens(this.cpr.id, eventId, id).then(
        (specimens) => {
          this.ctx.specimens = visit.$specimens = specimens;
        }
      );
    }
  }
}
</script>
