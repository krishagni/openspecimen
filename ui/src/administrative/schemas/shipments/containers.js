import ui from '@/global.js';

export default {
  columns: [
    {
      "name": "container.name",
      "captionCode": "shipments.container_name",
      "href": (row) => ui.ngServer + '#/containers/' + row.rowObject.container.id + '/overview'
    },
    {
      "name": "containerDimension",
      "captionCode": "shipments.container_dimension",
      "value": ({container}) => {
        if (container.positionLabelingMode != 'NONE') {
          return container.noOfRows + ' x ' + container.noOfColumns;
        }

        return '-';
      }
    },
    {
      "name": "allowedCps",
      "captionCode": "shipments.container_cps",
      "type": "span",
      "value": ({container}) => {
        if (container.usedFor == 'STORAGE') {
          if (container.calcAllowedCollectionProtocols.length > 0) {
            return container.calcAllowedCollectionProtocols.join(', ');
          } else {
            return 'All';
          }
        }

        return '-';
      }
    },
    {
      "name": "allowedTypes",
      "captionCode": "shipments.container_specimen_types",
      "type": "span",
      "value": ({container}) => {
        if (container.usedFor == 'STORAGE') {
          let types = [];
          if (container.calcAllowedSpecimenClasses.length > 0) {
            Array.prototype.push.apply(types, container.calcAllowedSpecimenClasses);
          }

          if (container.calcAllowedSpecimenTypes.length > 0) {
            Array.prototype.push.apply(types, container.calcAllowedSpecimenTypes);
          }

          if (types.length > 0) {
            return types.join(', ');
          }
        }

        return '-';
      }
    },
    {
      "name": "container.storageLocation",
      "captionCode": "shipments.container_parent",
      "type": "storage-position"
    },
    {
      "name": "specimensCount",
      "captionCode": "shipments.specimens"
    },
    {
      "name": "receivedQuality",
      "captionCode": "shipments.received_quality"
    }
  ]
}
