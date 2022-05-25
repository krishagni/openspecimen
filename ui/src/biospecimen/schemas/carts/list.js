import cartSvc   from '@/biospecimen/services/SpecimenCart.js';
import routerSvc from '@/common/services/Router.js';

export default {
  summary: {
    title: {
      text: ({cart}) => cartSvc.getDisplayName(cart),
      url: ({cart}, query) => routerSvc.getUrl('SpecimenCartsList', {cartId: cart.id}, query)
    },

    descriptions: [
      ({cart}) => {
        const filters = window.osUiApp.config.globalProperties.$filters || {};
        return filters.username(cart.owner);
      },

      ({cart}) => {
        const filters = window.osUiApp.config.globalProperties.$filters || {};
        return filters.date(cart.createdOn) + ' | ' + filters.date(cart.lastUpdatedOn) + ' | ' + cart.specimenCount;
      }
    ]
  },

  columns: [
    {
      name: "cart.starred",
      caption: "",
      type: "component",
      component: "os-star",
      data: ({cart}) => ({starred: cart.starred})
    },
    {
      name: "cartDisplayName",
      caption: "Name",
      value: ({cart}) => cartSvc.getDisplayName(cart),
      href: (row) => '#/placeholder/' + row.rowObject.cart.id
    },
    {
      name: "cart.owner",
      caption: "Created By",
      type: "user"
    },
    {
      name: "cart.createdOn",
      caption: "Creation Date",
      type: "date"
    },
    {
      name: "cart.lastUpdatedOn",
      caption: "Last Updated",
      type: "date"
    },
    {
      name: "cart.specimenCount",
      caption: "Specimens"
    }
  ],

  filters: [
    {
      name: "name",
      type: "text",
      caption: "Name"
    },
    {
      name: 'folderId',
      type: 'dropdown',
      caption: 'Folder',
      listSource: {
        apiUrl: 'specimen-list-folders',
        displayProp: 'name',
        selectProp: 'id'
      }
    },
  ]
}
