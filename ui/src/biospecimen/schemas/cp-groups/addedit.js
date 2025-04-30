export default {
  layout: {
    rows: [
      {
        fields: [
          {
            name: "cpg.name",
            type: "text",
            labelCode: "cpgs.name",
            validations: {
              required: {
                messageCode: "cpgs.name_req"
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            name: "cpg.cps",
            type: "multiselect",
            labelCode: "cpgs.collection_protocols",
            listSource: {
              apiUrl: "collection-protocols",
              displayProp: "shortTitle",
              searchProp: "query"
            },
            validations: {
              required: {
                messageCode: "cpgs.cps_req"
              }
            }
          }
        ]
      }
    ]
  }
}
