export default {
  columns: [
    {
      type: 'dropdown',
      labelCode: 'lab_services.service',
      name: 'serviceRate.service',
      listSource: {
        apiUrl: 'lab-services',
        searchProp: 'query',
        displayProp: (option) => option.description + ' (' + option.code + ')',
        queryParams: {
          dynamic: {
            notInRateListId: 'rateList.id'
          }
        }
      },
      validations: {
        required: {
          messageCode: 'lab_services.service_req'
        }
      },
      uiStyle: {
        'width': '550px'
      },
      showWhen: '!editServices'
    },
    {
      type: 'span',
      labelCode: 'lab_services.service',
      name: 'serviceRate.service.description',
      showWhen: 'editServices'
    },
    {
      type: 'number',
      maxFractionDigits: 2,
      labelCode: 'lab_services.rate',
      name: 'serviceRate.rate',
      unit: ({rateList}) => rateList.currency,
      validations: {
        required: {
          messageCode: 'lab_services.rate_req'
        }
      }
    }
  ]
}
