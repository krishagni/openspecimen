<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-t="'participants.bulk_update'">Bulk Edit Participants</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-bulk-update :dict="ctx.dict" object-prefix="cpr." :exclusion-list="ctx.exclusionList"
        @update="update($event)" @cancel="cancel" />
    </os-page-body>
  </os-page>
</template>

<script>
import alertsSvc from '@/common/services/Alerts.js';
import cprSvc    from '@/biospecimen/services/Cpr.js';
import itemsSvc  from '@/common/services/ItemsHolder.js';
import routerSvc from '@/common/services/Router.js';

const EXCLUSION_LIST = [
  'cpr.ppid',
  'cpr.externalSubjectId',
  'cpr.participant.firstName',
  'cpr.participant.lastName',
  'cpr.participant.middleName',
  'cpr.participant.emailAddress',
  'cpr.participant.birthDate',
  'cpr.participant.deathDate',
  'cpr.participant.empi',
  'cpr.participant.uid',
  'cpr.participant.pmis'
];

export default {
  inject: ['cpViewCtx'],

  data() {
    const cp = this.cpViewCtx.getCp();
    return {
      ctx: {
        bcrumb: [ 
          {url: routerSvc.getUrl('ParticipantsList'), label: cp.shortTitle}
        ],

        dict: [],

        exclusionList: EXCLUSION_LIST
      }
    }
  },

  created() {
    const cp = this.cpViewCtx.getCp();
    cprSvc.getDict(cp.id).then(dict => this.ctx.dict = dict);

    this.participants = itemsSvc.getItems('participants');
    itemsSvc.clearItems('participants');
  },

  methods: {
    update: function(detail) {
      const cprIds = this.participants.map(({id}) => id);
      cprSvc.bulkUpdate(cprIds, detail.cpr).then(
        savedCprs => {
          const msg = savedCprs.length == 1 ? 'participants.one_updated' : 'participants.bulk_updated';
          alertsSvc.success({code: msg, args: {count: savedCprs.length}});
          this.cancel();
        }
      );
    },

    cancel: function() {
      const cp = this.cpViewCtx.getCp();
      routerSvc.goto('ParticipantsList', {cpId: cp.id});
    }
  }
}
</script>
