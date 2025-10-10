export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'multiselect',
            labelCode: 'lab_services.select_cps_for_rate_list',
            name: 'cpsToAdd',
            listSource: {
              apiUrl: 'collection-protocols',
              searchProp: 'query',
              displayProp: 'shortTitle'
            }
          }
        ]
      }
    ]
  }
}
