
import http from '@/common/services/HttpClient.js';

class CollectionProtocolGroup {
  getGroups(opts) {
    return http.get('collection-protocol-groups', opts || {});
  }

  getGroupsCount(opts) {
    return http.get('collection-protocol-groups/count', opts || {});
  }

  getGroupById(id) {
    return http.get('collection-protocol-groups/' + id);
  }

  saveOrUpdate(cpg) {
    if (cpg.id > 0) {
      return http.put('collection-protocol-groups/' + cpg.id, cpg);
    } else {
      return http.post('collection-protocol-groups', cpg);
    }
  }

  deleteCpg(cpg) {
    return http.delete('collection-protocol-groups/' + cpg.id);
  }

  getForms(groupId, level) {
    return http.get('collection-protocol-groups/' + groupId + '/forms', {level})
  }

  addForms(groupId, level, forms) {
    const payload = {level, forms};
    return http.post('collection-protocol-groups/' + groupId + '/forms', payload);
  }

  removeForms(groupId, level, formIds) {
    const formIdQp = formIds.map(formId => 'formId=' + formId).join('&');
    const qp = 'level=' + level + '&' + formIdQp;
    return http.delete('collection-protocol-groups/' + groupId + '/forms?' + qp);
  }

  exportWorkflows(cpg) {
    http.downloadFile(http.getUrl('collection-protocol-groups/' + cpg.id + '/workflows-file'));
  }
}

export default new CollectionProtocolGroup();
