import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "freezerName",
      captionCode: "containers.freezer",
      value: function({container}) {
        if (container.freezerDisplayName) {
          return container.freezerDisplayName + ' (' + container.freezerName + ')';
        }

        return container.freezerName;
      },
      href: (row, query) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: row.rowObject.container.freezerId}, query)
    },
    {
      name: "containerName",
      captionCode: "containers.name",
      value: function({container}) {
        if (container.displayName) {
          return container.displayName + ' (' + container.name + ')';
        }

        return container.name;
      },
      href: (row, query) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: row.rowObject.container.id}, query)
    },
    {
      name: "container.typeName",
      captionCode: "containers.type"
    },
    {
      name: "container.usedPositions",
      captionCode: "containers.occupied_locations"
    },
    {
      name: "container.freePositions",
      captionCode: "containers.free_locations"
    }
  ]
}
