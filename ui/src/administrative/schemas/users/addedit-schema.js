export default {
  layout: {
    "rows": [
       {
         "fields": [ { "name": "user.type" } ]
       },

       {
         "fields": [ { "name": "user.firstName" }, { "name": "user.lastName" } ]
       },

       {
         "fields": [
           {
             "name": "user.emailAddress",
             "showWhen": "currentUser.id != user.id && (currentUser.admin || currentUser.instituteAdmin)"
           }
         ]
       },

       {
         "fields": [ { "name": "user.phoneNumber" } ]
       },

       {
         "fields": [ { "name": "user.domainName" } ]
       },

       {
         "fields": [ { "name": "user.loginName" } ]
       },

       {
         "fields": [ { "name": "user.instituteName", "showWhen": "currentUser.admin || user.type == 'CONTACT'" } ]
       },

       {
         "fields": [ { "name": "user.primarySite" } ]
       },

       {
         "fields": [ { "name": "user.timeZone" } ]
       },

       {
         "fields": [ { "name": "user.manageForms" } ]
       },

       {
         "fields": [ { "name": "user.manageWfs" } ]
       },

       {
         "fields": [ { "name": "user.dnd" } ]
       },

       {
         "fields": [ { "name": "user.apiUser" } ]
       },

       {
         "fields": [ { "name": "user.ipRange" } ]
       },

       {
         "fields": [ { "name": "user.defaultPrinter" } ]
       },

       {
         "fields": [ { "name": "user.downloadLabelsPrintFile" } ]
       },

       {
         "fields": [ { "name": "user.address" } ]
       }
    ]
  }
}
