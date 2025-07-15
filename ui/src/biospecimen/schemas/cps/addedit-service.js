
export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'text',
            labelCode: 'cps.code',
            name: 'service.code',
            validations: {
              required: {
                messageCode: 'cps.code_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'textarea',
            labelCode: 'cps.description',
            name: 'service.description',
            validations: {
              required: {
                messageCode: 'cps.description_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'datePicker',
            dateOnly: true,
            labelCode: 'cps.start_date',
            name: 'service.rateStartDate',
            validations: {
              requiredIf: {
                expr: 'service.rateEndDate || service.rate',
                messageCode: 'cps.start_date_req'
              }
            },
            showWhen: '!service.id'
          }
        ]
      },
      {
        fields: [
          {
            type: 'datePicker',
            dateOnly: true,
            labelCode: 'cps.end_date',
            name: 'service.rateEndDate',
            showWhen: '!service.id'
          }
        ]
      },
      {
        fields: [
          {
            type: 'number',
            maxFractionDigits: 2,
            labelCode: 'cps.service_rate',
            name: 'service.rate',
            validations: {
              requiredIf: {
                expr: 'service.rateStartDate || service.rateEndDate',
                messageCode: 'cps.service_rate_req'
              }
            },
            showWhen: '!service.id'
          }
        ]
      }
    ]
  }
}
