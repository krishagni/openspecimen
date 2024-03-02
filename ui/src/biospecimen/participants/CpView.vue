<template>
  <span class="os-cp-draft-marker" v-if="cp.draftMode">
    <span class="text" v-t="'participants.draft_watermark'">DRAFT</span>
  </span>
  <router-view :key="$route.params.cpId" />
</template>

<script>
import { provide, reactive, ref } from 'vue';

import CpViewContext from './CpViewContext.js';

export default {
  props: ['cpId'],

  setup(props) {
    const cpViewCtx = ref();
    const ctx = cpViewCtx.value = new CpViewContext(+props.cpId);

    const cp = reactive({});
    ctx.getCp().then(p => Object.assign(cp, p));

    provide('cpViewCtx', cpViewCtx);
    return { cp, cpViewCtx }
  },

  watch: {
    'cpId': function(newVal, oldVal) {
      if (newVal == oldVal || this.cpViewCtx.cpId == newVal) {
        return;
      }

      this.cpViewCtx = new CpViewContext(+newVal);
      this.cpViewCtx.getCp().then(cp => this.cp = cp);
    }
  }
}
</script>

<style scoped>
.os-cp-draft-marker {
  position: relative;
  display: block;
}

.os-cp-draft-marker .text {
  position: absolute;
  left: 50%;
  right: 0;
  top: 1.25rem;
  text-align: center;
  transform: rotate(-10deg);
  border: 5px solid red;
  width: 7rem;
  border-radius: 1.25rem;
  font-size: 1.25rem;
  color: red;
  opacity: 0.65;
  z-index: 0;
}
</style>
