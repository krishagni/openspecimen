import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {   
      type: 'text',
      labelCode: 'lab_services.rate_list',
      name: 'rateList',
      value: ({rateList}) => '#' + rateList.id + ' ' + rateList.name,
      href: ({rowObject: {rateList}}) => routerSvc.getUrl('RateListsItemDetail.Overview', {rateListId: rateList.id})
    },  
    {
      type: 'text',
      labelCode: 'lab_services.description',
      name: 'rateList.description'
    },
    {
      type: 'date',
      labelCode: 'lab_services.start_date',
      name: 'rateList.startDate'
    },
    {
      type: 'date',
      labelCode: 'lab_services.end_date',
      name: 'rateList.endDate'
    },
    { 
      type: 'number',
      labelCode: 'lab_services.rate',
      name: 'rateList.serviceRate'
    }
  ]
}
