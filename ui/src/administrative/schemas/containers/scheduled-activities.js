export default {
  columns: [
    {
      name: "activity.name",
      captionCode: "containers.activity",
    },
    {
      name: "activity.startDate",
      captionCode: "containers.start_date",
      type: "date"
    },
    {
      name: "activity.cycleInterval",
      captionCode: "containers.cycle_interval",
      value: ({activity}) => {
        return activity.cycleInterval + ' ' + activity.cycleIntervalUnit.charAt(0).toLowerCase();
      }
    },
    {
      name: "activity.reminderInterval",
      captionCode: "containers.remind_before",
      value: ({activity}) => {
        if (activity.disableReminders) {
          return 'N/A';
        }

        return activity.reminderInterval + ' ' + activity.reminderIntervalUnit.charAt(0).toLowerCase();
      }
    },
    {
      name: "activity.assignedUsers",
      captionCode: "containers.assigned_users",
      value: ({activity}) => {
        let result = [];
        for (let user of activity.assignedUsers) {
          let userName = user.firstName;
          if (userName) {
            userName += ' ';
          }

          userName += user.lastName;
          if (userName) {
            result.push(userName);
          }
        }

        return result.join(', ');
      }
    }
  ]
}
