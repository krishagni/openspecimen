import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "container.starred",
      caption: "",
      type: "component",
      component: "os-star",
      data: (rowObject) => ({starred: rowObject.container.starred})
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
      name: "container.siteName",
      captionCode: "containers.site",
    },
    {
      name: "container.createdBy",
      captionCode: "common.created_by",
      type: "user"
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
    },
    {
      name: "container.storedSpecimens",
      captionCode: "containers.stored_specimens"
    },
    {
      name: "container.capacity",
      captionCode: "containers.capacity"
    },
    {
      name: "container.utilisation",
      captionCode: "containers.utilisation",
      type: "component",
      component: "os-utilisation-bar",
      data: function(rowObject) {
        let utilisation = undefined;
        if (rowObject.container.capacity) {
          utilisation = Math.round(rowObject.container.storedSpecimens / rowObject.container.capacity * 100);
        }

        return {utilisation};
      }
    }
  ],

  filters: [
    {
      name: "name",
      type: "text",
      captionCode: "containers.name"
    },
    {
      name: 'site',
      type: 'site',
      captionCode: 'containers.site',
      listSource: {
        selectProp: 'name',
        queryParams: {
          static: {
            resource: 'StorageContainer'
          }
        }
      }
    }
  ]
}
