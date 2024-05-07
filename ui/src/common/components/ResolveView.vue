
<template>
  <div>
    <pre>{{params}}</pre>
  </div>
</template>

<script>
import routerSvc from '@/common/services/Router.js';
import util from '@/common/services/Util.js';

export default {
  props: ['params'],

  created() {
    const params = util.clone(this.params);
    const view = params.vueView;
    delete params.vueView;
    routerSvc.goto(view, params);
  }
}
</script>
