angular.module('openspecimen')
  .factory('AuthorizationService', function($http, User, ApiUtil, ApiUrls) {
    var userRights = [];

    var currentUser = {};

    function isAllowed(allowedOps, requestedOps) {
      var allowed = false;
      for (var i = 0; i < requestedOps.length; ++i) {
        if (allowedOps.indexOf(requestedOps[i]) != -1) {
          allowed = true;
          break;
        }
      }
      return allowed;
    }

    return {
      initializeUserRights: function(user) {
        currentUser = user;

        return User.getCurrentUserRoles().then(
          function(userRoles) {
            currentUser.roles = userRoles;

            userRights = [];
            angular.forEach(userRoles, function(userRole) {
              var site = userRole.site ? userRole.site.name : null;
              var cp = userRole.collectionProtocol ? userRole.collectionProtocol.shortTitle : null;
              angular.forEach(userRole.role.acl, function(ac) {
                userRights.push({
                  site: site,
                  cp: cp,
                  resource: ac.resourceName,
                  operations: ac.operations.map(function(op) { return op.operationName; } )
                });
              });
            });
            return userRights;
          }
        );
      },

      isAllowed: function(opts) {
        if (currentUser.admin) {
          return true;
        }

        var resources = opts.resources || [opts.resource];
        var allowed = false;
        for (var i = 0; i < userRights.length; i++) {
          if (!opts.sites && !opts.cp && resources.indexOf(userRights[i].resource) != -1) {
            //
            // For resources whose rights are independent of CP and Site
            //
            allowed = isAllowed(userRights[i].operations, opts.operations);

          } else if ((!opts.sites || !userRights[i].site || opts.sites.indexOf(userRights[i].site) != -1) &&
                    (!opts.cp || !userRights[i].cp || userRights[i].cp == opts.cp) &&
                    (resources.indexOf(userRights[i].resource) != -1)) {
            //
            // For resources whose rights are specified based on CP and/or Site
            //
            allowed = isAllowed(userRights[i].operations, opts.operations);
          }

          if (allowed) {
            break;
          }
        }

        return allowed;
      },

      hasPhiAccess: function() {
        if (currentUser.admin || currentUser.instituteAdmin) {
          return true;
        }

        for (var i = 0; i < userRights.length; i++) {
          if (userRights[i].resource == 'ParticipantPhi') {
            return true;
          }
        }

        return false;
      },

      currentUser: function() {
        return currentUser;
      },

      userRights: function() {
        return userRights;
      },

      getRole: function(cp) {
        if (currentUser.admin) {
          return 'system-admin';
        }

        if (!currentUser.roles || currentUser.roles.length == 0) {
          return null;
        }

        for (var i = 0; i < currentUser.roles.length; ++i) {
          var sr = currentUser.roles[i];
          if (!sr.site && !sr.collectionProtocol) {
            return sr.role.name;
          } else if (sr.collectionProtocol && sr.collectionProtocol.id == cp.id) {
            return sr.role.name;
          } else if (sr.site && !sr.collectionProtocol) {
            if (cp.cpSites.some(function(s) { return s.siteId == sr.site.id; })) {
              return sr.role.name;
            }
          }
        }

        return null;
      }
    }
  });
