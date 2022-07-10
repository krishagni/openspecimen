export default {
  columns: [
    {
      name: "activityName",
      captionCode: "containers.activity",
      value: ({activityLog}) => activityLog.scheduledActivityName || activityLog.taskName
    },
    {
      name: "activityLog.performedBy",
      captionCode: "containers.performed_by",
      type: "user"
    },
    {
      name: "activityLog.activityDate",
      captionCode: "containers.activity_date",
      type: "date"
    },
    {
      name: "activityLog.timeTaken",
      captionCode: "containers.activity_time_taken"
    },
    {
      name: "activityLog.comments",
      captionCode: "common.comments"
    }
  ]
}
