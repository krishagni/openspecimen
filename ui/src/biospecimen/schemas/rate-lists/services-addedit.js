export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'text',
            labelCode: 'lab_services.code',
            name: 'service.code',
            validations: {
              required: {
                messageCode: 'lab_services.code_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'textarea',
            labelCode: 'lab_services.description',
            name: 'service.description',
            validations: {
              required: {
                messageCode: 'lab_services.description_req'
              }
            }
          }
        ]
      }
    ]
  }
}
