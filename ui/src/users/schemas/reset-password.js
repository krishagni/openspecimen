export default {
  layout: {
    rows: [
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
      },
      {
        fields: [
          {
            name: 'userDetail.newPassword',
            type: 'password',
            'md-type': true,
            placeholder: 'New Password',
            validations: {
              required: {
                message: 'New Password is mandatory'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            name: 'userDetail.confirmPassword',
            type: 'password',
            'md-type': true,
            placeholder: 'Confirm Password',
            validations: {
              required: {
                message: 'Confirm Password is mandatory'
              },
              sameAs: {
                field: 'userDetail.newPassword',
                message: 'Passwords do not match'
              } 
            }
          }
        ]
      }
    ]
  }
}
