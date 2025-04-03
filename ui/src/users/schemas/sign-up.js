export default {
  layout: {
    rows: [
      {
        fields: [
          {
            name: 'userDetail.firstName',
            type: 'text',
            'md-type': true,
            placeholder: 'First Name',
            validations: {
              required: {
                message: 'First Name is Mandatory'
              }
            }
          },
          {
            name: 'userDetail.lastName',
            type: 'text',
            'md-type': true,
            placeholder: 'Last Name',
            validations: {
              required: {
                message: 'Last Name is Mandatory'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            name: 'userDetail.emailAddress',
            type: 'text',
            'md-type': true,
            placeholder: 'Email Address',
            validations: {
              required: {
                message: 'Email Address is Mandatory'
              }
            }
          },
          {
            name: 'userDetail.phoneNumber',
            type: 'text',
            'md-type': true,
            placeholder: 'Phone Number'
          }
        ]
      },
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
          },
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
      },
      {
        fields: [
          {
            type: 'textarea',
            name: 'userDetail.address',
            'md-type': true,
            placeholder: 'Address'
          }
        ]
      }
    ]
  }
}
