<template>
  <router-view :dp="dp" v-if="dp.id" />
</template>

<script>

import routerSvc from '@/common/services/Router.js';

export default {
  props: ['dp'],

  created() {
    const route = this.$route.matched[this.$route.matched.length - 1];
    const views  = route.name.split('.');
    if (views.length < 3) {
      routerSvc.goto(views[0] + '.Requirements.List', {dpId: this.dp.id});
    }
  }
}
</script>
