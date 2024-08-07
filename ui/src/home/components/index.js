
import Carts from './Carts.vue';
import CollectionProtocols from './CollectionProtocols.vue';
import Containers from './Containers.vue';
import DistributionProtocols from './DistributionProtocols.vue';
import Orders from './Orders.vue';
import Queries from './Queries.vue';
import Shipments from './Shipments.vue';

import ListCard from './ListCard.vue';

import homePageSvc from '@/common/services/HomePageService.js';

export default {
  install(app) {
    app.component('os-home-list-card', ListCard);

    app.component('os-home-collection-protocols', CollectionProtocols);
    app.component('os-home-container-list', Containers);
    app.component('os-home-dp-list', DistributionProtocols);
    app.component('os-home-order-list', Orders);
    app.component('os-home-query-list', Queries);
    app.component('os-home-shipment-list', Shipments);
    app.component('os-home-specimen-lists', Carts);

    homePageSvc.registerWidgets(
      [
        {
          name: 'collection-protocols',
          showIf: {resources: ['ParticipantPhi', 'ParticipantDeid'], operations: ['Read']}
        },
        {
          name: 'specimen-lists',
          showIf: {resources: ['Specimen', 'PrimarySpecimen'], operations: ['Read']}
        },
        {
          name: 'container-list',
          showIf: {resource: 'StorageContainer', operations: ['Read']}
        },
        {
          name: 'query-list',
          showIf: {resource: 'Query', operations: ['Read']}
        },
        {
          name: 'dp-list',
          showIf: {resource: 'DistributionProtocol', operations: ['Read']}
        },
        {
          name: 'order-list',
          showIf: {resource: 'Order', operations: ['Read']}
        },
        {
          name: 'shipment-list',
          showIf: {resource: 'ShippingAndTracking', operations: ['Read']}
        }
      ]
    );
  }
}

