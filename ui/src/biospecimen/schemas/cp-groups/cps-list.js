import i18n      from '@/common/services/I18n.js';
import routerSvc from '@/common/services/Router.js';

export default {
  columns: [
    {
      name: "cp.shortTitle",
      captionCode: "cps.title",
      href: (row) => routerSvc.getUrl('CpDetail.Overview', {cpId: row.rowObject.cp.id}),
      value: ({cp}) => {
        let result = cp.shortTitle;
        if (cp.code) {
          result += ' (' + cp.code + ')';
        }

        return result;
      }
    },
    {
      name: "cp.principalInvestigator",
      captionCode: "cps.pi",
      type: "user"
    },
    {
      name: "cp.startDate",
      captionCode: "cps.start_date",
      type: "date"
    },
    {
      name: "cp.groupWorkflowsInherited",
      captionCode: "cps.workflows_source",
      tooltipCode: "cps.workflows_source_help",
      value: ({cp}) => {
        if (cp.groupWorkflowsInherited == null) {
          return '';
        }

        return i18n.msg(cp.groupWorkflowsInherited ? 'cps.workflows_inherited' : 'cps.workflows_customised');
      }
    }
  ],

  filters: [
    {
      name: "title",
      type: "text",
      captionCode: "cps.title"
    },
    {
      name: "repositoryName",
      type: "site",
      captionCode: "cps.site",
      listSource: { selectProp: "name" }
    },
    {
      name: "piId",
      type: "user",
      captionCode: "cps.pi",
      selectProp: "id"
    }
  ]
};
