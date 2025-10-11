export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'dropdown',
            labelCode: 'specimens.service',
            name: 'serviceDetail.serviceCode',
            listSource: {
              apiUrl: 'lab-services',
              queryParams: {
                dynamic: {
                  cpId: 'serviceDetail.cpId'
                }
              },
              displayProp: (option) => option.description + ' (' + option.code + ') ',
              selectProp: 'code',
              searchProp: 'query'
            },
            validations: {
              required: {
                messageCode: 'specimens.service_req',
              }
            },
            disableWhen: "serviceDetail.id > 0"
          }
        ]
      },
      {
        fields: [
          {
            type: 'datePicker',
            showTime: true,
            labelCode: 'specimens.service_date',
            name: 'serviceDetail.serviceDate',
            validations: {
              required: {
                messageCode: 'specimens.service_date_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'user',
            labelCode: 'specimens.serviced_by',
            name: 'serviceDetail.servicedBy',
            validations: {
              required: {
                messageCode: 'specimens.serviced_by_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'textarea',
            labelCode: 'specimens.comments',
            name: 'serviceDetail.comments'
          }
        ]
      }
    ]
  }
}
