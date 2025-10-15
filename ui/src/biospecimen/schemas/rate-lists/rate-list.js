export default {
  fields:  [
    {
      type: 'text',
      labelCode: 'lab_services.name',
      name: 'rateList.name',
      validations: {
        required: {
          messageCode: 'lab_services.name_req'
        }
      }
    },
    {
      type: 'textarea',
      labelCode: 'lab_services.description',
      name: 'rateList.description',
      validations: {
        required: {
          messageCode: 'lab_services.description_req'
        }
      }
    },
    {
      type: 'datePicker',
      dateOnly: true,
      labelCode: 'lab_services.start_date',
      name: 'rateList.startDate',
      validations: {
        required: {
          messageCode: 'lab_services.start_date_req'
        }
      }
    },
    {
      type: 'datePicker',
      dateOnly: true,
      labelCode: 'lab_services.end_date',
      name: 'rateList.endDate'
    },
    {
      type: 'text',
      labelCode: 'lab_services.currency',
      name: 'rateList.currency',
      validations: {
        required: {
          messageCode: 'lab_services.currency_req'
        }
      }
    }
  ]
}
