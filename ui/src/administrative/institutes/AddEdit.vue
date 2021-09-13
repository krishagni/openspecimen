<template>
  <os-page>
    <os-page-head>
      <template #breadcrumb>
        <os-breadcrumb :items="ctx.bcrumb" />
      </template>

      <span>
        <h3 v-if="!dataCtx.institute.id">Create Institute</h3>
        <h3 v-else>Update {{dataCtx.institute.name}}</h3>
      </span>
    </os-page-head>

    <os-page-body>
      <os-form ref="instituteForm" :schema="ctx.addEditFs" :data="dataCtx" @input="handleInput($event)">
        <div>
          <os-button :label="!dataCtx.institute.id ? 'Create' : 'Update'" @click="saveOrUpdate" />
          <os-button label="Cancel" @click="cancel" />
        </div>
      </os-form>
    </os-page-body>
  </os-page>
</template>

<script>
import { reactive, inject } from 'vue';

import alertSvc     from '@/common/services/Alerts.js';
import routerSvc    from '@/common/services/Router.js';
import instituteSvc from '@/administrative/services/Institute.js';

export default {
  props: ['instituteId'],

  inject: ['ui'],

  setup(props) {
    const ui = inject('ui');

    let ctx = reactive({
      bcrumb: [
        {url: routerSvc.getUrl('InstitutesList'), label: 'Institutes'}
      ],

      addEditFs: {rows: []}
    });

    let dataCtx = reactive({
      institute: {},

      currentUser: ui.currentUser
    });


    let promises = [ instituteSvc.getAddEditFormSchema() ];
    if (props.instituteId && +props.instituteId > 0) {
      promises.push(instituteSvc.getInstitute(+props.instituteId));
    }

    Promise.all(promises).then(
      function(result) {
        ctx.addEditFs = result[0].schema;
        if (result.length > 1) {
          dataCtx.institute = result[1];
        }
      }
    );

    return { ctx, dataCtx };
  },

  methods: {
    handleInput: function(event) {
      Object.assign(this.dataCtx, event.data);
    },

    saveOrUpdate: function() {
      if (!this.$refs.instituteForm.validate()) {
        return;
      }

      instituteSvc.saveOrUpdate(this.dataCtx.institute).then(
        function(savedInstitute) {
          alertSvc.success('Institute ' + savedInstitute.name + ' saved!');
          routerSvc.goto('InstituteOverview', {instituteId: savedInstitute.id});
        }
      );
    },

    cancel: function() {
      routerSvc.back();
    }
  }
}
</script>
