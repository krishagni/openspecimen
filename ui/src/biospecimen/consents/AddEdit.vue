<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="dataCtx.statement.id >= 0">{{dataCtx.statement.statement}} ({{dataCtx.statement.code}})</h3>
        <h3 v-else>Create Consent</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="stmtForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <os-button primary :label="!dataCtx.statement.id ? 'Create' : 'Update'" @click="saveOrUpdate" />
          <os-button text label="Cancel" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertSvc       from '@/common/services/Alerts.js';
import consentStmtSvc from '@/biospecimen/services/ConsentStatement.js';
import routerSvc      from '@/common/services/Router.js';
import util           from '@/common/services/Util.js';

export default {
  props: ['statementId'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('ConsentStatementsList', {}), label: 'Consents'}
      ],

      addEditFs: {rows: []}
    });

    let dataCtx = reactive({
      statement: {},

      currentUser: ui.currentUser,
    });

    const { schema } = consentStmtSvc.getAddEditFormSchema();
    ctx.addEditFs = schema;

    if (props.statementId && +props.statementId >= 0) {
      consentStmtSvc.getStatement(props.statementId).then(statement => dataCtx.statement = statement);
    }

    return { ctx, dataCtx };
  },

  methods: {
    handleInput: function({data}) {
      Object.assign(this.dataCtx, data);
    },

    saveOrUpdate: async function() {
      if (!this.$refs.stmtForm.validate()) {
        return;
      }

      const toSave = util.clone(this.dataCtx.statement);
      consentStmtSvc.saveOrUpdate(toSave).then(
        (statement) => {
          routerSvc.goto('ConsentStatementsList', {})
          alertSvc.success('Consent ' + statement.code + (toSave.id ? ' updated.' : ' created.'));
        }
      );
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
