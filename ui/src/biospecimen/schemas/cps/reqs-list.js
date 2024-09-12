export default {
  key: "sr.id",

  columns: [
    {
      name: "sr",
      labelCode: "cps.req_name",
      type: "specimen-description"
    },

    {
      name: "sr.type",
      labelCode: "cps.type",
      type: "span"
    },

    {
      name: "sr.initialQty",
      labelCode: "cps.quantity",
      type: "specimen-measure",
      measure: "quantity",
      entity: "sr"
    }
  ]
}
