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
      caption: "Name",
      href: (row, query) =>  routerSvc.getUrl('ContainerTypesListItemDetail.Overview', {typeId: row.rowObject.type.id}, query)
    },
    {
      name: "type.nameFormat",
      caption: "Name Format",
    },
    {
      name: "type.dimension",
      caption: "Dimension",
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
      caption: "Name"
    },
    {
      name: 'canHold',
      type: 'dropdown',
      caption: 'Can Hold',
      listSource: {
        apiUrl: 'container-types',
        selectProp: 'name',
        displayProp: 'name',
        searchProp: 'name'
      }
    }
  ]
}
