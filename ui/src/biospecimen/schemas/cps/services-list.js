
export default {
  columns: [
    {   
      type: 'text',
      labelCode: 'cps.code',
      name: 'service.code'
    },  
    {     
      type: 'text',
      labelCode: 'cps.description',
      name: 'service.description'
    },
    { 
      type: 'text',
      labelCode: 'cps.present_service_rate',
      name: 'service.rate',
      value: ({currency, service}) => {
        if (service.rate == undefined || service.rate == null) {
          return service.rate;
        }

        return currency ? (currency + ' ' + service.rate) : service.rate;
      }
    }   
  ]
}
