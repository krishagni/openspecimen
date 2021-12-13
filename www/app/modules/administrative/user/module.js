
angular.module('os.administrative.user', 
  [
    'os.administrative.user.dropdown',
    'os.administrative.user.list',
    'os.administrative.user.addedit',
    'os.administrative.user.detail',
    'os.administrative.user.roles',
    'os.administrative.user.password',
    'os.administrative.user.displayname',
    'os.common.import'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('user-root', {
        abstract: true,
        template: '<div ui-view></div>',
        controller: function($scope, AuthorizationService) {
          // User Authorization Options
          $scope.userResource = {
            createOpts: {resource: 'User', operations: ['Create']},
            updateOpts: {resource: 'User', operations: ['Update']},
            deleteOpts: {resource: 'User', operations: ['Delete']},
            importOpts: {resource: 'User', operations: ['Export Import']},
            updateAllowed: AuthorizationService.isAllowed({resource: 'User', operations: ['Update']})
          }

          $scope.extnState = 'user-detail.forms.';
        },
        parent: 'signed-in'
      })
      .state('user-list', {
        url: '/users?filters&groupId&old',
        templateUrl: 'modules/administrative/user/list.html',
        resolve: {
          group: function($stateParams, UserGroup) {
            if ($stateParams.groupId > 0) {
              return UserGroup.getById($stateParams.groupId);
            }

            return null;
          }
        },
        controller: 'UserListCtrl',
        parent: 'user-root',
        data: {
          vueView: true
        }
      })
      .state('user-addedit', {
        url: '/user-addedit/:userId?old',
        templateUrl: 'modules/administrative/user/addedit.html',
        resolve: {
          user: function($stateParams, User) {
            if ($stateParams.userId) {
              return User.getById($stateParams.userId);
            }

            return new User({dnd: false});
          },
          users: function() {
            return [];
          },
          editProfile: function() {
            return false;
          },
          oldView: function() {
            return $stateParams.old == true || $stateParams.old == 'true';
          }
        },
        controller: 'UserAddEditCtrl',
        parent: 'user-root',
        data: {
          vueView: true
        }
      })
      .state('user-edit-profile', {
        url: '/user-edit-profile',
        templateUrl: 'modules/administrative/user/addedit.html',
        resolve: {
          user: function(currentUser, User) {
            return User.getById(currentUser.id).then(
              function(user) {
                user.$$editProfile = true;
                return user;
              }
            );
          },
          users: function() {
            return [];
          },
          editProfile: function() {
            return true;
          }
        },
        controller: 'UserAddEditCtrl',
        parent: 'user-root',
        data: {
          vueView: true
        }
      })
      .state('user-bulk-edit', {
        url: '/bulk-edit-users',
        templateUrl: 'modules/administrative/user/bulk-edit.html',
        resolve: {
          user: function(User) {
            return new User();
          },
          users: function(ItemsHolder) {
            var users = ItemsHolder.getItems('users');
            ItemsHolder.setItems('users', undefined);
            return users;
          },
          editProfile: function() {
            return false;
          }
        },
        controller: 'UserAddEditCtrl',
        parent: 'user-root',
        data: {
          vueView: true
        }
      })
      .state('user-import', {
        url: '/users-import?objectType',
        templateUrl: 'modules/common/import/add.html',
        controller: 'ImportObjectCtrl',
        resolve: {
          forms: function($stateParams, currentUser, Form, Alerts) {
            if ($stateParams.objectType != 'extensions') {
              return [];
            }

            var entityId = currentUser.admin ? undefined : currentUser.instituteId;
            return Form.listForms(['User', 'UserProfile'], {entityId: entityId}).then(
              function(forms) {
                if (forms.length > 0) {
                  return forms;
                }

                Alerts.error('user.no_forms');
                throw 'No user forms';
              }
            );
          },
          importDetail: function($stateParams, forms, currentUser) {
            var objectType = $stateParams.objectType;
            var title = undefined;
            var types = [];
            if (objectType == 'user') {
              title = 'user.bulk_import_users';
            } else if (objectType == 'userRoles') {
              title = 'user.bulk_import_user_roles';
            } else if (objectType == 'extensions') {
              var entityId = currentUser.admin ? -1 : currentUser.instituteId;
              title = 'user.bulk_import_user_forms';
              types = forms.map(
                function(form) {
                  return {
                    type: 'userExtensions',
                    title: form.caption,
                    params: {
                      entityType: 'User',
                      entityId: entityId,
                      formName: form.name
                    }
                  };
                }
              );
            }

            return {
              breadcrumbs: [{state: 'user-list', title: 'user.list'}],
              objectType: objectType,
              title: title,
              onSuccess: {state: 'user-import-jobs'},
              types: types
            };
          }
        },
        parent: 'user-root'
      })
      .state('user-import-jobs', {
        url: '/users-import-jobs',
        templateUrl: 'modules/common/import/list.html',
        controller: 'ImportJobsListCtrl',
        resolve: {
          importDetail: function() {
            return {
              breadcrumbs: [{state: 'user-list', title: 'user.list'}],
              title: 'user.bulk_import_jobs',
              objectTypes: ['user', 'userRoles', 'userExtensions']
            }
          }
        },
        parent: 'user-root'
      })
      .state('user-export-forms', {
        url: '/users-export-forms',
        templateUrl: 'modules/common/export/add.html',
        controller: 'AddEditExportJobCtrl',
        resolve: {
          users: function(ItemsHolder) {
            var users = ItemsHolder.getItems('users');
            ItemsHolder.setItems('users', undefined);
            if (users == undefined) {
              return [];
            }

            return users;
          },

          forms: function(currentUser, users, Form) {
            var entityId = currentUser.admin ? undefined : currentUser.instituteId;
            if (entityId == undefined) {
              var instituteId = users.length > 0 ? users[0].instituteId : -1;
              for (var i = 0; i < users.length; ++i) {
                if (users[i].instituteId != instituteId) {
                  instituteId = -1;
                  break;
                }
              }

              if (instituteId > 0) {
                entityId = instituteId;
              }
            }

            return Form.listForms(['User', 'UserProfile'], {entityId: entityId}).then(
              function(forms) {
                if (forms.length > 0) {
                  return forms;
                }

                Alerts.error('user.no_forms');
                throw 'No user forms';
              }
            );
          },

          exportDetail: function(forms, currentUser, users) {
            var entityId = currentUser.admin ? -1 : currentUser.instituteId;
            var input = undefined;
            if (users.length > 0) {
              input = {var: 'emailIds', varName: 'user.email_ids', varDesc: 'user.email_ids_csv'};
            }

            var types = forms.map(
              function(form) {
                return {
                  type: 'userExtensions',
                  '$$input': input,
                  title: form.caption,
                  params: { entityType: 'User', entityId: entityId, formName: form.name }
                };
              }
            );

            return {
              breadcrumbs: [{state: 'user-list', title: 'user.list'}],
              title: 'user.export_user_forms',
              type: undefined,
              inputCsv: users.map(function(u) { return u.emailAddress }).join(','),
              onSuccess: {state: 'user-list'},
              types: types,
              params: {
                entityId: entityId
              },
              delayedNav: 5000
            };
          }
        },
        parent: 'user-root'
      })
      .state('user-detail', {
        url: '/users/:userId',
        templateUrl: 'modules/administrative/user/detail.html',
        resolve: {
          user: function($stateParams, User) {
            return User.getById($stateParams.userId);
          }
        },
        controller: 'UserDetailCtrl',
        parent: 'user-root'
      })
      .state('user-detail.overview', {
        url: '/overview',
        templateUrl: 'modules/administrative/user/overview.html',
        parent: 'user-detail'
      })
      .state('user-detail.forms', {
        url: '/forms',
        template: '<div ui-view></div>',
        controller: function($scope, user, forms, records, ExtensionsUtil) {
          $scope.extnOpts = {
            update: $scope.userResource.updateOpts,
            isEntityActive: user.activityStatus == 'Active',
            entity: user
          }

          $scope.object = user;
          ExtensionsUtil.linkFormRecords(forms, records);
        },
        resolve: {
          forms: function(user) {
            return user.getForms();
          },
          records: function(user) {
            return user.getRecords();
          },
          viewOpts: function() {
            return {
              goBackFn: null,
              showSaveNext: true
            };
          }
        },
        abstract: true,
        parent: 'user-detail'
      })
      .state('user-detail.forms.list', {
        url: '/list?formId&formCtxtId&recordId',
        templateUrl: 'modules/biospecimen/extensions/list.html',
        controller: 'FormsListCtrl',
        parent: 'user-detail.forms'
      })
      .state('user-detail.forms.addedit', {
        url: '/addedit?formId&recordId&formCtxId',
        templateUrl: 'modules/biospecimen/extensions/addedit.html',
        resolve: {
          formDef: function($stateParams, Form) {
            return Form.getDefinition($stateParams.formId);
          },
          postSaveFilters: function() {
            return [];
          }
        },
        controller: 'FormRecordAddEditCtrl',
        parent: 'user-detail.forms'
      })
      .state('user-detail.roles', {
        url: '/roles',
        templateUrl: 'modules/administrative/user/roles.html',
        resolve: {
          userRoles: function(user) {
            return user.getRoles();
          },
          
          currentUserInstitute: function(currentUser) {
            return currentUser.getInstitute();
          }
        },
        controller: 'UserRolesCtrl',
        parent: 'user-detail'
      })
      .state('user-password', {
        url: '/user-password-change/:userId',
        templateUrl: 'modules/administrative/user/password.html',
        controller: 'UserPasswordCtrl',
        parent: 'user-root',
        data: {
          vueView: true
        }
      })

      .state('user-groups', {
        url: '/user-groups?filters',
        templateUrl: 'modules/administrative/user/list-groups.html',
        controller: 'UserGroupsListCtrl',
        resolve: {
          pagerOpts: function(ListPagerOpts) {
            return new ListPagerOpts({recordsPerPage: 100});
          }
        },
        parent: 'user-root'
      })
      .state('user-group-addedit', {
        url: '/user-group-addedit/:groupId',
        templateUrl: 'modules/administrative/user/addedit-group.html',
        resolve: {
          group: function($stateParams, UserGroup) {
            if ($stateParams.groupId > 0) {
              return UserGroup.getById($stateParams.groupId);
            }

            return new UserGroup({});
          }
        },
        controller: 'AddEditUserGroupCtrl',
        parent: 'user-root'
      })
  })

  .run(function(UrlResolver, QuickSearchSvc) {
    UrlResolver.regUrlState('user-overview', 'user-detail.overview', 'userId');
    UrlResolver.regUrlState('user-roles', 'user-detail.roles', 'userId');
    UrlResolver.regUrlState('user-password-change', 'user-password', 'userId');

    var opts = {caption: 'entities.user', state: 'user-detail.overview'};
    QuickSearchSvc.register('user', opts);
  });
