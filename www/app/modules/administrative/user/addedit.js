angular.module('os.administrative.user.addedit', ['os.administrative.models'])
  .controller('UserAddEditCtrl', function(
    $scope, $rootScope, $state, $stateParams, user, users, currentUser,
    User, Institute, AuthDomain, Util, TimeZone, LocationChangeListener, ItemsHolder, VueApp) {

    var instituteSites = {}, prevInstitute;

    function init() {
      if (!$stateParams.old || $stateParams.old == 'false') {
        var url = 'user-addedit';
        if (+$stateParams.userId > 0) {
          url += '/' + $stateParams.userId;
        } else {
          url += '/-1';
        }

        if (users && users.length > 0) {
          ItemsHolder.setItems('users', users);
        }
 
        VueApp.setVueView(url, {});
        return;
      }

      prevInstitute = user.instituteName;

      $scope.user = user;
      $scope.signedUp = false;
      loadPvs();

      $scope.disabledFields = {fields: {}};

      if (user.$$editProfile && user.type != 'SUPER') {
        [
          'firstName', 'lastName', 'emailAddress', 'domainName', 'loginName',
          'instituteName', 'primarySite', 'type', 'manageForms'
        ].forEach(function(f) { $scope.disabledFields.fields['user.' + f] = true; });
      }
    }

    function loadPvs() {
      $scope.domains = [];
      AuthDomain.getDomainNames().then(
        function(domains) {
          $scope.domains = domains;
          if (!$scope.user.id && $scope.domains.length == 1) {
            $scope.user.domainName = $scope.domains[0];
          }
        }
      );

      if (!currentUser) {
        return;
      }

      $scope.timeZones = [];
      TimeZone.query().then(
        function(timeZones) {
          $scope.timeZones = timeZones;
        }
      );
    }

    function loadSites(instituteName, siteName) {
      if (!instituteName || instituteName.length == 0) {
        $scope.sites = [];
        return;
      }

      var sites = instituteSites[instituteName];
      if (sites && sites.length < 100) {
        $scope.sites = sites;
        return;
      }

      Institute.getSites(instituteName, siteName).then(
        function(sites) {
          $scope.sites = sites.map(function(site) { return site.name });
          if (!siteName) {
            instituteSites[instituteName] = $scope.sites;
          }
        }
      );
    }

    function saveUser() {
      var user = angular.copy($scope.user);
      user.$saveOrUpdate().then(
        function(savedUser) {
          if ($scope.user.$$editProfile) {
            angular.extend(currentUser, savedUser);
            $state.go('home', {}, {reload: true});
          } else {
            $state.go('user-detail.overview', {userId: savedUser.id});
          }
        }
      );
    }

    function bulkUpdate() {
      var userIds = users.map(function(user) { return user.id; });
      User.bulkUpdate({detail: $scope.user, ids: userIds}).then(
        function(savedUsers) {
          // $state.go('user-list');
          $scope.back();
        }
      );
    }

    $scope.onInstituteSelect = function(instituteName) {
      $scope.user.primarySite = undefined;
      loadSites(instituteName);
    }

    $scope.searchSites = function(siteName) {
      if (!$scope.user.instituteName) {
        return;
      }

      loadSites($scope.user.instituteName, siteName);
    }

    $scope.onContactTypeSelect = function() {
      $scope.user.manageForms = false;
      $scope.user.domainName = undefined;
      $scope.user.loginName = undefined;
    }

    $scope.createUser = function() {
      if (!$scope.user.id || $scope.user.instituteName == prevInstitute) {
        saveUser();
        return;
      }

      Util.showConfirm({
        isWarning: true,
        title: 'user.confirm_institute_update_title',
        confirmMsg: 'user.confirm_institute_update_q',
        input: {count: 1, users: [$scope.user]},
        ok: saveUser
      });
    };

    $scope.signup = function() {
      var user = angular.copy($scope.user);
      User.signup(user).then(
        function(resp) {
          if (resp.status == 'ok') {
            $scope.signedUp = true;
          }
        }
      )
    };

    $scope.bulkUpdate = function() {
      var instituteChange = users.some(function(u) { return u.instituteName != $scope.user.instituteName; });
      if (!instituteChange) {
        bulkUpdate();
        return;
      }

      Util.showConfirm({
        isWarning: true,
        title: 'user.confirm_institute_update_title',
        confirmMsg: 'user.confirm_institute_update_q',
        input: {count: users.length, users: users},
        ok: bulkUpdate
      });
    }
     
    init();
  });
