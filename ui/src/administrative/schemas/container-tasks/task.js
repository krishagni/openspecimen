export default {
  fields:  [
    {
      "type": "text",
      "label": "Name",
      "name": "task.name",
      "validations": {
        "required": {
          "message": "Container task name is mandatory"
        }
      }
    },
    {
      "type": "textarea",
      "label": "Description",
      "name": "task.description",
      "rows": 5,
      "validations": {
        "required": {
          "message": "Container task description is mandatory"
        }
      }
    }
  ]
}
