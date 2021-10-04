
angular.module('os.administrative.models.user', ['os.common.models'])
  .factory('User', function(osModel, $http, $translate, Form, ApiUtil, ApiUrls) {
    var User =
      osModel(
        'users',
        function(user) {
          user.roleModel =  osModel('rbac/subjects/' + user.$id() + '/roles');
          user.roleModel.prototype.$saveProps = saveRoleProps;
        }
      );

    function saveRoleProps() {
      var userRole = angular.copy(this);
      var all = $translate.instant('user.role.all');
      if (userRole.site == all) {
        delete userRole.site;
      } else {
        userRole.site = {name: userRole.site};
      }

      if (userRole.collectionProtocol == all) {
        delete userRole.collectionProtocol;
      } else {
        userRole.collectionProtocol = {shortTitle: userRole.collectionProtocol};
      }

      userRole.role = {name: userRole.role};
      delete userRole.isUpdateAllowed;
      return userRole;
    }

    User.prototype.getType = function() {
      return 'user';
    }

    User.prototype.getDisplayName = function() {
      return this.firstName + ' ' + this.lastName;
    }

    User.prototype.getInstitute = function() {
      return $http.get(User.url() + this.id + '/institute').then(
        function(result) {
          return result.data;
        }
      );
    }

    User.prototype.impersonate = function() {
      var promise;
      if (this.id > 0) {
        promise = $http.post(ApiUrls.getBaseUrl() + 'sessions/impersonate', {userId: this.id});
      } else {
        promise = $http.delete(ApiUrls.getBaseUrl() + 'sessions/impersonate');
      }

      return promise.then(
        function(resp) {
          return resp.data && resp.data.impersonateUserToken;
        }
      );
    }

    User.prototype.getRoles = function() {
      var that = this;
      return this.roleModel.query().then(
        function(roles) {
          that.roles = roles;
          return roles;
        }
      );;
    }

    User.prototype.newRole = function(role) {
      return new this.roleModel(role);
    }

    User.prototype.updateStatus = function(status) {
      return $http.put(User.url() + this.$id() + '/activity-status', {activityStatus: status})
        .then(User.modelRespTransform);
    }

    // opts => {cpId: <cpId>, sites: [site]}
    User.prototype.hasRole = function(role, opts) {
      if (!this.roles || this.roles.length == 0) {
        return false;
      }

      var matchingRoles = [];
      for (var i = 0; i < this.roles.length; ++i) {
        var userRole = this.roles[i];
        if (userRole.role.name != role) {
          continue;
        }

        if (!userRole.collectionProtocol && !userRole.site) {
          return true;
        } else if (!opts || (!opts.cpId && !(opts.sites && opts.sites.length > 0))) {
          return true;
        } else if (opts.cpId && userRole.collectionProtocol && opts.cpId != userRole.collectionProtocol.id) {
          continue;
        } else if (opts.sites && opts.sites.length > 0 && userRole.site && opts.sites.indexOf(userRole.site.name) == -1) {
          continue;
        }

        return true;
      }

      return false;
    }

    User.prototype.getForms = function(params) {
      return Form.listFor(User.url(), this.$id(), params);
    }

    User.prototype.getRecords = function() {
      var url = User.url() + this.$id() + '/form-records';
      return Form.listRecords(url);
    }

    User.sendPasswordResetLink = function(user) {
      return $http.post(User.url() + 'forgot-password', user).then(ApiUtil.processResp);
    }

    User.resetPassword = function(passwordDetail) {
      return $http.post(User.url() + "reset-password", passwordDetail).then(ApiUtil.processResp);
    }

    User.changePassword = function(passwordDetail) {
      return $http.put(User.url() + "password", passwordDetail).then(ApiUtil.processResp);
    }

    User.getCurrentUser = function() {
      return $http.get(User.url() + 'current-user').then(User.modelRespTransform);
    }

    User.getUiState = function() {
      return $http.get(User.url() + 'current-user-ui-state').then(
        function(resp) {
          return resp.data;
        }
      );
    }

    User.saveUiState = function(uiState) {
      return $http.put(User.url() + 'current-user-ui-state', uiState).then(
        function(resp) {
          return resp.data;
        }
      );
    }

    User.signup = function(user) {
      return $http.post(User.url() + 'sign-up', user).then(ApiUtil.processResp);
    }

    User.activate = function(id) {
      return $http.put(User.url() + id + '/activity-status', {activityStatus: 'Active'}).then(User.modelRespTransform);
    }

    User.getCurrentUserRoles = function() {
      return $http.get(User.url() + 'current-user-roles').then(
        function(result) {
          return result.data;
        }
      );
    }

    User.broadcastAnnouncement = function(announcement) {
      return $http.post(User.url() + 'announcements', announcement).then(
        function(result) {
          return result.data;
        }
      );
    }

    User.bulkUpdate = function(detail) {
      return $http.put(User.url() + 'bulk-update', detail).then(User.modelArrayRespTransform);
    }

    User.bulkDelete = function(userIds) {
      return User.bulkUpdate({detail: {activityStatus: 'Disabled'}, ids: userIds});
    }

    return User;
  });

