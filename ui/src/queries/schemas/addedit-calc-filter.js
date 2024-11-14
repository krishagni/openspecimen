export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'textarea',
            name: 'filter.expr',
            labelCode: 'queries.expression',
            validations: {
              required: {
                messageCode: 'queries.expression_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'booleanCheckbox',
            name: 'filter.parameterized',
            inlineLabelCode: 'queries.parameterized'
          }
        ]
      },
      {
        fields: [
          {
            type: 'text',
            name: 'filter.desc',
            labelCode: 'queries.description',
            validations: {
              required: {
                messageCode: 'queries.description_req'
              }
            }
          }
        ]
      }
    ]
  }
}
