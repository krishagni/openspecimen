
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
      name: 'rate.rate',
      value: ({currency, rate}) => {
        if (rate.rate == undefined || rate.rate == null) {
          return rate.rate;
        }

        return currency ? (currency + ' ' + rate.rate) : rate.rate;
      }
    }      
  ]
}
