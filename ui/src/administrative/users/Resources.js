
import authSvc from '@/common/services/Authorization.js';

class UserResources {
  createOpts    = {resource: 'User', operations: ['Create']};

  updateOpts    = {resource: 'User', operations: ['Update']};

  deleteOpts    = {resource: 'User', operations: ['Delete']};

  importOpts    = {resource: 'User', operations: ['Export Import']};

  isUpdateAllowed() {
    return authSvc.isAllowed({resource: 'User', operations: ['Update']});
  }
}

export default new UserResources();
