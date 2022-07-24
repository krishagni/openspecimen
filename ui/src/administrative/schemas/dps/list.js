import routerSvc  from '@/common/services/Router.js';
import userFilter from '@/common/filters/User.js';

export default {
  summary: {
    title: {
      text: (ro) => ro.dp.shortTitle,
      url:  (ro, query) => routerSvc.getUrl('DpsListItemDetail.Overview', {dpId: ro.dp.id}, query),
    },

    descriptions: [
      (ro) => userFilter.name(ro.dp.principalInvestigator),

      "dp.instituteName"
    ]
  },

  columns: [
    {
      "name": "dp.shortTitle",
      "captionCode": "dps.short_title",
      "href": (row, query) => routerSvc.getUrl('DpsListItemDetail.Overview', {dpId: row.rowObject.dp.id}, query)
    },
    {
      "name": "dp.instituteName",
      "captionCode": "dps.receiving_institute",
    },
    {
      "name": "dp.defReceivingSiteName",
      "captionCode": "dps.receiving_site",
    },
    {
      "name": "dp.principalInvestigator",
      "captionCode": "dps.principal_investigator",
      "type": "user"
    },
    {
      "name": "dp.startDate",
      "captionCode": "dps.start_date",
      "type": "date"
    },
    {
      "name": "dp.distributedSpecimensCount",
      "captionCode": "dps.distributed_specimens"
    }
  ],

  filters: [
    {
      name: 'title',
      type: 'text',
      captionCode: 'dps.title'
    },
    {
      name: 'irbId',
      type: 'text',
      captionCode: 'dps.ethics_id',
    },
    {
      name: 'receivingInstitute',
      type: 'dropdown',
      captionCode: 'dps.receiving_institute',
      listSource: {
        apiUrl: 'institutes',
        displayProp: 'name',
        selectProp: 'name',
        searchProp: 'name'
      }
    },
    {
      name: 'receivingSite',
      type: 'site',
      captionCode: 'dps.receiving_site',
      listSource: {
        selectProp: 'name',
        queryParams: {
          dynamic: {
            institute: 'receivingInstitute'
          }
        }
      }
    },
    {
      name: 'piId',
      type: 'user',
      captionCode: 'dps.principal_investigator',
      selectProp: 'id',
      listSource: {
        queryParams: {
          static: {
            activityStatus: 'all'
          }
        }
      }
    },
    {
      name: 'activityStatus',
      type: 'dropdown',
      captionCode: 'dps.status',
      listSource: {
        selectProp: 'value',
        displayProp: 'value',
        options: [
          { value: 'Active' },
          { value: 'Closed' },
        ]
      }
    }
  ]
}
