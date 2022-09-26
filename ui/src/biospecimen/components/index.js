import PpidList  from './PpidList.vue';
import VisitList from './VisitList.vue';

import fieldFactory from '@/common/services/FieldFactory.js';

export default {
  install(app) {
    app.component('os-ppid-list', PpidList);
    app.component('os-visit-list', VisitList);

    fieldFactory.registerComponent('ppid-list', 'os-ppid-list');
    fieldFactory.registerComponent('visit-list', 'os-visit-list');
  }
}
