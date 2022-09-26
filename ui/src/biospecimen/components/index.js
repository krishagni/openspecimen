import PpidList from './PpidList.vue';

import fieldFactory from '@/common/services/FieldFactory.js';

export default {
  install(app) {
    app.component('os-ppid-list', PpidList);

    fieldFactory.registerComponent('ppid-list', 'os-ppid-list');
  }
}
