export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: "textarea",
            name: "publish.changes",
            labelCode: "cps.summary_of_changes",
            rows: 5,
            validations: {
              required: {
                messageCode: "cps.summary_of_changes_req"
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: "textarea",
            name: "publish.reason",
            labelCode: "cps.reason",
            rows: 5,
            validations: {
              required: {
                messageCode: "cps.reason_req"
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: "user",
            multiple: true,
            name: "publish.reviewers",
            labelCode: "cps.reviewers",
            validations: {
              required: {
                messageCode: "cps.reviewers_req"
              }
            }
          }
        ]
      }
    ]
  }
}
