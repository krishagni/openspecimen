import i18n      from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';
import util      from '@/common/services/Util.js';

export default {
  summary: {
    title: {
      text: ({rateList}) => '#' + rateList.id + ' ' + rateList.name,
      url: ({rateList}, query) => routerSvc.getUrl('RateListsItemDetail.Overview', {rateListId: rateList.id}, query)
    },

    descriptions: [
      "rateList.description",
      ({rateList}) => {
        let result = util.formatDate(new Date(rateList.startDate));
        if (rateList.endDate) {
          result += ' | ' + util.formatDate(new Date(rateList.endDate));
        }

        return result;
      },
      ({rateList: {cpsCount, servicesCount}}) => i18n.msg('lab_services.services') + ': ' + servicesCount + ' | ' +
        i18n.msg('lab_services.cps') + ': ' + cpsCount
    ]
  },

  columns: [
    {
      type: 'text',
      labelCode: 'lab_services.name',
      name: 'rateListName',
      value: ({rateList}) => '#' + rateList.id + ' ' + rateList.name,
      href: (row, query) => routerSvc.getUrl('RateListsItemDetail.Overview', {rateListId: row.rowObject.rateList.id}, query)
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
      labelCode: 'lab_services.services',
      name: 'rateList.servicesCount'
    },
    {
      type: 'number',
      labelCode: 'lab_services.cps',
      name: 'rateList.cpsCount'
    }
  ],

  filters: [
    {
      type: 'text',
      labelCode: 'lab_services.id_name_desc',
      name: 'query'
    }
  ]
}
