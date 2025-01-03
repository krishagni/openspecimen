
import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "folder.name",
      caption: "Name",
      href: (row) => routerSvc.getUrl('SpecimenCartsList', {cartId: -1}, {folderId: row.rowObject.folder.id})
    },
    {
      name: "folder.description",
      caption: "Description"
    },
    {
      name: "folder.owner",
      caption: "Created By",
      type: "user"
    },
    {
      name: "folder.creationTime",
      caption: "Creation Date",
      type: "date"
    },
    {
      name: "folder.cartsCount",
      caption: "Carts"
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
