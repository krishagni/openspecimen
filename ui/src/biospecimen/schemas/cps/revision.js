export default {
  fields: [
    {
      "type": "datePicker",
      "showTime": true,
      "name": "revision.publicationDate",
      "labelCode": "cps.publication_date"
    },
    {
      "type": "user",
      "name": "revision.publishedBy",
      "labelCode": "cps.publisher"
    },
    {
      "type": "user",
      "name": "revision.reviewers",
      "labelCode": "cps.reviewers",
      "multiple": true
    },
    {
      "type": "textarea",
      "name": "revision.changes",
      "labelCode": "cps.change_summary"
    },
    {
      "type": "textarea",
      "name": "revision.reason",
      "labelCode": "cps.change_reason"
    }
  ]
}
