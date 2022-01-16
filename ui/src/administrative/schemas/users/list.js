import routerSvc from '@/common/services/Router.js';
import dateFmt   from '@/common/filters/DateFormatter.js';

export default {
  key: "user.id",

  summary: {
    title: {
      text: (ro) => ro.user.firstName + ' ' + ro.user.lastName,
      url:  (ro) => routerSvc.getUrl('UserDetail.Overview', {userId: ro.user.id}),
    },

    descriptions: [
      (ro) => ro.user.type != 'CONTACT' ? ro.user.loginName : '',
      "user.emailAddress",
      (ro) => dateFmt.date(ro.user.creationDate)
    ]
  },

  columns: [
    {
      name: 'user.name',
      caption: 'Name',
      value: (rowObject) => rowObject.user.firstName + ' ' + rowObject.user.lastName,
      href: (row) => routerSvc.getUrl('UserDetail.Overview', {userId: row.rowObject.user.id})
    },
    {
      name: 'user.emailAddress',
      caption: 'Email Address'
    },
    {
      name: 'user.loginName',
      caption: 'Login Name',
      value: (rowObject) => rowObject.user.type == 'CONTACT' ?  '-' : rowObject.user.loginName
    },
    {
      name: 'user.instituteName',
      caption: 'Institute'
    },
    {
      name: 'user.primarySite',
      caption: 'Primary Site'
    },
    {
      name: 'user.creationDate',
      caption: 'Active Since',
      type: 'date'
    }
  ],

  filters: [
    {
      name: 'name',
      type: 'text',
      caption: 'Name'
    },
    {
      name: 'loginName',
      type: 'text',
      caption: 'Login Name'
    },
    {
      name: 'institute',
      type: 'dropdown',
      caption: 'Institute',
      listSource: {
        apiUrl: 'institutes',
        displayProp: 'name',
        selectProp: 'name',
        searchProp: 'name'
      }
    },
    {
      name: 'group',
      type: 'dropdown',
      caption: 'User Group',
      listSource: {
        apiUrl: 'user-groups',
        displayProp: 'name',
        selectProp: 'name'
      }
    },
    {
      name: 'activityStatus',
      type: 'dropdown',
      caption: 'Activity Status',
      listSource: {
        selectProp: 'value',
        displayProp: 'value',
        options: [
          {value: 'Active'},
          {value: 'Archived'},
          {value: 'Expired'},
          {value: 'Locked'},
          {value: 'Pending'}
        ]
      }
    },
    {
      name: 'type',
      type: 'dropdown',
      caption: 'Type',
      listSource: {
        selectProp: 'name',
        displayProp: 'caption',
        options: [
          { name: 'SUPER', caption: 'Super Admin' },
          { name: 'INSTITUTE', caption: 'Institute Admin' },
          { name: 'CONTACT', caption: 'Contact' },
          { name: 'NONE', caption: 'Regular' }
        ]
      }
    }
  ]
}
