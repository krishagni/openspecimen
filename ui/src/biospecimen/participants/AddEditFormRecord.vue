<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="bcrumb" />
      </template>

      <span>
        <h3>
          <span v-show="!recordId" v-t="'common.add_record'">Add Record</span>
          <span v-show="!!recordId" v-t="'common.update_record'">Update Record</span>
          <span>: {{ctx.formDef.caption}}</span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-addedit-form-record :entity="cpr" :form-def="ctx.formDef" :form-id="formId"
        :form-ctxt-id="formCtxtId" :record-id="recordId" :hide-panel="true" :show-next="!!this.ctx.nextForm"
        @saved="saved" @cancelled="saveCancelled" />
    </os-page-body>
  </os-page>
</template>

<script>

import alertsSvc  from '@/common/services/Alerts.js';
import formSvc    from '@/forms/services/Form.js';
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['cpr', 'formId', 'formCtxtId', 'recordId'],

  inject: ['cpViewCtx'],

  data() {
    return {
      ctx: {
        cp: {},

        formDef: {}
      }
    };
  },

  async created() {
    this._setupForm();
  },

  computed: {
    bcrumb: function() {
      const cp = this.ctx.cp;
      if (!cp) {
        return [];
      }

      const {cpId, id: cprId} = this.cpr;
      return [
        {
          url: routerSvc.getUrl('ParticipantsList', {cpId, cprId: -1}),
          label: cp.shortTitle
        },
        {
          url: routerSvc.getUrl('ParticipantsListItemDetail.Overview', {cpId, cprId}),
          label: this.cpr.ppid
        }
      ];
    }
  },

  watch: {
    '$props.formId': function() {
      this._setupForm();
    }
  },

  methods: {
    saved: function(record) {
      alertsSvc.success({
        code: 'common.form_record_saved',
        args: {recordId: record.id, formCaption: this.ctx.formDef.caption}
      });

      if (record.nextForm) {
        this._nextForm(this.ctx.nextForm);
      } else {
        this._navToOverview(this.cpr, record);
      }
    },

    saveCancelled: function() {
      this._navToOverview(this.cpr);
    },

    _setupForm: async function() {
      formSvc.getDefinition(this.formId).then(formDef => this.ctx.formDef = formDef);
      this.cpViewCtx.getCp().then(
        async (cp) => {
          this.ctx.cp = cp
          if (!this.recordId) {
            if (!this.forms) {
              this.forms = await this.cpViewCtx.getParticipantForms({cp: cp, cpr: this.cpr});
            }

            let nextForm = undefined;
            let cfFound  = false; // current form found?
            for (let i = 0; i < this.forms.length - 1; ++i) {
              let f = this.forms[i];
              let nf = this.forms[i + 1];
              if (!cfFound && f.formId == this.formId && f.formCtxtId == this.formCtxtId) {
                cfFound = true;
              }

              if (cfFound && !nf.sysForm && (nf.multiRecord || nf.records.length == 0)) {
                nextForm = nf;
                break;
              }
            }

            this.ctx.nextForm = nextForm;
          }
        }
      );
    },

    _navToOverview: function({cpId, id: cprId}, {containerId: formId, id: recordId}) {
      if (cprId > 0) {
        routerSvc.goto('ParticipantsListItemDetail.Forms', {cpId, cprId}, {formId, recordId});
      } else {
        routerSvc.goto('ParticipantsList', {cpId, cprId: -1});
      }
    },

    _nextForm: function({formId, formCtxtId}) {
      const route = routerSvc.getCurrentRoute();
      routerSvc.goto(route.name, route.params, {formId, formCtxtId});
    }
  }
}
</script>
