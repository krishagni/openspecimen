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
      <os-addedit-form-record :entity="object" :form-def="ctx.formDef" :form-id="formId"
        :form-ctxt-id="formCtxtId" :record-id="recordId" :hide-panel="true" :show-next="!!ctx.nextForm"
        @saved="saved" @cancelled="saveCancelled" />
    </os-page-body>
  </os-page>
</template>

<script>

import alertsSvc  from '@/common/services/Alerts.js';
import formSvc    from '@/forms/services/Form.js';
import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['object', 'formId', 'formCtxtId', 'recordId', 'api'],

  data() {
    return {
      ctx: {
        formDef: {}
      },

      bcrumb: this.api.getBreadcrumb()
    };
  },

  async created() {
    this._setupForm();
  },

  computed: {
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
        this._navToOverview(record);
      }
    },

    saveCancelled: function() {
      this._navToOverview({});
    },

    _setupForm: async function() {
      formSvc.getDefinition(this.formId).then(formDef => this.ctx.formDef = formDef);
      if (!this.recordId) {
        if (!this.forms) {
          this.forms = await this.api.getForms();
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
    },

    _navToOverview: function({containerId: formId, id: recordId}) {
      this.api.gotoOverview(formId, recordId);
    },

    _nextForm: function({formId, formCtxtId}) {
      const route = routerSvc.getCurrentRoute();
      routerSvc.goto(route.name, route.params, {formId, formCtxtId});
    }
  }
}
</script>
