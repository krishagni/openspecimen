
<template>
  <div>
    <router-view :forms="ctx.forms" :records="ctx.records" v-if="ctx.forms && ctx.records"> </router-view>
  </div>
</template>

<script>

import {reactive, watchEffect} from 'vue';
import userSvc from '@/administrative/services/User.js';

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

            let fcMap = {};
            ctx.forms.forEach((form) => fcMap[form.formCtxtId] = form);
            ctx.records.forEach(
              (formRecs) => {
                 formRecs.records.forEach(
                   (record) => {
                     let form = fcMap[record.fcId];
                     form.records = form.records || [];
                     form.records.push(record);
                   }
                 );
              }
            );
          }
        );
      }
    );

    return { ctx };
  }
}

</script>
