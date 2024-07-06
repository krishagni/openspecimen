export default {
  layout: {
    rows: [
      {
        fields: [
          {
            labelCode: 'forms.title',
            name: 'form.caption',
            type: 'text',
            validations: {
              required: {
                messageCode: 'form.title_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            labelCode: 'forms.name',
            name: 'form.name',
            type: 'text',
            validations: {
              required: {
                messageCode: 'form.name_req'
              }
            }
          }
        ]
      }
    ]
  }
}
