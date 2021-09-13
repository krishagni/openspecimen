
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

export default {
  columns: [
    {
      "name": "institute.name",
      "caption": "Name",
      "href": (row) => routerSvc.getUrl(
        'InstitutesList',
        {},
        {filters: util.uriEncode({name: row.rowObject.institute.name, exactMatch: true})}
      )
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
