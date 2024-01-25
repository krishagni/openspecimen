
<template>
  <div>
    <pre>{{params}}</pre>
  </div>
</template>

<script>
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['params'],

  created() {
    routerSvc.goto(this.params.vueView, this.params, this.params);
  }
}
</script>
