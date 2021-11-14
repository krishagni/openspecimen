import ui from '@/global.js';

export default {
  columns: [
    {
      "name": "container.name",
      "caption": "Name",
      "href": (row) => ui.ngServer + '#/containers/' + row.rowObject.container.id + '/overview'
    },
    {
      "name": "containerDimension",
      "caption": "Dimension",
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
      "caption": "Parent Container",
      "type": "storage-position"
    },
    {
      "name": "specimensCount",
      "caption": "Specimens"
    },
    {
      "name": "receivedQuality",
      "caption": "Quality"
    }
  ]
}
