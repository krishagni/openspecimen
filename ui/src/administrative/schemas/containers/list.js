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
      name: "container.name",
      caption: "Name",
      href: (row) => routerSvc.getUrl('ContainerDetail.Overview', {containerId: row.rowObject.container.id})
    },
    {
      name: "container.siteName",
      caption: "Site",
    },
    {
      name: "container.createdBy",
      caption: "Created By",
      type: "user"
    },
    {
      name: "container.dimension",
      caption: "Dimension",
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
      caption: "Stored Specimens"
    },
    {
      name: "container.capacity",
      caption: "Approximate Capacity"
    },
    {
      name: "container.utilisation",
      caption: "Utilisation",
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
      caption: "Name"
    },
    {
      name: 'site',
      type: 'site',
      caption: 'Site',
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
