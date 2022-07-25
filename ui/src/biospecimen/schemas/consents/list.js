import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "statement.code",
      captionCode: "consents.statement_code",
      href: ({rowObject}) => routerSvc.getUrl('ConsentStatementAddEdit', {statementId: rowObject.statement.id})
    },
    {
      name: "statement.statement",
      captionCode: "consents.statement"
    },
  ],

  filters: [
    {
      name: "code",
      type: "text",
      captionCode: "consents.statement_code"
    },
    {
      name: "statement",
      type: "text",
      captionCode: "consents.statement"
    }
  ]
}
