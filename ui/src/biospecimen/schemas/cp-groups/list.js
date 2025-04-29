import routerSvc from '@/common/services/Router.js';

export default {
  summary: {
    title: {
      text: ({cpg}) => cpg.name + ' (' + cpg.cpsCount + ')',
      url: ({cpg}, query) => routerSvc.getUrl('CpgDetail.Overview', {cpgId: cpg.id}, query)
    },

    descriptions: [ ]
  },

  columns: [
    {
      name: "cpg.name",
      captionCode: "cpgs.name",
      href: (row, query) => routerSvc.getUrl('CpgDetail.Overview', {cpgId: row.rowObject.cpg.id}, query)
    },
    {
      name: "cpg.cpsCount",
      captionCode: "cpgs.cps_count"
    }
  ],

  filters: [
    {
      name: "query",
      type: "text",
      captionCode: "cpgs.name"
    },
    {
      name: 'cpShortTitle',
      type: 'dropdown',
      captionCode: 'cpgs.collection_protocol',
      listSource: {
        apiUrl: 'collection-protocols',
        displayProp: 'shortTitle',
        selectProp: 'shortTitle',
        searchProp: 'query'
      }
    }
  ]
}
