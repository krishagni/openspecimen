<template>
  <a :class="buttonClass" :href="link" target="_blank" v-os-tooltip.bottom="$t('common.open_in_new_tab')">
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
  border: 1px solid #ccc;
  color: #666;
  padding: 3px 7px 3px;
  line-height: inherit;
  display: inline-block;
  border-radius: 50%;
  height: 1.75rem;
  width: 1.75rem;
}

a:hover {
  background: #ddd;
  border: 1px solid #a5a5a5;
}
</style>
