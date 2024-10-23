import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "query.starred",
      caption: "",
      type: "component",
      component: "os-star",
      data: ({query}) => ({starred: query.starred})
    },
    {
      name: "queryName",
      captionCode: "queries.title",
      value: ({query}) => '#' + query.id + ' ' + query.title,
      href: ({rowObject: {query}}) => routerSvc.getUrl('QueryDetail.Results', {queryId: query.id})
    },
    {
      name: "updatedBy",
      type: "user",
      captionCode: "common.updated_by",
      value: ({query}) => {
        const user = query.lastModifiedBy || query.createdBy || {};
        let result = user.firstName;
        if (result && user.lastName) {
          result += ' ';
        }

        if (user.lastName) {
          result += user.lastName;
        }

        return result;
      }
    },
    {
      name: "query.lastModifiedOn",
      type: "date",
      captionCode: "common.update_time"
    }
  ],

  filters: [
    {
      name: "searchString",
      type: "text",
      captionCode: "queries.id_or_title"
    }
  ]
}
