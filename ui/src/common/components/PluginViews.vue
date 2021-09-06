
<template>
  <component :is="view.component" v-bind="view" v-for="view of views" :key="view.name" />
</template>

<script>

import pluginViewsReg from '@/common/services/PluginViewsRegistry.js';

export default {
  props: ['page', 'view'],

  computed: {
    views: function() {
      alert('Get views of ' + this.page + ' for ' + this.view);
      alert(JSON.stringify(pluginViewsReg.getViews(this.page, this.view) || []));
      return pluginViewsReg.getViews(this.page, this.view);
    }
  }
}

</script>
