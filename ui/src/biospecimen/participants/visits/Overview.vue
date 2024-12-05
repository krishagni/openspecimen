
<template>
  <os-page-toolbar>
    <template #default>
      <span v-if="visit.id > 0">
        <os-button left-icon="edit" :label="$t('common.buttons.edit')"
          @click="editVisit" v-if="isUpdateAllowed" />

        <os-button left-icon="plus" :label="$t('participants.add_specimen')"
          @click="addSpecimen" v-if="cpr.hasConsented && ctx.visit.status == 'Complete' && isCreateSpecimenAllowed" />

        <os-button left-icon="print" :label="$t('participants.print_specimen_labels')"
          @click="printLabels" v-if="isPrintSpecimenLabelAllowed" />

        <os-button left-icon="trash" :label="$t('common.buttons.delete')"
          @click="deleteVisit" v-if="isDeleteAllowed" />

        <os-menu :label="$t('common.buttons.more')" :options="ctx.moreOptions" />
      </span>
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="ctx.dict" :object="ctx" v-if="ctx.dict.length > 0" />

      <SpecimenTree :cp="ctx.cp" :cpr="cpr" :visit="visit" :specimens="ctx.specimens"
        :ref-date="visit.status && visit.status != 'Pending' ? visit.visitDate : 0"
        @reload="reloadSpecimens" v-if="ctx.cp.id > 0 && isReadSpecimenAllowed" />
    </os-grid-column>
  </os-grid>

  <os-delete-object ref="deleteVisitDialog" :input="ctx.deleteOpts" />

  <os-audit-trail ref="auditTrailDialog" :objects="ctx.auditObjs" />

  <os-plugin-views ref="moreMenuPluginViews" page="visit-detail" view="more-menu" :view-props="ctx" />
</template>

<script>

import SpecimenTree from '@/biospecimen/components/SpecimenTree.vue';

import i18n        from '@/common/services/I18n.js';
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
    const cp = this.cpViewCtx.getCp();

    return {
      ctx: {
        cp,

        cpr: this.cpr,

        visit: {},

        dict: [],

        auditObjs: [],

        routeQuery: this.$route.query,

        specimens: [],

        userRole: this.cpViewCtx.getRole(),

        moreOptions: []
      }
    };
  },

  async created() {
    this._setupVisit();
    this._loadSpecimens();
    this.ctx.dict = await this.cpViewCtx.getVisitDict();

    // Need to load menu options only after the above async call is finished
    // Otherwise the plugin menu options ref is undefined (not available)
    this._loadMoreMenuOptions();
  },

  watch: {
    visit: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupVisit();
        this._loadSpecimens();
        this._loadMoreMenuOptions();
      }
    }
  },

  computed: {
    isUpdateAllowed: function() {
      return this.cpViewCtx.isUpdateVisitAllowed(this.cpr);
    },

    isDeleteAllowed: function() {
      return this.cpViewCtx.isDeleteVisitAllowed(this.cpr);
    },

    isCreateSpecimenAllowed: function() {
      return this.cpViewCtx.isCreateSpecimenAllowed(this.cpr);
    },

    isReadSpecimenAllowed: function() {
      return this.cpViewCtx.isReadSpecimenAllowed(this.cpr);
    },

    isPrintSpecimenLabelAllowed: function() {
      return this.cpViewCtx.isPrintSpecimenAllowed(this.cpr) && !this.cpViewCtx.isCoordinator();
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

    viewAuditTrail: function() {
      this.$refs.auditTrailDialog.open();
    },

    _setupVisit: function() {
      const visit = this.ctx.visit = this.visit;
      if (visit.id > 0) {
        this.ctx.auditObjs = [
          {objectId: visit.id, objectName: 'visit'}
        ]

        this.ctx.deleteOpts = {
          type: i18n.msg('visits.visit'),
          title: visit.name,
          dependents: () => visitSvc.getDependents(visit),
          forceDelete: true,
          askReason: true,
          deleteObj: (reason) => visitSvc.deleteVisit(visit.id, true, reason)
        };
      }
    },

    _loadSpecimens: function() {
      this.ctx.specimens = [];
      if (!this.isReadSpecimenAllowed) {
        return;
      }

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
    },

    _loadMoreMenuOptions: function() {
      const ctxt = this.pluginViewProps = {...this.ctx, cpViewCtx: this.cpViewCtx};
      util.getPluginMenuOptions(this.$refs.moreMenuPluginViews, 'visit-detail', 'more-menu', ctxt)
        .then(
          pluginOptions => {
            const options = this.ctx.moreOptions = (this.moreOptions || []).concat(pluginOptions);
            if (this.ctx.visit.id > 0) {
              if (options.length > 0) {
                options.push({divider: true});
              }

              options.push({icon: 'history', caption: i18n.msg('audit.trail'), onSelect: this.viewAuditTrail});
            }
          }
        );
    }
  }
}
</script>
