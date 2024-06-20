<template>
  <os-page-toolbar>
    <template #default>
      <os-button left-icon="edit" :label="$t('common.buttons.edit')"
        @click="edit" v-if="isUpdateAllowed" />

      <os-button left-icon="print" :label="$t('common.buttons.print')"
        @click="printLabels" v-if="isPrintSpecimenLabelsAllowed" />

      <os-button left-icon="trash" :label="$t('common.buttons.delete')"
        @click="deleteCpr" v-if="isDeleteAllowed" />

      <os-menu :label="$t('common.buttons.more')" :options="ctx.moreOptions" />
    </template>
  </os-page-toolbar>

  <os-grid>
    <os-grid-column width="12">
      <os-overview :schema="dict" :object="ctx" v-if="dict.length > 0" />

      <os-section v-if="ctx.cpr.participant && ctx.cpr.participant.registeredCps.length > 0">
        <template #title>
          <span v-t="'participants.other_registered_cps'">Other Registered Protocols</span>
        </template>

        <template #content>
          <ul class="os-other-protocols">
            <li v-for="{cprId, cpId, cpShortTitle} in ctx.cpr.participant.registeredCps" :key="cprId">
              <router-link :to="{name: 'ParticipantsListItemDetail.Overview', params: {cpId, cprId}}">
                <span>{{cpShortTitle}}</span>
              </router-link>
            </li>
          </ul>
        </template>
      </os-section>

      <os-section v-if="hasOccurredVisits">
        <template #title>
          <span v-t="'participants.occurred_visits'">Visits</span>
        </template>

        <template #content>
          <OccurredVisits :cp="ctx.cp" :cpr="ctx.cpr" :visits="ctx.visits" :dict="ctx.visitDict" />
        </template>
      </os-section>

      <os-section v-if="hasMissedVisits">
        <template #title>
          <span v-t="'participants.missed_or_not_collected_visits'">Visits</span>
        </template>

        <template #content>
          <MissedVisits :cp="ctx.cp" :cpr="ctx.cpr" :visits="ctx.visits" :dict="ctx.visitDict" />
        </template>
      </os-section>

      <os-section v-if="hasPendingVisits">
        <template #title>
          <span v-t="'participants.pending_visits'">Visits</span>
        </template>

        <template #content>
          <PendingVisits :cp="ctx.cp" :cpr="ctx.cpr" :visits="ctx.visits" :dict="ctx.visitDict" />
        </template>
      </os-section>
    </os-grid-column>

    <os-delete-object ref="deleteCprDialog" :input="ctx.deleteOpts" />

    <os-audit-trail ref="auditTrailDialog" :objects="ctx.auditObjs" />

    <os-plugin-views ref="moreMenuPluginViews" page="participant-detail" view="more-menu" :viewProps="ctx" />

    <os-dialog ref="addToAnotherCpDialog">
      <template #header>
        <span v-t="'participants.add_to_another_cp'"></span>
      </template>
      <template #content>
        <os-form ref="addToAnotherCpForm" :schema="anotherRegCtx.formSchema" :data="anotherRegCtx" />
      </template>
      <template #footer>
        <os-button text :label="$t('common.buttons.cancel')" @click="cancelAnotherCpReg" />
        <os-button primary :label="$t('common.buttons.proceed')" @click="proceedToAnotherCpReg" />
      </template>
    </os-dialog>
  </os-grid>
</template>

<script>
import formUtil from '@/common/services/FormUtil.js';
import routerSvc from '@/common/services/Router.js';
import util from '@/common/services/Util.js';

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import cprSvc from '@/biospecimen/services/Cpr.js';
import visitSvc from '@/biospecimen/services/Visit.js';
import specimenSvc from '@/biospecimen/services/Specimen.js';

import MissedVisits   from './MissedVisits.vue';
import OccurredVisits from './OccurredVisits.vue';
import PendingVisits  from './PendingVisits.vue';

