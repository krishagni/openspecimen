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
      ({rateList: {cpsCount, servicesCount}}) => i18n.msg('lab_services.services') + ': ' + (servicesCount || 0) + ' | ' +
        i18n.msg('lab_services.cps') + ': ' + (cpsCount || 0)
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
      type: 'text',
      labelCode: 'lab_services.currency',
      name: 'rateList.currency'
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
    },
    {
      type: 'dropdown',
      labelCode: 'lab_services.cp',
      name: 'cpShortTitle',
      listSource: {
        apiUrl: 'collection-protocols',
        displayProp: 'shortTitle',
        selectProp: 'shortTitle',
        searchProp: 'query'
      }
    },
    {
      type: 'date',
      dateOnly: true,
      labelCode: 'lab_services.effective_on',
      name: 'rateEffectiveOn'
    },
    {
      type: 'dropdown',
      labelCode: 'lab_services.service',
      name: 'serviceCode',
      listSource: {
        apiUrl: 'lab-services',
        displayProp: ({code, description}) => code + ' (' + description + ')',
        selectProp: 'code',
        searchProp: 'query'
      }
    }
  ]
}
