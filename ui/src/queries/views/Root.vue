<template>
  <router-view />
</template>

<script>
import formsCache from '@/queries/services/FormsCache.js';

export default {
  mounted() {
    formsCache.init();
  },

  unmounted() {
    formsCache.destroy();
  }
}
</script>
