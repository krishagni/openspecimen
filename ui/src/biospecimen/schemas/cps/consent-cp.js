export default {
  layout: {
    rows: [
      {
        fields: [
          {
            name: "cp",
            type: "dropdown",
            labelCode: "cps.select_consent_cp",
            listSource: {
              apiUrl: "collection-protocols",
              displayProp: "shortTitle",
              searchProp: "query"
            },
            validations: {
              required: {
                messageCode: "cps.select_consent_cp_req"
              }
            }
          }
        ]
      }
    ]
  }
}
