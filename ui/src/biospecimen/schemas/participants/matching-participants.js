export default {
  columns: [
    {
      "name": "participant.firstName",
      "labelCode": "participants.first_name",
      "type": "span"
    },
    {
      "name": "participant.lastName",
      "labelCode": "participants.last_name",
      "type": "span"
    },
    {
      "name": "participant.birthDate",
      "labelCode": "participants.birth_date",
      "type": "span",
      "displayType": "datePicker"
    },
    {
      "name": "participant.uid",
      "labelCode": "participants.uid",
      "type": "span"
    },
    {
      "name": "participant.empi",
      "labelCode": "participants.empi",
      "type": "span"
    },
    {
      "name": "participant.pmis",
      "labelCode": "participants.medical_ids",
      "type": "span",
      "value": ({participant}) => {
        const {pmis} = participant;
        return (pmis || []).map(({siteName, mrn}) => siteName + (mrn ? ' (' + mrn + ') ' : '')).join(', ');
      }
    },
    {
      "name": "participant.registeredCps",
      "labelCode": "participants.registered_cps",
      "type": "span",
      "value": ({participant}) => {
        const {registeredCps} = participant;
        return (registeredCps || []).map(({cpShortTitle}) => cpShortTitle).join(', ');
      }
    }
  ]
}
