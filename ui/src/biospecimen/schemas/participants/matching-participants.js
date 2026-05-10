import routerSvc from '@/common/services/Router.js';

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
    }
  ],

  registrationsTable: {
    columns: [
      {
        "name": "cpShortTitle",
        "labelCode": "participants.cp",
        "type": "span",
        "href": ({rowObject: {cpId, cprId}}) => {
          if (cpId > 0 && cprId > 0) {
            return routerSvc.getUrl('ParticipantsListItemDetail.Overview', {cpId, cprId});
          }
        },
        "hrefTarget": "_blank"
      },
      {
        "name": "ppid",
        "labelCode": "participants.ppid",
        "type": "span"
      },
      {
        "name": "registrationDate",
        "labelCode": "participants.registration_date",
        "type": "span",
        "displayType": "datePicker"
      },
      {
        "name": "site",
        "labelCode": "participants.registration_site",
        "type": "span"
      }
    ]
  }
}
