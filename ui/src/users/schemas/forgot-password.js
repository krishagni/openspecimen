export default {
  layout: {
    rows: [
      {
        fields: [
          {
            name: 'userDetail.userId',
            type: 'text',
            'md-type': true,
            placeholder: 'Email Address / Login Name',
            validations: {
              required: {
                message: 'Email Address / Login Name is mandatory'
              }
            }
          }
        ]
      }
    ]
  }
}
