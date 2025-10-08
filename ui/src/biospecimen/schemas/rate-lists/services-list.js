
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
    }
  ]
}