export default {
  props: ['cpr'],

  components: {
    MissedVisits,
    OccurredVisits,
    PendingVisits
  },

  inject: ['cpViewCtx'],

  data() {
    const cp = this.cpViewCtx.getCp();
    return {
      ctx: {
        cp,

        cpr: {},

        consents: null,

        dict: [],

        auditObjs: [],

        visits: [],

        routeQuery: this.$route.query,

        visitDict: undefined,

        userRole: null,

        moreOptions: []
      },

      anotherRegCtx: {
        cpId: null,

        defCpsList: null,

        formSchema: this._getAddToAnotherCpFs()
      }
    };
  },

  created() {
    Promise.all([
      this.cpViewCtx.getCprDict(),
      this.cpViewCtx.getConsentsDict(),
      this.cpViewCtx.getVisitDict(),
      this.cpViewCtx.getRole()
    ]).then(
      ([dict, consentsDict, visitDict, userRole]) => {
        Object.assign(this.ctx, {dict, consentsDict, visitDict, userRole});
        this._setupCpr();
        this._loadMoreMenuOptions();
      }
    );
  },

  computed: {
    dict: function() {
      const result = [];
      Array.prototype.push.apply(result, this.ctx.dict || []);
      Array.prototype.push.apply(result, this.ctx.consentsDict || []);
      return result;
    },

    hasOccurredVisits: function() {
      return (this.ctx.visits || []).some(visit => visit.status == 'Complete');
    },

    hasMissedVisits: function() {
      return (this.ctx.visits || []).some(visit => visit.status == 'Missed Collection' || visit.status == 'Not Collected');
    },

    hasPendingVisits: function() {
      return (this.ctx.visits || []).some(visit => !visit.status || visit.status == 'Pending');
    },

    isUpdateAllowed: function() {
      return this.cpViewCtx.isUpdateParticipantAllowed(this.cpr);
    },

    isDeleteAllowed: function() {
      return this.cpViewCtx.isDeleteParticipantAllowed(this.cpr);
    },

    isPrintSpecimenLabelsAllowed: function() {
      return this.cpViewCtx.isPrintSpecimenAllowed(this.cpr) && !this.cpViewCtx.isCoordinator();
    }
  },

  watch: {
    cpr: function(newVal, oldVal) {
      if (newVal != oldVal) {
        this._setupCpr();
        this._loadMoreMenuOptions();
      }
    }
  },

  methods: {
    edit: function() {
      const cpr = this.ctx.cpr;
      routerSvc.goto('ParticipantAddEdit', {cpId: cpr.cpId, cprId: cpr.id});
    },

    printLabels: function() {
      const cpr = this.ctx.cpr;
      const ts = util.formatDate(new Date(), 'yyyyMMdd_HHmmss');
      const outputFilename = [cpr.cpShortTitle, cpr.ppid, ts].join('_') + '.csv';
      specimenSvc.printLabels({cprId: cpr.id}, outputFilename);
    },

    deleteCpr: function() {
      this.$refs.deleteCprDialog.execute().then(
        (resp) => {
          if (resp == 'deleted') {
            routerSvc.goto('ParticipantsList', {cprId: -2}, this.ctx.routeQuery);
          }
        }
      );
    },

    addToAnother: function() {
      this.$refs.addToAnotherCpDialog.open();
    },

    proceedToAnotherCpReg: function() {
      if (!this.$refs.addToAnotherCpForm.validate()) {
        return;
      }

      const {cpId} = this.anotherRegCtx;
      const {participant: {id: participantId}} = this.cpr;
      routerSvc.goto('ParticipantAddEdit', {cpId, cprId: -1}, {participantId});
    },

    cancelAnotherCpReg: function() {
      this.$refs.addToAnotherCpDialog.close();
    },

    viewAuditTrail: function() {
      this.$refs.auditTrailDialog.open();
    },

    _setupCpr: function() {
      const cpr = this.ctx.cpr = this.cpr;

      this.ctx.auditObjs = [
        {objectId: cpr.id, objectName: 'collection_protocol_registration'},
        {objectId: cpr.participant.id, objectName: 'participant'}
      ]

      let name = '';
      if (cpr.participant.firstName && cpr.participant.firstName != '###') {
        name = cpr.participant.firstName;
      }

      if (cpr.participant.lastName && cpr.participant.lastName != '###') {
        if (name) {
          name += ' ';
        }

        name += cpr.participant.lastName;
      }

      name = cpr.ppid + (name ? ' (' + name + ')' : '');
      this.ctx.deleteOpts = {
        type: this.$t('participants.cpr'),
        title: name,
        dependents: () => cprSvc.getDependents(cpr),
        forceDelete: true,
        askReason: true,
        deleteObj: (reason) => cprSvc.deleteCpr(cpr.id, true, reason)
      };

      if (this.ctx.consentsDict && this.ctx.consentsDict.length > 0 && this.cpViewCtx.isReadConsentAllowed(cpr)) {
        cprSvc.getConsents(cpr).then(
          consent => {
            const codedResps = this.ctx.consents = {};
            if (consent) {
              for (let resp of consent.responses || []) {
                codedResps[resp.code] = resp.response;
              }
            }
          }
        );
      } else {
        this.ctx.consents = null;
      }

      if (!this.cpViewCtx.isReadVisitAllowed(cpr)) {
        return;
      }

      const eventRulesQ = this.cpViewCtx.getAnticipatedEventsRules();
      const visitsQ = visitSvc.getVisits(this.cpr);
      Promise.all([eventRulesQ, visitsQ]).then(
        ([eventRules, visits]) => {
          const allowedEvents = cprSvc.getAllowedEvents(this.ctx.cpr, eventRules);
          this.ctx.visits = (visits || [])
            .filter(
              visit => {
                if (visit.status && visit.status != 'Pending') {
                  return true;
                }

                return !allowedEvents || !visit.eventCode || allowedEvents.indexOf(visit.eventCode) != -1;
              }
            );

          for (let visit of this.ctx.visits) {
            formUtil.createCustomFieldsMap(visit, true);
          }
        }
      );
    },

    _getAddToOtherCps: async function({query}) {
      if (!this.query) {
        if (this.anotherRegCtx.defCpsList) {
          return this.anotherRegCtx.defCpsList;
        }
      }

      const {participant: {pmis, registeredCps}} = this.ctx.cpr;
      const preRegisteredCps = (registeredCps || []).map(({cpShortTitle}) => cpShortTitle);
      preRegisteredCps.push(this.ctx.cp.shortTitle);

      let mrnSites = [];
      if (this.cpViewCtx.isAccessBasedOnMrnSite()) {
        mrnSites = (pmis || []).map(({siteName}) => siteName);
      }

      return cpSvc.getCpsForRegistrations(mrnSites, query).then(
        cps => {
          cps = cps.filter(cp => preRegisteredCps.indexOf(cp.shortTitle) == -1);
          if (!this.query) {
            this.anotherRegCtx.defCpsList = cps;
          }

          return cps;
        }
      );
    },

    _loadMoreMenuOptions: async function() {
      const ctxt = this.pluginViewProps = {...this.ctx, cpViewCtx: this.cpViewCtx};
      const moreOptions = [
        {icon: 'plus', caption: this.$t('participants.add_to_another_cp'), onSelect: this.addToAnother}
      ];
      util.getPluginMenuOptions(this.$refs.moreMenuPluginViews, 'participant-detail', 'more-menu', ctxt)
        .then(
          pluginOptions => {
            const options = this.ctx.moreOptions = moreOptions.concat(pluginOptions);
            if (options.length > 0) {
              options.push({divider: true});
            }

            options.push({icon: 'history', caption: this.$t('audit.trail'), onSelect: this.viewAuditTrail});
          }
        );
    },

    _getAddToAnotherCpFs: function() {
      return {
        rows: [{
          fields: [
            {
              type: 'dropdown',
              labelCode: 'participants.select_cp',
              name: 'cpId',
              listSource: {
                selectProp: 'id',
                displayProp: 'shortTitle',
                loadFn: ({query, maxResults}) => this._getAddToOtherCps({query, maxResults})
              },
              validations: {
                required: {
                  messageCode: 'participants.cp_req'
                }
              }
            }
          ]
        }]
      }
    }
  }
}
</script>

<style scoped>
.os-other-protocols {
  list-style: none;
  padding: 0;
  margin-bottom: 0;
}

.os-other-protocols li {
  display: inline-block;
}

.os-other-protocols li:not(:last-child):after {
  content: ', ';
  display: inline-block;
}
</style>
