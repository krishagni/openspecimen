
import http from '@/common/services/HttpClient.js';

class CollectionProtocol {

  workflows = {};

  async getCps(filterOpts) {
    return http.get('collection-protocols', filterOpts || {});
  }

  async getWorkflow(cpId, wfName) {
    let workflow = await this.loadWorkflows(cpId, wfName);
    if (workflow && workflow.data) {
      if (workflow.data instanceof Array) {
        return workflow.data.length > 0 ? workflow.data : null;
      } else if (Object.keys(workflow.data).length > 0) {
        return workflow.data;
      }
    }

    if (cpId == -1) {
      return null;
    }

    workflow = await this.loadWorkflows(-1, wfName);
    return workflow && workflow.data;
  }

  async loadWorkflows(cpId, wfName) {
    let workflow = this.workflows[cpId];
    if (!workflow) {
      workflow = await http.get('collection-protocols/' + cpId + '/workflows');
      workflow = this.workflows[cpId] = workflow.workflows;
    }

    return workflow[wfName];
  }

  async isBarcodingEnabled() {
    return http.get('collection-protocols/barcoding-enabled').then(resp => resp == true || resp == 'true');
  }
}

export default new CollectionProtocol();

