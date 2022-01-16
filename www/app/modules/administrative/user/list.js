
angular.module('os.administrative.user.list', ['os.administrative.models'])
  .controller('UserListCtrl', function(
    $scope, $state, $stateParams, $modal, $translate, currentUser, group,
    osRightDrawerSvc, osExportSvc, User, ItemsHolder, PvManager,
    Util, DeleteUtil, CheckList, Alerts, ListPagerOpts, UserGroup, VueApp) {

    var pagerOpts, filterOpts, ctx;
    var pvInit = false;

    function init() {
      pagerOpts = $scope.pagerOpts = new ListPagerOpts({listSizeGetter: getUsersCount});
      ctx = $scope.ctx = {
        old: $stateParams.old == 'true',
        exportDetail: {objectType: 'user'},
        group: group,
        emptyState: {
          empty: true,
          loading: true,
          emptyMessage: 'user.empty_list',
          loadingMessage: 'user.loading_list'
        }
      };

      if (!ctx.old) {
        VueApp.setVueView('users/-1', $stateParams);
      } else {
        initPvsAndFilterOpts();
        loadUsers($scope.userFilterOpts);
        ItemsHolder.setItems('users', undefined);
      }
    }
  
    function initPvsAndFilterOpts() {
      filterOpts = $scope.userFilterOpts = Util.filterOpts({includeStats: true, maxResults: pagerOpts.recordsPerPage + 1});
      if (group) {
        filterOpts.group = group.name;
      }

      $scope.$on('osRightDrawerOpen', function() {
        if (pvInit) {
          return;
        }

        loadActivityStatuses();
        loadUserTypes();
        Util.filter($scope, 'userFilterOpts', loadUsers);
        pvInit = true;
      });
    }
   
    function loadActivityStatuses() {
      PvManager.loadPvs('activity-status').then(
        function(result) {
          var statuses = [].concat(result);
          statuses.push('Locked');
          statuses.push('Expired');
          var idx = statuses.indexOf('Disabled');
          if (idx != -1) {
            statuses.splice(idx, 1);
          }

          idx = statuses.indexOf('Closed');
          if (idx != -1) {
            statuses[idx] = 'Archived';
          } else {
            statuses.push('Archived');
          }

          $scope.activityStatuses = statuses.sort();
        }
      );
    }

    function loadUserTypes() {
      $translate('user.types.NONE').then(
        function() {
          $scope.userTypes = ['SUPER', 'INSTITUTE', 'CONTACT', 'NONE'].map(
            function(type) {
              return {type: type, name: $translate.instant('user.types.' + type)};
            }
          );
        }
      );
    }

    function loadUsers(filterOpts) {
      if (!currentUser.admin) {
        filterOpts = filterOpts || {};
        filterOpts.institute = currentUser.instituteName;
      }

      ctx.emptyState.loading = true;
      User.query(filterOpts).then(function(result) {
        if (!$scope.users && result.length > 12) {
          //
          // Show search options when # of users are more than 12
          //
          osRightDrawerSvc.open();
        }

        $scope.users = result;
        ctx.emptyState.loading = false;
        ctx.emptyState.empty = result.length <= 0;
        pagerOpts.refreshOpts(result);
        $scope.ctx.checkList = new CheckList($scope.users);
      });
    };

    function getUsersCount() {
      return User.getCount($scope.userFilterOpts)
    }

    function updateStatus(prevStatuses, status, msgKey) {
      var users = $scope.ctx.checkList.getSelectedItems()
        .filter(function(u) { return prevStatuses.indexOf(u.activityStatus) != -1; });

      var usersMap = {};
      angular.forEach(users, function(u) { usersMap[u.id] = u; });

      User.bulkUpdate({detail: {activityStatus: status}, ids: Object.keys(usersMap)}).then(
        function(savedUsers) {
          Alerts.success(msgKey, {count: savedUsers.length});

          angular.forEach(savedUsers,
            function(su) {
              usersMap[su.id].activityStatus = su.activityStatus;
            }
          );

          $scope.ctx.checkList = new CheckList($scope.users);
        }
      );
    }

    function getUserIds(users) {
      return users.map(function(user) { return user.id; });
    }

    function exportRecords(type) {
      var userIds = getUserIds($scope.ctx.checkList.getSelectedItems());
      var exportDetail = {objectType: type, recordIds: userIds};
      osExportSvc.exportRecords(exportDetail);
    }

    $scope.showUserOverview = function(user) {
      $state.go('user-detail.overview', {userId:user.id});
    };

    $scope.broadcastAnnouncement = function() {
      $modal.open({
        templateUrl: 'modules/administrative/user/announcement.html',
        controller: 'AnnouncementCtrl'
      }).result.then(
        function(announcement) {
          User.broadcastAnnouncement(announcement).then(
            function(resp) {
              Alerts.success('user.announcement.success');
            }
          );
        }
      );
    }

    $scope.deleteUsers = function() {
      var users = $scope.ctx.checkList.getSelectedItems();

      if (!currentUser.admin) {
        var admins = users.filter(function(user) { return !!user.admin; })
          .map(function(user) { return user.getDisplayName(); });

        if (admins.length > 0) {
          Alerts.error('user.admin_access_req', {adminUsers: admins});
          return;
        }
      }

      var opts = {
        confirmDelete: 'user.delete_users',
        successMessage: 'user.users_deleted',
        onBulkDeletion: function() {
          loadUsers($scope.userFilterOpts);
        }
      }

      DeleteUtil.bulkDelete({bulkDelete: User.bulkDelete}, getUserIds(users), opts);
    }

    $scope.editUsers = function() {
       var users = $scope.ctx.checkList.getSelectedItems();
       ItemsHolder.setItems('users', users);
       $state.go('user-bulk-edit');
    }

    $scope.unlockUsers = function() {
      updateStatus(['Locked'], 'Active', 'user.users_unlocked');
    }

    $scope.approveUsers = function() {
      updateStatus(['Pending'], 'Active', 'user.users_approved');
    }

    $scope.lockUsers = function() {
      updateStatus(['Active'], 'Locked', 'user.users_locked');
    }

    $scope.archiveUsers = function() {
      updateStatus(['Locked', 'Active', 'Expired'], 'Closed', 'user.users_archived');
    }

    $scope.reactivateUsers = function() {
      updateStatus(['Closed'], 'Active', 'user.users_reactivated');
    }

    $scope.pageSizeChanged = function() {
      filterOpts.maxResults = pagerOpts.recordsPerPage + 1;
    }

    $scope.exportUsers = function() {
      exportRecords('user');
    }

    $scope.exportUserRoles = function() {
      exportRecords('userRoles');
    }

    $scope.exportUserForms = function() {
      var users = $scope.ctx.checkList.getSelectedItems();
      ItemsHolder.setItems('users', users);
      $state.go('user-export-forms');
    }

    $scope.addToGroup = function(group) {
      var users = $scope.ctx.checkList.getSelectedItems();
      if (!users || users.length == 0) {
        return;
      }

      var instituteId = users[0].instituteId;
      for (var i = 0; i < users.length; ++i) {
        if (users[i].instituteId != instituteId) {
          Alerts.error('user.multi_institute_users');
          return;
        }
      }

      if (!!group) {
        group.addUsers(users).then(
          function(result) {
            Alerts.success('user.group_users_added', result);
          }
        );
      } else {
        ItemsHolder.setItems('users', users);
        $state.go('user-group-addedit', {groupId: ''});
      }
    }

    $scope.removeFromGroup = function(group) {
      var users = $scope.ctx.checkList.getSelectedItems();
      if (!users || users.length == 0) {
        return;
      }

      group.removeUsers(users).then(
        function(result) {
          Alerts.success('user.group_users_removed');
          loadUsers($scope.userFilterOpts);
        }
      )
    }

    $scope.searchGroups = function(query) {
      if (ctx.defGroups && (!query || ctx.defGroups.length < 100)) {
        ctx.groups = ctx.defGroups;
        return;
      }

      UserGroup.query({query: query, listAll: false}).then(
        function(groups) {
          if (!query && !ctx.defGroups) {
            ctx.defGroups = groups;
          }

          ctx.groups = groups;
        }
      );
    }

    init();
  });
