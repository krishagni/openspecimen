
angular.module('os.administrative.role', 
  [
    'os.administrative.role.list',
    'os.administrative.role.detail',
    'os.administrative.role.addedit'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('role-list', {
        url: '/roles',
        templateUrl: 'modules/administrative/role/list.html',
        controller: 'RoleListCtrl',
        parent: 'signed-in'
      })
      .state('role-addedit', {
        url: '/role-addedit/:roleId',
        templateUrl: 'modules/administrative/role/addedit.html',
        resolve: {
          role: function($stateParams, Role) {
            if ($stateParams.roleId) {
              return Role.getById($stateParams.roleId);
            }
            return new Role();
          }
        },
        controller: 'RoleAddEditCtrl',
        parent: 'signed-in'
      })
      .state('role-detail', {
        url: '/roles/:roleId',
        templateUrl: 'modules/administrative/role/detail.html',
        resolve: {
          role: function($stateParams, Role) {
            return Role.getById($stateParams.roleId);
          }
        },
        controller: 'RoleDetailCtrl',
        parent: 'signed-in'
      })
      .state('role-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/administrative/role/overview.html',
        parent: 'role-detail'
      })
  });

