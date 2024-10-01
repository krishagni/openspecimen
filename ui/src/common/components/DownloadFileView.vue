<template>
  <div>
    <os-message type="info">
      <span>File download initiated...</span>
    </os-message>
  </div>
</template>

<script>
import alertSvc  from '@/common/services/Alerts.js';
import routerSvc from '@/common/services/Router.js';

export default {
  props: ['downloadUrl'],

  created() {
    window.open(this.downloadUrl, '_self');
    alertSvc.info({code: 'common.file_download_initiated'});
    routerSvc.goto('HomePage');
  }
}
</script>
