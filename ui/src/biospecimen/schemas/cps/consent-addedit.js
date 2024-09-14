export default {
  layout: {
    rows: [
      {
        fields: [
          {
            name: "tier.statementCode",
            type: "dropdown",
            labelCode: "cps.consent_statement",
            listSource: {
              apiUrl: "consent-statements",
              selectProp: "code",
              displayProp: ({statement, code}) => statement + ' (' + code + ')',
              searchProp: "searchString"
            },
            validations: {
              required: {
                messageCode: "cps.consent_statement_req"
              }
            }
          }
        ]
      }
    ]
  }
}
