
<template>
  <div>
    <os-message v-if="ctx.loading">
      <span>Loading forms and records. Please wait for a moment...</span>
    </os-message>

    <os-message v-if="!ctx.loading && ctx.forms && ctx.forms.length == 0">
      <span>No forms to show!</span>
    </os-message>

    <router-view :entity="ctx.entity"
      :forms="ctx.forms"
      :records="ctx.records"
      :list-view="listView"
      :add-edit-view="addEditView"
      @reload-records="reloadRecords"
      v-if="!ctx.loading && ctx.forms && ctx.forms.length > 0 && ctx.records"
    />
  </div>
</template>

<script>

import {reactive, watchEffect} from 'vue';

import userResources from '@/administrative/users/Resources.js';
import userSvc from '@/administrative/services/User.js';
import formUtil from '@/common/services/FormUtil.js';

export default {
  props: ['user', 'entityType', 'listView', 'addEditView'],

  setup(props) {
    let ctx = reactive({ });

    watchEffect(
      () => {
        ctx.entity = {
          isActive: props.user.activityStatus == 'Active',
          isUpdateAllowed: userResources.isFormUpdateAllowed(props.entityType, props.user.id),
          entity: props.user,
          id: props.user.id,
          entityType: props.entityType,
          loading: true
        };

        let formsQ = userSvc.getForms(props.user, props.entityType);
        let recsQ  = userSvc.getFormRecords(props.user, props.entityType);
        Promise.all([formsQ, recsQ]).then(
          (data) => {
            ctx.loading = false;
            ctx.forms   = data[0];
            ctx.records = data[1];
            formUtil.relinkFormRecords(ctx.forms, ctx.records);
          }
        );
      }
    );

    return { ctx };
  },

  methods: {
    reloadRecords: function() {
      this.ctx.records = undefined;
      userSvc.getFormRecords(this.user, this.entityType).then(
        (records) => {
          this.ctx.records = records;
          formUtil.relinkFormRecords(this.ctx.forms, this.ctx.records);
        }
      );
    }
  }
}

</script>
