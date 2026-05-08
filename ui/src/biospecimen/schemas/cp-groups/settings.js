export default {
  fields: [
    {
      "type": "dropdown",
      "name": "cpg.reqManagers",
      "labelCode": "cpgs.request_managers",
      "listSource": {
        "apiUrl": "user-groups",
        "displayProp": "name",
        "searchProp": "query"
      }
    }
  ]
}
