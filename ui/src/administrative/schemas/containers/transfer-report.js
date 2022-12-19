export default {
  layout: {
    rows: [
      {
        fields: [
          {
            name: "criteria.name",
            labelCode: "containers.freezer",
            type: "multiselect",
            listSource: {
              apiUrl: "storage-containers",
              searchProp: "name",
              displayProp: "name",
              selectProp: "name",
              valueProp: "naam",
              queryParams: {
                static: {
                  topLevelContainers: true,
                  status: ['AVAILABLE', 'CHECKED_OUT']
                }
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            name: "criteria.cp",
            labelCode: "containers.cps",
            type: "multiselect",
            listSource: {
              apiUrl: "collection-protocols",
              displayProp: "shortTitle",
              selectProp: "shortTitle",
            }
          }
        ]
      },
      {
        fields: [
          {
            name: "criteria.type",
            labelCode: "containers.types",
            type: "multiselect",
            listSource: {
              apiUrl: "container-types",
              displayProp: "name",
              selectProp: "name",
              searchProp: "name"
            }
          }
        ]
      },
      {
        fields: [
          {
            name: "criteria.fromDate",
            labelCode: "containers.from_date",
            type: "datePicker",
            showWhen: "showDateRange"
          }
        ]
      },
      {
        fields: [
          {
            name: "criteria.toDate",
            labelCode: "containers.to_date",
            type: "datePicker",
            showWhen: "showDateRange"
          }
        ]
      }
    ]
  }
}
