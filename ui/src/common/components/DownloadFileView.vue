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

  mounted() {
    this._downloadFile();
    alertSvc.info({code: 'common.file_download_initiated'});
    routerSvc.goto('HomePage');
  },

  methods: {
    _downloadFile() {
      if (!this.downloadUrl) {
        return;
      }

      let link = document.createElement('a');
      link.href = this.downloadUrl;
      link.target = '_blank';
      link.rel = 'noopener';
      link.download = '';
      link.style.display = 'none';

      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  }
}
</script>
