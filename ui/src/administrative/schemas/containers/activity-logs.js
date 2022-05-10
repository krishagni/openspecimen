
export default {
  columns: [
    {
      name: "activityName",
      caption: "Activity",
      value: ({activityLog}) => activityLog.scheduledActivityName || activityLog.taskName
    },
    {
      name: "activityLog.performedBy",
      caption: "Performed By",
      type: "user"
    },
    {
      name: "activityLog.activityDate",
      caption: "Activity Date",
      type: "date"
    },
    {
      name: "activityLog.timeTaken",
      caption: "Time Taken (mins)"
    },
    {
      name: "activityLog.comments",
      caption: "Comments"
    }
  ]
}
