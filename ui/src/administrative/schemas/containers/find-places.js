export default {
  "layout": {
    "rows": [
      {
        "fields": [
          {
            "type": "dropdown",
            "name": "criteria.type",
            "labelCode": "containers.type",
            "listSource": {
              "displayProp": "name",
              "searchProp": "name",
              "apiUrl": "container-types"
            },
            "validations": {
              "required": {
                "messageCode": "containers.type_required"
              }
            }
          }
        ]
      },
      {
        "fields": [
          {
            "type": "dropdown",
            "name": "criteria.freezer",
            "labelCode": "containers.freezer",
            "listSource": {
              "displayProp": "name",
              "searchProp": "name",
              "initUsingSelectProp": true,
              "apiUrl": "storage-containers",
              "queryParams": {
                "static": {
                  "topLevelContainers": true,
                  "includeChildren": false,
                  "includeStats": false
                }
              }
            },
          }
        ]
      },
      {
        "fields": [
          {
            "type": "dropdown",
            "name": "criteria.cp",
            "labelCode": "containers.find_places.cp",
            "listSource": {
              "displayProp": "shortTitle",
              "searchProp": "query",
              "initUsingSelectProp": true,
              "apiUrl": "collection-protocols"
            }
          }
        ]
      },
      {
        "fields": [
          {
            "type": "number",
            "name": "criteria.requiredPlaces",
            "labelCode": "containers.find_places.required_places"
          }
        ]
      },
      {
        "fields": [
          {
            "type": "booleanCheckbox",
            "name": "criteria.allInOneContainer",
            "inlineLabelCode": "containers.find_places.all_in_the_same_container",
            "showWhen": "criteria.requiredPlaces > 1"
          }
        ]
      }
    ]
  }
}
