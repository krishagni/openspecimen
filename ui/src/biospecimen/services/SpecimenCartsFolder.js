
import http     from '@/common/services/HttpClient.js';
import formUtil from '@/common/services/FormUtil.js';

import folderSchema  from '@/biospecimen/schemas/cart-folders/cart-folder.js';
import addEditLayout from '@/biospecimen/schemas/cart-folders/addedit.js';

class SpecimenCartsFolder {
  async getFolders(filterOpts) {
    return http.get('specimen-list-folders', filterOpts || {});
  }

  async getFoldersCount(filterOpts) {
    return http.get('specimen-list-folders/count', filterOpts || {});
  }

  async getFolder(folderId) {
    return http.get('specimen-list-folders/' + folderId);
  }

  async saveOrUpdate(folder) {
    if (folder.id) {
      return http.put('specimen-list-folders/' + folder.id, folder);
    } else {
      return http.post('specimen-list-folders', folder);
    }
  }

  async delete(folder) {
    return http.delete('specimen-list-folders/' + folder.id);
  }

  async addCarts(folder, carts) {
    const ids = (carts || []).map(cart => cart.id);
    if (ids.length == 0) {
      return {count: 0};
    }

    return http.put('specimen-list-folders/' + folder.id + '/carts', ids, {operation: 'ADD'});
  }

  async removeCarts(folder, carts) {
    const ids = (carts || []).map(cart => cart.id);
    if (ids.length == 0) {
      return {count: 0};
    }

    return http.put('specimen-list-folders/' + folder.id + '/carts', ids, {operation: 'REMOVE'});
  }

  getAddEditFormSchema() {
    const addEditFs = formUtil.getFormSchema(folderSchema.fields, addEditLayout.layout);
    return { schema: addEditFs };
  }
}

export default new SpecimenCartsFolder();
