import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "pickList.name",
      captionCode: "carts.name",
      href: ({rowObject: {pickList}}, query) => routerSvc.getUrl('PickList', {cartId: pickList.cartId, listId: pickList.id}, query)
    },
    {
      name: "pickList.creator",
      captionCode: "common.created_by",
      type: "user"
    },
    {
      name: "pickList.creationTime",
      captionCode: "common.creation_date",
      type: "date"
    },
    {
      name: "pickListProgress",
      captionCode: "carts.progress",
      type: "component",
      component: "os-progress-bar",
      data: ({pickList}) => ({
        value: pickList.pickedSpecimens,
        total: pickList.totalSpecimens,
        tooltip: window.osSvc.i18nSvc.msg('carts.picklist_progress', pickList)
      })
    }
  ],

  filters: [
    {
      name: "name",
      type: "text",
      captionCode: "carts.name"
    }
  ]
}
