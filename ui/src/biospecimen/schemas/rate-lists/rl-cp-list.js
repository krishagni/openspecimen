import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      type: 'text',
      labelCode: 'lab_services.cp',
      name: 'cp.shortTitle',
      href: (row) => routerSvc.getUrl('CpDetail.Overview', {cpId: row.rowObject.cp.id})
    },
    {
      type: 'text',
      labelCode: 'lab_services.code',
      name: 'cp.code'
    },
    {
      type: 'user',
      labelCode: 'lab_services.cp_pi',
      name: 'cp.principalInvestigator'
    },
    {
      type: 'date',
      labelCode: 'lab_services.start_date',
      name: 'cp.startDate'
    }
  ],

  filters: [
    {
      type: 'text',
      labelCode: 'lab_services.cp',
      name: 'query'
    },
    {
      type: 'user',
      selectProp: 'id',
      labelCode: 'lab_services.cp_pi',
      name: 'piId'
    },
    {
      type: 'site',
      listSource: { selectProp: 'name' },
      labelCode: 'lab_services.site',
      name: 'site'
    }
  ]
}
