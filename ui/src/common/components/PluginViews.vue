
<template>
  <component
    :page="page"
    :view="view"
    :query="query"
    :is="pluginView.component"
    v-bind="bindAttrs"
    v-for="pluginView of views"
    :key="pluginView.name"
    :ref="pluginView.name"
  />
</template>

<script>

import pluginViewsReg from '@/common/services/PluginViewsRegistry.js';

export default {
  props: ['page', 'view', 'query', 'viewProps'],

  computed: {
    views: function() {
      const views = [...pluginViewsReg.getViews(this.page, this.view)];
      views.sort(({name: name1}, {name: name2}) => name1.localeCompare(name2));
      return views;
    },

    bindAttrs: function() {
      const attrs = {};
      Object.assign(attrs, this.$attrs || {});
      return Object.assign(attrs, this.viewProps || {});
    }
  }
}

</script>
