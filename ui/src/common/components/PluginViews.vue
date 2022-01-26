
<template>
  <component
    :page="page"
    :view="view"
    :query="query"
    :is="pluginView.component"
    v-bind="viewProps"
    v-for="pluginView of views"
    :key="pluginView.name"
  />
</template>

<script>

import pluginViewsReg from '@/common/services/PluginViewsRegistry.js';

export default {
  props: ['page', 'view', 'query', 'viewProps'],

  computed: {
    views: function() {
      return pluginViewsReg.getViews(this.page, this.view);
    }
  }
}

</script>
