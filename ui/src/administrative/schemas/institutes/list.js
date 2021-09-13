
import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      "name": "institute.name",
      "caption": "Name",
      "href": (row) => routerSvc.getUrl('InstituteOverview', {instituteId: row.rowObject.institute.id})
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
