import cartSvc   from '@/biospecimen/services/SpecimenCart.js';
import routerSvc from '@/common/services/Router.js';

export default {
  summary: {
    title: {
      text: ({cart}) => cartSvc.getDisplayName(cart),
      url: ({cart}, query) => routerSvc.getUrl('CartSpecimensList', {cartId: cart.id}, query)
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
      captionCode: "carts.name",
      value: ({cart}) => cartSvc.getDisplayName(cart),
      href: (row, query) => routerSvc.getUrl('CartSpecimensList', {cartId: row.rowObject.cart.id}, query)
    },
    {
      name: "cart.owner",
      captionCode: "common.created_by",
      type: "user"
    },
    {
      name: "cart.createdOn",
      captionCode: "common.creation_date",
      type: "date"
    },
    {
      name: "cart.lastUpdatedOn",
      captionCode: "common.last_updated",
      type: "date"
    },
    {
      name: "cart.specimenCount",
      captionCode: "carts.specimens"
    }
  ],

  filters: [
    {
      name: "name",
      type: "text",
      captionCode: "carts.name"
    },
    {
      name: 'folderId',
      type: 'dropdown',
      captionCode: 'carts.folder',
      listSource: {
        apiUrl: 'specimen-list-folders',
        displayProp: 'name',
        selectProp: 'id'
      }
    },
  ]
}
