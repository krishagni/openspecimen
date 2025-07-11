export default {
  columns: [
    {
      type: 'text',
      labelCode: 'specimens.service',
      name: 'calcService',
      value: ({service}) => service.serviceDescription + ' (' + service.serviceCode + ')',
      uiStyle: {
        'max-width': '300px'
      }
    },
    {
      type: 'datePicker',
      dateOnly: true,
      labelCode: 'specimens.service_date',
      name: 'service.serviceDate'
    },
    {
      type: 'user',
      labelCode: 'specimens.serviced_by',
      name: 'service.servicedBy'
    },
    {
      type: 'text',
      labelCode: 'specimens.comments',
      name: 'service.comments',
      uiStyle: {
        'max-width': '250px'
      }
    },
    {
      type: 'text',
      labelCode: 'specimens.service_rate',
      name: 'service.serviceRate',
      value: ({currency, service}) => {
        if (service.serviceRate == null || service.serviceRate == undefined) {
          return service.serviceRate;
        }

        return currency ? (currency + ' ' + service.serviceRate) : service.serviceRate;
      }
    }
  ]
}
