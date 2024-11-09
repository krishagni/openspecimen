import i18n from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "queryName",
      captionCode: "queries.query",
      value: ({auditLog}) => {
        if (auditLog.queryId > 0) {
          return '#' + auditLog.queryId + ' ' + auditLog.queryTitle;
        } else {
          return i18n.msg('queries.unsaved_query');
        }
      },
      href: ({rowObject: {auditLog}}) => {
        if (auditLog.queryId > 0) {
          return routerSvc.getUrl('QueryDetail.AddEdit', {queryId: auditLog.queryId});
        } else {
          return null;
        }
      }
    },
    {
      name: "auditLog.runBy",
      type: "user",
      captionCode: "queries.executed_by"
    },
    {
      name: "auditLog.runType",
      captionCode: "queries.run_type"
    },
    {
      name: "auditLog.timeOfExecution",
      type: "date-time",
      captionCode: "queries.time_of_exec"
    },
    {
      name: "auditLog.timeToFinish",
      captionCode: "queries.exec_time",
      value: ({auditLog}) => auditLog.timeToFinish / 1000
    }
  ],

  filters: [
    {
      name: "query",
      type: "text",
      captionCode: "queries.id_or_title"
    },
    {
      name: "userId",
      type: "user",
      captionCode: "queries.executed_by",
      selectProp: "id"
    }
  ]
}
