
import routerSvc    from '@/common/services/Router.js';
import instituteSvc from '@/administrative/services/Institute.js';

export default {
  summary: {
    "title": {
      "text": "site.name",
      "url": (ro, query) => routerSvc.getUrl('SitesListItemDetail.Overview', {siteId: ro.site.id}, query)
    },

    "descriptions": [
      "site.type",
      "site.instituteName"
    ]
  },

  columns: [
    {
      "name": "site.name",
      "captionCode": "sites.name",
      "href": (row, query) => routerSvc.getUrl('SitesListItemDetail.Overview', {siteId: row.rowObject.site.id}, query)
    },
    {
      "name": "site.type",
      "captionCode": "sites.type",
    },
    {
      "name": "site.code",
      "captionCode": "sites.code",
    },
    {
      "name": "site.instituteName",
      "captionCode": "sites.institute",
    },
    {
      "name": "site.cpCount",
      "captionCode": "sites.collection_protocols"
    }
  ],

  filters: [
    {
      name: 'name',
      type: 'text',
      captionCode: 'sites.name'
    },
    {
      name: 'institute',
      type: 'dropdown',
      captionCode: 'sites.institute',
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
