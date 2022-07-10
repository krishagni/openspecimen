import routerSvc from '@/common/services/Router.js';

export default {
  summary: {
    title: {
      text: (ro) => ro.type.name,
      url: (ro, query) => routerSvc.getUrl('ContainerTypesListItemDetail.Overview', {typeId: ro.type.id}, query)
    },

    descriptions: [
      "type.nameFormat",

      ({type}) => {
        if (type.positionLabelingMode != 'NONE') {
          return type.noOfRows + ' x ' + type.noOfColumns;
        } else {
          return '';
        }
      }
    ]
  },

  columns: [
    {
      name: "type.name",
      captionCode: "container_types.name",
      href: (row, query) =>  routerSvc.getUrl('ContainerTypesListItemDetail.Overview', {typeId: row.rowObject.type.id}, query)
    },
    {
      name: "type.nameFormat",
      captionCode: "container_types.name_format"
    },
    {
      name: "type.dimension",
      captionCode: "container_types.dimension",
      value: function({type}) {
        if (type.positionLabelingMode != 'NONE') {
          return type.noOfRows + ' x ' + type.noOfColumns;
        } else {
          return '';
        }
      }
    }
  ],

  filters: [
    {
      name: "name",
      type: "text",
      captionCode: "container_types.name"
    },
    {
      name: 'canHold',
      type: 'dropdown',
      captionCode: 'container_types.can_hold',
      listSource: {
        apiUrl: 'container-types',
        selectProp: 'name',
        displayProp: 'name',
        searchProp: 'name'
      }
    }
  ]
}
