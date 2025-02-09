<template>
  <span>Loading</span>
</template>

<script>
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['catalogId', 'requestId'],

  created() {
    routerSvc.ngGoto('specimen-requests/' + this.requestId + '/overview?catalogId=' + this.catalogId);
  }
}
</script>

