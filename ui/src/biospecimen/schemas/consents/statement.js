export default {
  fields:  [
    {
      "type": "text",
      "labelCode": "consents.statement_code",
      "name": "statement.code",
      "validations": {
        "required": {
          "messageCode": "consents.statement_code_req"
        }
      }
    },
    {
      "type": "text",
      "labelCode": "consents.statement",
      "name": "statement.statement",
      "validations": {
        "required": {
          "messageCode": "consents.statement_req"
        }
      }
    }
  ]
}
