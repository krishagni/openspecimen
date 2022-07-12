
import routerSvc from '@/common/services/Router.js';
import instituteSvc from '@/administrative/services/Institute.js';

export default {
  columns: [
    {
      "name": "name",
      "captionCode": "user_groups.name",
      "href": (row) => routerSvc.getUrl('UsersList', {userId: -1}, {groupId: row.rowObject.id})
    },
    {
      "name": "description",
      "captionCode": "user_groups.description",
    },
    {
      "name": "institute",
      "captionCode": "user_groups.institute",
    },
    {
      "name": "noOfUsers",
      "captionCode": "user_groups.users"
    }
  ],

  filters: [
    {
      name: 'query',
      type: 'text',
      captionCode: 'user_groups.name'
    },
    {
      name: 'institute',
      type: 'dropdown',
      captionCode: 'user_groups.institute',
      listSource: {
        displayProp: 'name',
        selectProp: 'name',
        loadFn: (opts) => {
          opts = opts || {};
          opts.name = opts.query;
          return instituteSvc.getInstitutes(opts);
        }
      }
    }
  ]
}
