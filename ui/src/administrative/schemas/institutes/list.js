
import routerSvc from '@/common/services/Router.js';

export default {
  summary: {
    title: {
      text: "institute.name",
      url:  (ro) => routerSvc.getUrl('InstituteDetail.Overview', {instituteId: ro.institute.id}),
    }
  },

  columns: [
    {
      "name": "institute.name",
      "caption": "Name",
      "href": (row) => routerSvc.getUrl('InstituteDetail.Overview', {instituteId: row.rowObject.institute.id})
    },
    {
      "name": "institute.usersCount",
      "caption": "Users"
    }
  ],

  filters: [
    {
      name: 'name',
      type: 'text',
      caption: 'Name'
    }
  ]
}
