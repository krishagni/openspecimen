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
