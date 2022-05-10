
export default {
  columns: [
    {
      name: "task.name",
      caption: "Name",
      href: (row) => "#/container-tasks/" + row.rowObject.task.name
    },
    {
      name: "task.description",
      caption: "Description",
    }
  ],

  filters: [
    {
      name: "name",
      type: "text",
      caption: "Name"
    }
  ]
}
