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
      "caption": "Short Title",
      "href": (row, query) => routerSvc.getUrl('DpsListItemDetail.Overview', {dpId: row.rowObject.dp.id}, query)
    },
    {
      "name": "dp.instituteName",
      "caption": "Receiving Institute"
    },
    {
      "name": "dp.defReceivingSiteName",
      "caption": "Receiving Site"
    },
    {
      "name": "dp.principalInvestigator",
      "caption": "Principal Investigator",
      "type": "user"
    },
    {
      "name": "dp.startDate",
      "caption": "Start Date",
      "type": "date"
    },
    {
      "name": "dp.distributedSpecimensCount",
      "caption": "Distributed Specimens"
    }
  ],

  filters: [
    {
      name: 'title',
      type: 'text',
      caption: 'Title'
    },
    {
      name: 'irbId',
      type: 'text',
      caption: 'IRB ID'
    },
    {
      name: 'receivingInstitute',
      type: 'dropdown',
      caption: 'Receiving Institute',
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
      caption: 'Receiving Site',
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
      caption: 'Principal Investigator',
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
      caption: 'Status',
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
