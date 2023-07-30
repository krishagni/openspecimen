import routerSvc from '@/common/services/Router.js';
import dateFmt   from '@/common/filters/DateFormatter.js';

export default {
  key: "user.id",

  summary: {
    title: {
      text: (ro) => ro.user.firstName + ' ' + ro.user.lastName,
      url:  (ro, query) => routerSvc.getUrl('UsersListItemDetail.Overview', {userId: ro.user.id}, query),
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
      captionCode: 'users.name',
      value: (rowObject) => rowObject.user.firstName + ' ' + rowObject.user.lastName,
      href: (row, query) => routerSvc.getUrl('UsersListItemDetail.Overview', {userId: row.rowObject.user.id}, query)
    },
    {
      name: 'user.emailAddress',
      captionCode: 'users.email_address'
    },
    {
      name: 'user.loginName',
      captionCode: 'users.login_name',
      value: (rowObject) => rowObject.user.type == 'CONTACT' ?  '-' : rowObject.user.loginName
    },
    {
      name: 'user.instituteName',
      captionCode: 'users.institute'
    },
    {
      name: 'user.primarySite',
      captionCode: 'users.primary_site'
    },
    {
      name: 'user.creationDate',
      captionCode: 'users.active_since',
      type: 'date'
    }
  ],

  filters: [
    {
      name: 'name',
      type: 'text',
      captionCode: 'users.name'
    },
    {
      name: 'loginName',
      type: 'text',
      captionCode: 'users.login_name'
    },
    {
      name: 'institute',
      type: 'dropdown',
      captionCode: 'users.institute',
      listSource: {
        apiUrl: 'institutes',
        displayProp: 'name',
        selectProp: 'name',
        searchProp: 'name'
      },
      showWhen: "currentUser.admin"
    },
    {
      name: 'group',
      type: 'dropdown',
      captionCode: 'users.user_group',
      listSource: {
        apiUrl: 'user-groups',
        displayProp: 'name',
        selectProp: 'name'
      }
    },
    {
      name: 'activityStatus',
      type: 'dropdown',
      captionCode: 'users.activity_status',
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
      captionCode: 'users.type',
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
