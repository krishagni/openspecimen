<template>
  <span class="os-cp-draft-marker" v-if="cp.draftMode">
    <span class="text" v-t="'participants.draft_watermark'">DRAFT</span>
  </span>
  <router-view :key="$route.params.cpId" v-if="cp.id > 0" />
</template>

<script>
import { reactive } from 'vue';

import cpSvc from '@/biospecimen/services/CollectionProtocol.js';
import settingSvc from '@/common/services/Setting.js';
import util from '@/common/services/Util.js';

import CpViewContext from './CpViewContext.js';

export default {
  props: ['cpId'],

  async setup(props) {
    const dbCp = await cpSvc.getCpById(+props.cpId);
    const cp = reactive(dbCp);

    const settings = await settingSvc.getSetting('biospecimen', 'mrn_restriction_enabled')
    const accessBasedOnMrn = util.isTrue(settings[0].value);

    const cpViewCtx = reactive(new CpViewContext(cp, accessBasedOnMrn));
    return { cp, cpViewCtx };
  },

  provide() {
    return {cpViewCtx: this.cpViewCtx};
  },

  watch: {
    'cpId': async function(newVal, oldVal) {
      if (newVal == oldVal || this.cpViewCtx.cpId == newVal) {
        return;
      }

      const cp = this.cp = await cpSvc.getCpById(+newVal);

      const settings = await settingSvc.getSetting('biospecimen', 'mrn_restriction_enabled')
      const accessBasedOnMrn = util.isTrue(settings[0].value);

      this.cpViewCtx = new CpViewContext(cp, accessBasedOnMrn);
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
