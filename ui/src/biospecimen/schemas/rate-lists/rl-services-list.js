import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      type: 'text',
      labelCode: 'lab_services.code',
      name: 'service.serviceCode',
      href: ({rowObject: {service}}) => routerSvc.getUrl('LabServicesList', {}, {serviceId: service.serviceId})
    },
    {
      type: 'text',
      labelCode: 'lab_services.description',
      name: 'service.serviceDescription',
      href: ({rowObject: {service}}) => routerSvc.getUrl('LabServicesList', {}, {serviceId: service.serviceId})
    },
    {
      type: 'number',
      labelCode: 'lab_services.rate',
      name: 'service.rate',
      value: ({rateList, service}) => rateList.currency + ' ' + service.rate
    }
  ]
}
