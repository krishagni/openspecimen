<template>
  <router-view :key="'cp_view_' + cpId" v-if="cpId > 0" />
</template>

<script>
import { provide, ref } from 'vue';

import CpViewContext from './CpViewContext.js';

export default {
  props: ['cpId'],

  setup(props) {
    const cpViewCtx = ref();
    cpViewCtx.value = new CpViewContext(+props.cpId);

    provide('cpViewCtx', cpViewCtx);
    return { cpViewCtx }
  },

  watch: {
    'cpId': function(newVal, oldVal) {
      if (newVal == oldVal || this.cpViewCtx.cpId == newVal) {
        return;
      }

      this.cpViewCtx = new CpViewContext(+newVal);
    }
  }
}
</script>
