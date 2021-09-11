
import routerSvc    from '@/common/services/Router.js';
import instituteSvc from '@/administrative/services/Institute.js';

export default {
  columns: [
    {
      "name": "site.name",
      "caption": "Name",
      "href": (row) => routerSvc.getUrl('SiteOverview', {siteId: row.rowObject.site.id})
    },
    {
      "name": "site.type",
      "caption": "Type",
    },
    {
      "name": "site.code",
      "caption": "Code",
    },
    {
      "name": "site.instituteName",
      "caption": "Institute",
    },
    {
      "name": "site.cpCount",
      "caption": "Collection Protocols"
    }
  ],

  filters: [
    {
      name: 'name',
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
