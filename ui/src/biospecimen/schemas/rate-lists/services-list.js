
export default {
  columns: [
    {   
      type: 'text',
      labelCode: 'lab_services.code',
      name: 'service.code'
    },  
    {     
      type: 'text',
      labelCode: 'lab_services.description',
      name: 'service.description'
    },
    { 
      type: 'number',
      labelCode: 'lab_services.rate_lists',
      name: 'service.rateLists'
    }   
  ],

  filters: [
    {
      type: 'text',
      labelCode: 'lab_services.code_or_desc',
      name: 'query'
    },
    {
      type: 'dropdown',
      labelCode: 'lab_services.cp',
      name: 'cpShortTitle',
      listSource: {
        apiUrl: 'collection-protocols',
        displayProp: 'shortTitle',
        selectProp: 'shortTitle',
        searchProp: 'query'
      }
    }
  ]
}
