export default {
  fields:  [
    {
      "type": "text",
      "label": "Code",
      "name": "statement.code",
      "validations": {
        "required": {
          "message": "Code is mandatory"
        }
      }
    },
    {
      "type": "text",
      "label": "Statement",
      "name": "statement.statement",
      "validations": {
        "required": {
          "message": "Statement is mandatory"
        }
      }
    }
  ]
}
