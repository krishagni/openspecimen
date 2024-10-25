export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'text',
            labelCode: 'queries.title',
            name: 'query.title',
            validations: {
              required: {
                messageCode: 'queries.title_req'
              }
            }
          }
        ]
      }
    ]
  }
}
