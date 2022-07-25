<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="dataCtx.statement.id >= 0">
          <span v-t="{path: 'common.update', args: {name: dataCtx.statement.statement + ' (' + dataCtx.statement.code + ')'}}"></span>
        </h3>
        <h3 v-else>
          <span v-t="'consents.create'"></span>
        </h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="stmtForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <os-button primary :label="$t(!dataCtx.statement.id ? 'common.buttons.create' : 'common.buttons.update')"
            @click="saveOrUpdate" />
          <os-button text :label="$t('common.buttons.cancel')" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertSvc       from '@/common/services/Alerts.js';
import consentStmtSvc from '@/biospecimen/services/ConsentStatement.js';
import i18n           from '@/common/services/I18n.js';
import routerSvc      from '@/common/services/Router.js';
import util           from '@/common/services/Util.js';

export default {
  props: ['statementId'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('ConsentStatementsList', {}), label: i18n.msg('consents.list')}
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
          alertSvc.success({code: toSave.id ? 'consents.updated' : 'consents.created', args: statement});
        }
      );
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
