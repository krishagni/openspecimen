
import http from '@/common/services/HttpClient.js';

class CollectionProtocol {

  workflows = {};

  async getCpById(cpId) {
    return http.get('collection-protocols/' + cpId);
  }

  async getCps(filterOpts) {
    return http.get('collection-protocols', filterOpts || {});
  }

  async getCpsForRegistrations(sites, query) {
    const filterOpts = {
      siteName: sites,
      resource: 'ParticipantPhi',
      op: 'Create',
      title: query
    };

    return http.get('collection-protocols/byop', filterOpts || {});
  }

  async getCpe(eventId) {
    return http.get('collection-protocol-events/' + eventId);
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

  async getWorkflowProperty(cpId, wfName, propName) {
    const wf = await this.getWorkflow(cpId, wfName);
    if (wf && wf[propName]) {
      return wf[propName];
    } else if (cpId == -1 || !cpId) {
      return null;
    }

    const sysWf = await this.getWorkflow(-1, wfName);
    return sysWf && sysWf[propName];
  }

  async loadWorkflows(cpId, wfName) {
    let workflow = this.workflows[cpId];
    if (!workflow) {
      workflow = await http.get('collection-protocols/' + cpId + '/workflows');
      workflow = this.workflows[cpId] = workflow.workflows;
    }

    return workflow[wfName];
  }

  async getSpecimenTreeCfg(cpId) {
    return this.getWorkflow(-1, 'specimenTree').then(
      (sysTreeCfg) => {
        const result = Object.assign({}, sysTreeCfg || {});
        if (cpId == -1) {
          return result;
        }

        return this.getWorkflow(cpId, 'specimenTree').then(cpTreeCfg => Object.assign(result, cpTreeCfg || {}));
      }
    );
  }

  async isBarcodingEnabled() {
    return http.get('collection-protocols/barcoding-enabled').then(resp => resp == true || resp == 'true');
  }

  async getLockedFields(cpId, entityType, source) {
    return this.getWorkflowProperty(cpId, 'locked-fields', entityType).then(
      sources => sources && sources[source] instanceof Array ? sources[source] : []
    );
  }
}

export default new CollectionProtocol();
