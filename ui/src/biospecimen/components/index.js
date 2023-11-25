import CpList                  from './CpList.vue';
import CpeList                 from './CpeList.vue';
import EventDescription        from './EventDescription.vue';
import PpidList                from './PpidList.vue';
import SpecimenRequirementList from './SpecimenRequirementList.vue';
import VisitList               from './VisitList.vue';
import VisitSpecimenCollectionStats from './VisitSpecimenCollectionStats.vue';
import VisitSpecimenUtilisationStats from './VisitSpecimenUtilisationStats.vue';

import fieldFactory from '@/common/services/FieldFactory.js';

export default {
  install(app) {
    app.component('os-cp-list',    CpList);
    app.component('os-cpe-list',   CpeList);
    app.component('os-ppid-list',  PpidList);
    app.component('os-sr-list',    SpecimenRequirementList);
    app.component('os-visit-list', VisitList);

    app.component('os-visit-event-desc', EventDescription);
    app.component('os-visit-specimen-collection-stats', VisitSpecimenCollectionStats);
    app.component('os-visit-specimen-utilisation-stats', VisitSpecimenUtilisationStats);

    fieldFactory.registerComponent('cp-list',    'os-cp-list');
    fieldFactory.registerComponent('cpe-list',   'os-cpe-list');
    fieldFactory.registerComponent('ppid-list',  'os-ppid-list');
    fieldFactory.registerComponent('sr-list',    'os-sr-list');
    fieldFactory.registerComponent('visit-list', 'os-visit-list');
  }
}
