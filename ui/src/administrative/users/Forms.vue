
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
        userSvc.getForms(props.user).then((forms) => ctx.forms = forms);
        userSvc.getFormRecords(props.user).then((records) => ctx.records = records);
      }
    );

    return { ctx };
  }
}

</script>
