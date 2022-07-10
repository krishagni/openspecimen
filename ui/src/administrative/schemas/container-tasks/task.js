export default {
  "fields":  [
    {
      "type": "text",
      "labelCode": "container_tasks.name",
      "name": "task.name",
      "validations": {
        "required": {
          "messageCode": "container_tasks.name_required"
        }
      }
    },
    {
      "type": "textarea",
      "labelCode": "container_tasks.description",
      "name": "task.description",
      "rows": 5,
      "validations": {
        "required": {
          "messageCode": "container_tasks.description_required"
        }
      }
    }
  ]
}
