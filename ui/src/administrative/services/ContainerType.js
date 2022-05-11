
import formUtil from '@/common/services/FormUtil.js';
import http     from '@/common/services/HttpClient.js';
import util     from '@/common/services/Util.js';

import addEditLayout  from '@/administrative/schemas/container-types/addedit.js';
import typeSchema     from '@/administrative/schemas/container-types/type.js';

class ContainerType {
  async getTypes(filterOpts) {
    return http.get('container-types', filterOpts || {});
  }

  async getTypesCount(filterOpts) {
    return http.get('container-types/count', filterOpts || {});
  }

  async getType(typeId) {
    return http.get('container-types/' + typeId);
  }

  async saveOrUpdate(type) {
    if (type.id) {
      return http.put('container-types/' + type.id, type);
    } else {
      return http.post('container-types', type);
    }
  }

  async delete(type) {
    return http.delete('container-types/' + type.id, {}, {});
  }

  async bulkDelete(typeIds) {
    return http.delete('container-types', {}, {id: typeIds});
  }

  getDict() {
    return util.clone(typeSchema.fields);
  }

  getAddEditFormSchema() {
    const addEditFs = formUtil.getFormSchema(typeSchema.fields, addEditLayout.layout);
    return { schema: addEditFs };
  }
}

export default new ContainerType();
