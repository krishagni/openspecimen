
import routerSvc    from '@/common/services/Router.js';

export default {
  summary: {
    "title": {
      "text": "role.name",
      "url": (ro, query) => routerSvc.getUrl('UserRolesListItemDetail.Overview', {roleId: ro.role.id}, query)
    },

    "descriptions": [
      "role.description"
    ]
  },

  columns: [
    {
      "name": "role.name",
      "captionCode": "roles.name",
      "href": (row, query) => routerSvc.getUrl('UserRolesListItemDetail.Overview', {roleId: row.rowObject.role.id}, query)
    },
    {
      "name": "role.description",
      "captionCode": "roles.description",
    }
  ],

  filters: [
    {
      name: 'name',
      type: 'text',
      captionCode: 'roles.name'
    }
  ]
}
