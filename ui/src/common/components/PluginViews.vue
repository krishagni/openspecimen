
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
    @plugin-event="emitPluginEvent"
  />
</template>

<script>

import pluginViewsReg from '@/common/services/PluginViewsRegistry.js';

export default {
  props: ['page', 'view', 'query', 'viewProps', 'viewNames'],

  computed: {
    views: function() {
      let views = [...pluginViewsReg.getViews(this.page, this.view)];
      if (this.viewNames) {
        views = views.filter(view => this.viewNames.includes(view.name));
      }

      views.sort(({name: name1}, {name: name2}) => name1.localeCompare(name2));
      return views;
    },

    bindAttrs: function() {
      const attrs = {};
      Object.assign(attrs, this.$attrs || {});
      return Object.assign(attrs, this.viewProps || {});
    }
  },

  methods: {
    emitPluginEvent(event) {
      if (event && typeof event.eventName == 'string' && event.eventName) {
        this.$emit(event.eventName, event);
      }
    }
  }
}

</script>
