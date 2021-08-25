
<template>
  <div>
    <router-view :object-id="user.id"
      :forms="ctx.forms"
      :records="ctx.records"
      @reload-records="reloadRecords"
      v-if="ctx.forms && ctx.records"
    />
  </div>
</template>

<script>

import {reactive, watchEffect} from 'vue';
import userSvc from '@/administrative/services/User.js';
import formUtil from '@/common/services/FormUtil.js';

export default {
  props: ['user'],

  setup(props) {
    let ctx = reactive({ });

    watchEffect(
      () => {
        let formsQ = userSvc.getForms(props.user);
        let recsQ  = userSvc.getFormRecords(props.user);
        Promise.all([formsQ, recsQ]).then(
          (data) => {
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
      userSvc.getFormRecords(this.user).then(
        (records) => {
          this.ctx.records = records;
          formUtil.relinkFormRecords(this.ctx.forms, this.ctx.records);
        }
      );
    }
  }
}

</script>
