
import ui from '@/global.js';
import http from '@/common/services/HttpClient.js';

class Authorization {

  userRights = [];

  async loadUserRights() {
    this.userRights = [];
    return http.get('users/current-user-roles').then(
      (userRoles) => {
        this.userRights = [];
        for (let userRole of userRoles) {
          let site = userRole.site ? userRole.site.name : null;
          let cp = userRole.collectionProtocol ? userRole.collectionProtocol.shortTitle : null;
          for (let ac of userRole.role.acl || []) {
            this.userRights.push({
              site: site,
              cp: cp,
              resource: ac.resourceName,
              operations: ac.operations.map(function(op) { return op.operationName; } )
            });
          }
        }

        return this.userRights;
      }
    );
  }

  isAllowed(opts) {
    if (ui.currentUser.admin) {
      return true;
    }

    if (opts == 'admin') {
      return false;
    } else if (opts == 'institute-admin') {
      return ui.currentUser.instituteAdmin == true;
    }

    let resources = opts.resources || [opts.resource];
    let allowed = false;
    for (let right of this.userRights) {
      if (!opts.sites && !opts.cp && resources.indexOf(right.resource) != -1) {
        //
        // For resources whose rights are independent of CP and Site
        //
        allowed = this.isPresent(right.operations, opts.operations);
      } else if ((!opts.sites || !right.site || opts.sites.indexOf(right.site) != -1) &&
        (!opts.cp || !right.cp || right.cp == opts.cp) &&
        (resources.indexOf(right.resource) != -1)) {
        //
        // For resources whose rights are specified based on CP and/or Site
        //
        allowed = this.isPresent(right.operations, opts.operations);
      }

      if (allowed) {
        break;
      }
    }

    return allowed;
  }

  isPresent(main, test) {
    for (let op of test) {
      if (main.indexOf(op) != -1) {
        return true;
      }
    }

    return false;
  }
}

export default new Authorization();
