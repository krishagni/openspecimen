<template>
  <os-button left-icon="link" :size="size" @click="copyLink" v-os-tooltip.bottom="$t('common.copy_record_url')" />
</template>

<script>

import alertSvc   from '@/common/services/Alerts.js';
import routerSvc  from '@/common/services/Router.js';
import settingSvc from '@/common/services/Setting.js';
import util       from '@/common/services/Util.js';

export default {
  props: ['route', 'size'],

  methods: {
    copyLink: async function() {
      const route   = this.route || {};
      const href    = routerSvc.getUrl(route.name, route.params, route.query);
      const setting = await settingSvc.getSetting('common', 'app_url');

      let appUrl = setting[0].value;
      if (!appUrl.endsWith('/')) {
        appUrl += '/';
      }

      util.toClipboard(appUrl + 'ui-app/' + href);
      alertSvc.success({code: 'common.url_copied'});
    }
  }
}
</script>
