
import routerSvc from '@/common/services/Router.js';

export default {
  summary: {
    title: {
      text: "institute.name",
      url:  (ro, query) => routerSvc.getUrl('InstitutesListItemDetail.Overview', {instituteId: ro.institute.id}, query),
    }
  },

  columns: [
    {
      "name": "institute.name",
      "captionCode": "institutes.name",
      "href": (row, query) => routerSvc.getUrl('InstitutesListItemDetail.Overview', {instituteId: row.rowObject.institute.id}, query)
    },
    {
      "name": "institute.usersCount",
      "captionCode": "institutes.users"
    }
  ],

  filters: [
    {
      "name": "name",
      "type": "text",
      "captionCode": "institutes.name"
    }
  ]
}
