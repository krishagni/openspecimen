export default {
  layout: {
    rows: [
      {
        fields: [ { name: "cpr.registrationDate" } ]
      },

      {
        fields: [ { name: "cpr.ppid" } ]
      },

      {
        fields: [ { name: "cpr.site" } ]
      },

      {
        fields: [ { name: "cpr.externalSubjectId" } ]
      },

      {
        fields: [
          { name: "cpr.participant.firstName" },
          { name: "cpr.participant.middleName" },
          { name: "cpr.participant.lastName" }
        ]
      },

      {
        fields: [ { name: "cpr.participant.emailAddress" } ]
      },

      {
        fields: [ { name: "cpr.participant.emailOptIn" } ]
      },

      {
        fields: [ { name: "cpr.participant.phoneNumber" } ]
      },

      {
        fields: [ { name: "cpr.participant.textOptIn" } ]
      },

      {
        fields: [ { name: "cpr.participant.birthDate" } ]
      },

      {
        fields: [ { name: "cpr.participant.empi" } ]
      },

      {
        fields: [ { name: "cpr.participant.uid" } ]
      },

      {
        fields: [ { name: "cpr.participant.gender" } ]
      },

      {
        fields: [ { name: "cpr.participant.vitalStatus" } ]
      },

      {
        fields: [ { name: "cpr.participant.deathDate" } ]
      },

      {
        fields: [ { name: "cpr.participant.races" } ]
      },

      {
        fields: [ { name: "cpr.participant.ethnicities" } ]
      },

      {
        fields: [ { name: "cpr.participant.pmis" } ]
      }
    ]
  }
}
