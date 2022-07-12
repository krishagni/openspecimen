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
      "value": (rowObject) => {
        let container = rowObject.container;
        if (container.positionLabelingMode != 'NONE') {
          return container.noOfRows + ' x ' + container.noOfColumns;
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
