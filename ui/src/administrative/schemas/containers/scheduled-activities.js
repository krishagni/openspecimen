
export default {
  columns: [
    {
      name: "activity.name",
      caption: "Activity",
    },
    {
      name: "activity.startDate",
      caption: "Start Date",
      type: "date"
    },
    {
      name: "activity.cycleInterval",
      caption: "Cycle Interval",
      value: ({activity}) => {
        return activity.cycleInterval + ' ' + activity.cycleIntervalUnit.charAt(0).toLowerCase();
      }
    },
    {
      name: "activity.reminderInterval",
      caption: "Remind Before",
      value: ({activity}) => {
        return activity.reminderInterval + ' ' + activity.reminderIntervalUnit.charAt(0).toLowerCase();
      }
    },
    {
      name: "activity.assignedUsers",
      caption: "Assigned Users",
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
