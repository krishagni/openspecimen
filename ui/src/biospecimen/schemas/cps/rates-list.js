
export default {
  columns: [
    {
      type: 'datePicker',
      labelCode: 'cps.start_date',
      name: 'rate.startDate', 
      dateOnly: true
    },
    { 
      type: 'datePicker',
      labelCode: 'cps.end_date',
      name: 'rate.endDate',
      dateOnly: true
    },  
    {   
      type: 'number',
      labelCode: 'cps.service_rate',
      name: 'rate.rate'
    }      
  ]
}
