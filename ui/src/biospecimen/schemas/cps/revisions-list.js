export default {
  key: "revision.id",

  columns: [
    {
      name: "revision.id",
      captionCode: "cps.revision_id",
      value: ({revision}) => '#' + revision.id
    },

    {
      name: "revision.publicationDate",
      captionCode: "cps.publication_date",
      type: "date-time"
    },

    {
      name: "revision.publishedBy",
      captionCode: "cps.publisher",
      type: "user"
    },

    {
      name: "revision.reviewers",
      captionCode: "cps.reviewers",
      type: "user",
      uiStyle: {
        'max-width': '350px'
      }
    }
  ]
}
