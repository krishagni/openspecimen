
import routerSvc from '@/common/services/Router.js';
import instituteSvc from '@/administrative/services/Institute.js';

export default {
  columns: [
    {
      "name": "name",
      "caption": "Name",
      "href": (row) => routerSvc.getUrl('UsersList', {userId: -1}, {groupId: row.rowObject.id})
    },
    {
      "name": "description",
      "caption": "Description",
    },
    {
      "name": "institute",
      "caption": "Institute",
    },
    {
      "name": "noOfUsers",
      "caption": "Users"
    }
  ],

  filters: [
    {
      name: 'query',
      type: 'text',
      caption: 'Name'
    },
    {
      name: 'institute',
      type: 'dropdown',
      caption: 'Institute',
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
