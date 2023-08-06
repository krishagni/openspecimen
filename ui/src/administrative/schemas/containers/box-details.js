export default {
  "layout": {
    "rows": [
      {
        "fields": [
          {
            "type": "dropdown",
            "name": "newBox.typeName",
            "labelCode": "containers.type",
            "listSource": {
              "apiUrl" : "container-types",
              "selectProp": "name",
              "displayProp": "name",
              "searchProp": "name"
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
            "type": "multiselect",
            "name": "newBox.allowedCollectionProtocols",
            "labelCode": "containers.collection_protocols",
            "listSource": {
              "displayProp": "shortTitle",
              "selectProp": "shortTitle",
              "loadFn": ({context, query, maxResults}) => context.formData.getAllowedCps({query, maxResults})
            }
          }     
        ]     
      },    
      {
        "fields": [
          {
            "type": "groupselect",
            "name": "newBox.allowedTypes",
            "labelCode": "containers.specimen_types",
            "listSource": {
              "displayProp": "type",
              "groupNameProp": "specimenClass",
              "groupItemsProp": "types",
              "loadFn": ({context}) => context.formData.getAllowedTypes()
            }
          }
        ]
      }
    ]
  }
}
