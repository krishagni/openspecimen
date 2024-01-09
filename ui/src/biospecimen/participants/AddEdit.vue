<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3>
          <span v-if="!dataCtx.cpr.id" v-t="'participants.register_participant'">Register Participant</span>
          <span v-else v-t="{path: 'common.update', args: {name}}"></span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="cprForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <os-button primary :label="$t(!dataCtx.cpr.id ? 'participants.add_participant' : 'common.buttons.update')"
            @click="saveOrUpdate" />

          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>

import cprSvc     from '@/biospecimen/services/Cpr.js';

import formUtil   from '@/common/services/FormUtil.js';
import pvSvc      from '@/common/services/PermissibleValue.js';
import routerSvc  from '@/common/services/Router.js';
import util       from '@/common/services/Util.js';

export default {
  props: ['cpr'],

  inject: ['cpViewCtx'],

  data() {
    const copy = util.clone(this.cpr || {});
    if (!copy.id) {
      copy.registrationDate = new Date();
    }

    if (copy.participant.extensionDetail) {
      delete copy.participant.extensionDetail.attrsMap;
    }

    formUtil.createCustomFieldsMap(copy.participant);

    this.cpViewCtx.getCp().then(
      cp => {
        this.dataCtx.cp = cp;
        this.ctx.bcrumb[0].label = cp.shortTitle;
        copy.cpId = cp.id;
      }
    );

    return {
      dataCtx: {
        cpr: copy,

        cp: {}
      },

      ctx: {
        addEditFs: {rows: []},

        bcrumb: [{url: routerSvc.getUrl('ParticipantsList', {cprId: -1}), label: ''}]
      }
    };
  },

  created() {
    const cpr = this.dataCtx.cpr;
    const p = cpr.participant;

    const dictQ   = this.cpViewCtx.getCprDict();
    const layoutQ = this.cpViewCtx.getCprAddEditLayout();
    Promise.all([dictQ, layoutQ]).then(
      (answers) => {
        this.ctx.addEditFs = formUtil.getFormSchema(answers[0], answers[1]);
      }
    );

    pvSvc.getPvs('vital_status', null, {includeOnlyLeafValue: true, includeProps: true}).then(
      (vitalStatuses) => {
        this.deadStatuses = vitalStatuses
          .filter(status => status.props && (status.props.dead == true || status.props.dead == 'true'))
          .map(status => status.value);

        p.dead = this.deadStatuses.indexOf(p.vitalStatus) >= 0;
      }
    );
  },

  computed: {
    name: function() {
      return cprSvc.getFormattedTitle(this.cpr);
    }
  },

  methods: {
    handleInput: function({field, value}) {
      const cpr = this.dataCtx.cpr;
      const p = cpr.participant;
      if (field.name == 'cpr.participant.phoneNumber' && !value) {
        p.textOptIn = false;
      } else if (field.name == 'cpr.participant.emailAddress' && !value) {
        p.emailOptIn = false;
      } else if (field.name == 'cpr.participant.birthDate') {
        if (value) {
          p.birthDateStr = util.formatDate(value, 'yyyy-MM-dd');
        } else {
          p.birthDateStr = null;
        }
      } else if (field.name == 'cpr.participant.vitalStatus') {
        p.dead = this.deadStatuses.indexOf(p.vitalStatus) >= 0;
        if (!p.dead) {
          p.deathDate = null;
        }
      }
    },

    saveOrUpdate: function() {
      if (!this.$refs.cprForm.validate()) {
        return;
      }

      cprSvc.saveOrUpdate(this.dataCtx.cpr).then(
        ({id, cpId}) => routerSvc.goto('ParticipantsListItemDetail.Overview', {cpId, cprId: id})
      );
    }
  }
}
</script>
