<template>
  <router-view :cp="cp" :key="'cp_view_' + cp.id" v-if="cp.id > 0" />
</template>

<script>

import { provide, ref } from 'vue';

import cpSvc from '../services/CollectionProtocol.js';

export default {
  props: ['cpId'],

  async setup(props) {
    const cp = ref();
    provide('cp', cp);

    cp.value = await cpSvc.getCpById(props.cpId);
    return { cp }
  }
}
</script>
