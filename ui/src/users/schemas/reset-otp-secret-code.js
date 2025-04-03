export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'dropdown',
            name: 'userDetail.domainName',
            listSource: {
              selectProp: 'name',
              displayProp: 'name',
              loadFn: ({context}) => context.formData.getDomains(),
              initUsingSelectProp: true
            },
            'md-type': true,
            placeholder: 'Domain Name',
            validations: {
              required: {
                message: 'Domain Name is mandatory'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            name: 'userDetail.loginName',
            type: 'text',
            'md-type': true,
            placeholder: 'Login Name',
            validations: {
              required: {
                message: 'Login Name is mandatory'
              }
            }
          }
        ]
      }
    ]
  }
}
