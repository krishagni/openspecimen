<template>
  <a :class="buttonClass" :href="link" target="_blank" v-os-tooltip.bottom="'Open record in a new tab'">
    <os-icon name="external-link-alt" :size="iconSize" />
  </a>
</template>

<script>

import routerSvc  from '@/common/services/Router.js';

export default {
  props: ['route', 'size'],

  computed: {
    link: function() {
      const route = this.route || {};
      return routerSvc.getUrl(route.name, route.params, route.query);
    },

    iconSize: function() {
      return this.size == 'small' ? '85%' : '';
    },

    buttonClass: function() {
      return this.size == 'small' ? 'btn btn-xs' : 'btn';
    }
  }
}
</script>

<style scoped>

a {
  background: #f5f5f5!important;
  border: 1px solid #ccc;
  color: #666;
  border-radius: 3px;
  padding: 1px 6px;
  line-height: inherit;
  display: inline-block;
}

</style>
