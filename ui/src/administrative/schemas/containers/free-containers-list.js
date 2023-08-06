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
      href: (row) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: row.rowObject.container.freezerId})
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
      href: (row) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: row.rowObject.container.id})
    },
    {
      name: "container.typeName",
      captionCode: "containers.type"
    },
    {
      name: "container.usedPositions",
      captionCode: "containers.occupied_places"
    },
    {
      name: "container.freePositions",
      captionCode: "containers.free_places"
    }
  ]
}
