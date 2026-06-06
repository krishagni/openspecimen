export default {
  fields: [
    {
      type: "dropdown",
      name: "cpg.requestManagerGroup",
      labelCode: "cpgs.request_manager_group",
      listSource: {
        apiUrl: "request-manager-groups",
        displayProp: "name",
        searchProp: "query"
      }
    }
  ],

  layout: {
    rows: [
      {
        fields: [ { name: "cpg.requestManagerGroup" } ]
      }
    ]
  }
}
