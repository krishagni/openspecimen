import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "statement.code",
      caption: "Code",
      href: ({rowObject}) => routerSvc.getUrl('ConsentStatementAddEdit', {statementId: rowObject.statement.id})
    },
    {
      name: "statement.statement",
      caption: "Statement"
    },
  ],

  filters: [
    {
      name: "code",
      type: "text",
      caption: "Code"
    },
    {
      name: "statement",
      type: "text",
      caption: "Statement"
    }
  ]
}
