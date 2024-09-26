export default {
  fields: [
    {
      type: "multiselect",
      name: "cp.distributionProtocols",
      labelCode: "cps.distribution_protocols",
      listSource: {
        displayProp: "shortTitle",
        apiUrl: "distribution-protocols",
        searchProp: "query",
        queryParams: {
          static: {
            activityStatus: "Active",
            excludeExpiredDps: true
          },
          dynamic: {
            cp: "cp.shortTitle"
          }
        }
      }
    }
  ]
}
