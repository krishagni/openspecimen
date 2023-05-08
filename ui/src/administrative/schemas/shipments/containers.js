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
        let allowedCps = (container.allowedCollectionProtocols || []).join(', ');
        return !allowedCps.trim() ? '-' : allowedCps;
      }
    },
    {
      "name": "allowedTypes",
      "captionCode": "shipments.container_specimen_types",
      "type": "span",
      "value": ({container}) => {
        let classTypes = [].concat(container.allowedSpecimenClasses || []);
        classTypes.push.apply(classTypes, container.allowedSpecimenTypes || []);
        let result = classTypes.join(', ');
        return !result.trim() ? '-' : result;
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
