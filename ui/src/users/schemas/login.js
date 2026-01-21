
export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'dropdown',
            name: 'loginDetail.domainName',
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
            type: 'text',
            name: 'loginDetail.loginName',
            'md-type': true,
            placeholder: 'Login Name',
            validations: {
              required: {
                message: 'Login Name is mandatory'
              }
            },
            showWhen: '!!loginDetail.domainName && !externalAuth'
          }
        ]
      },
      {
        fields: [
          {
            type: 'password',
            name: 'loginDetail.password',
            'md-type': true,
            placeholder: 'Password',
            validations: {
              required: {
                message: 'Password is mandatory'
              }
            },
            showWhen: '!!loginDetail.domainName && !externalAuth'
          }
        ]
      },
      {
        fields: [
          {
            type: 'text',
            name: 'loginDetail.props.otp',
            'md-type': true,
            placeholder: 'OTP',
            validations: {
              required: {
                message: 'OTP is mandatory'
              }
            },
            showWhen: '!!loginDetail.domainName && otpAuthEnabled && !externalAuth'
          }
        ]
      },
    ]
  }
}
