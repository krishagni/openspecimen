
export default {
  layout: {
    "rows": [
      {
        "fields": [ { "name": "dp.title" } ]
      },

      {
        "fields": [ { "name": "dp.shortTitle" } ]
      },

      {
        "fields": [ { "name": "dp.instituteName" } ]
      },

      {
        "fields": [ { "name": "dp.defReceivingSiteName" } ]
      },

      {
        "fields": [ { "name": "dp.principalInvestigator" } ]
      },

      {
        "fields": [ { "name": "dp.coordinators" } ]
      },

      {
        "fields": [ { "name": "dp.irbId" } ]
      },

      {
        "fields": [ { "name": "dp.startDate" }, { "name": "dp.endDate" } ]
      },

      {
        "fields": [ { "name": "dp.orderItemLabelFormat" } ]
      },

      {
        "fields": [ { "name": "dp.orderExtnForm" } ]
      },

      {
        "fields": [ { "name": "dp.report" } ]
      },

      {
        "fields": [ { "name": "dp.attachmentType" } ]
      },

      {
        "fields": [ { "name": "dp.disableEmailNotifs", "optionsPerRow": 3 } ]
      },

      {
        "fields": [
          {
            "name": "dp.distributingSites",
            "showWhen": "currentUser.admin"
          }
        ]
      },

      {
        "fields": [
          {
            "name": "dp.distributingSites.0.sites",
            "showWhen": "!currentUser.admin"
          }
        ]
      }
    ]
  }
}
