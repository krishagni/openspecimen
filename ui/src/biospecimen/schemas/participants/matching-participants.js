export default {
  columns: [
    {
      "name": "cpShortTitle",
      "labelCode": "participants.cp",
      "type": "span"
    },
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
    }
  ]
}
