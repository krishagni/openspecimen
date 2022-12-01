import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "containerName",
      captionCode: "containers.name",
      value: function({container}) {
        if (container.displayName) {
          return container.displayName + ' (' + container.name + ')';
        }

        return container.name;
      },
      href: (row) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: row.rowObject.container.id})
    },
    {
      name: "container.siteName",
      captionCode: "containers.site",
    },
    {
      "name": "container.storageLocation",
      "captionCode": "containers.parent_container",
      "type": "storage-position"
    },
    {
      name: "container.dimension",
      captionCode: "containers.dimension",
      value: function(rowObject) {
        if (rowObject.container.positionLabelingMode != 'NONE') {
          return rowObject.container.noOfRows + ' x ' + rowObject.container.noOfColumns;
        } else {
          return '';
        }
      }
    }
  ]
}
