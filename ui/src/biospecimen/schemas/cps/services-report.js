export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'datePicker',
            dateOnly: true,
            name: 'rptCriteria.startDate',
            labelCode: 'cps.start_date',
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
            name: 'rptCriteria.endDate',
            labelCode: 'cps.end_date'
          }
        ]
      }
    ]
  }
}

