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
        :form-ctxt-id="formCtxtId" :record-id="recordId" :hide-panel="true"
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
    this.cpViewCtx.getCp().then(cp => this.ctx.cp = cp);
    return {
      ctx: {
        cp: {},

        formDef: {}
      }
    };
  },

  async created() {
    this.ctx.formDef = await formSvc.getDefinition(this.formId);
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

  methods: {
    saved: function(record) {
      alertsSvc.success({
        code: 'common.form_record_saved',
        args: {recordId: record.id, formCaption: this.ctx.formDef.caption}
      });
      this._navToOverview(this.cpr, record);
    },

    saveCancelled: function() {
      this._navToOverview(this.cpr);
    },

    _navToOverview: function({cpId, id: cprId}, {containerId: formId, id: recordId}) {
      if (cprId > 0) {
        routerSvc.goto('ParticipantsListItemDetail.Forms', {cpId, cprId}, {formId, recordId});
      } else {
        routerSvc.back();
      }
    }
  }
}
</script>
