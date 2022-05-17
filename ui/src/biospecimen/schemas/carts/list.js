import cartSvc from '@/biospecimen/services/SpecimenCart.js';

export default {
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
    }
  ]
}
