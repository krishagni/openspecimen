export default {
  layout: {
    rows: [
      {
        fields: [
          {
            name: "specimen.storageLocation",
            type: "storage-position",
            labelCode: "specimens.location",
            showWhen: "!specimen.checkout",
            validations: {
              required: {
                messageCode: "specimens.location_req"
              }
            }
          }
        ]
      },

      {
        fields: [
          {
            name: "specimen.transferUser",
            type: "user",
            labelCode: "specimens.user",
            validations: {
              required: {
                messageCode: "specimens.user_req"
              }
            }
          }
        ]
      },

      {
        fields: [
          {
            name: "specimen.transferTime",
            type: "datePicker",
            showTime: true,
            labelCode: "specimens.time",
            validations: {
              required: {
                messageCode: "specimens.time_req"
              }
            }
          }
        ]
      },

      {
        fields: [
          {
            name: "specimen.transferComments",
            type: "textarea",
            labelCode: "specimens.comments"
          }
        ]
      }
    ]
  }
}
