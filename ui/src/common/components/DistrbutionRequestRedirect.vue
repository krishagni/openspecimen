<template>
  <span>Loading</span>
</template>

<script>
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['catalogId', 'requestId'],

  created() {
    routerSvc.goto('CatalogRequestDetail.Overview', {catalogId: this.catalogId, requestId: this.requestId}, {});
  }
}
</script>

