
export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'span',
            labelCode: 'cps.code',
            name: 'service.code'
          }
        ]
      },
      {
        fields: [
          {
            type: 'span',
            labelCode: 'cps.description',
            name: 'service.description'
          }
        ]
      },
      {
        fields: [
          {
            type: 'datePicker',
            dateOnly: true,
            labelCode: 'cps.start_date',
            name: 'rate.startDate',
            validations: {
              required: {
                messageCode: 'cps.start_date_req'
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
            labelCode: 'cps.end_date',
            name: 'rate.endDate'
          }
        ]
      },
      {
        fields: [
          {
            type: 'number',
            maxFractionDigits: 2,
            labelCode: 'cps.service_rate',
            name: 'rate.rate',
            validations: {
              required: {
                messageCode: 'cps.service_rate_req'
              }
            }
          }
        ]
      }
    ]
  }
}
