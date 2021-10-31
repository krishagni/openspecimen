
import ui from '@/global.js';
import authSvc from '@/common/services/Authorization.js';

class UserResources {
  createOpts    = {resource: 'User', operations: ['Create']};

  updateOpts    = {resource: 'User', operations: ['Update']};

  deleteOpts    = {resource: 'User', operations: ['Delete']};

  importOpts    = {resource: 'User', operations: ['Export Import']};

  isUpdateAllowed() {
    return authSvc.isAllowed({resource: 'User', operations: ['Update']});
  }

  isFormUpdateAllowed(entityType, userId) {
    if (entityType == 'User') {
      return this.isUpdateAllowed();
    } else if (entityType == 'UserProfile') {
      return ui.currentUser.id == userId || this.isUpdateAllowed();
    }
  }
}

export default new UserResources();
