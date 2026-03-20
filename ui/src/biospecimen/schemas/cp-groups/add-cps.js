export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'multiselect',
            labelCode: 'cpgs.select_cps',
            name: 'cpsToAdd',
            listSource: {
              displayProp: 'displayLabel',
              loadFn: ({context, query, maxResults}) => context.formData.getAvailableCps({query, maxResults})
            }
          }
        ]
      }
    ]
  }
}
