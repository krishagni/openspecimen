
import http from '@/common/services/HttpClient.js';
import util from '@/common/services/Util.js';

class CpWorkflow {
  workflows = {};

  async getDictionary(cpId) {
    return this.getWorkflow(cpId, 'dictionary').then(
      function(data) {
        data = data || {};
        return data.fields || [];
      }
    );
  }

  async getWorkflow(cpId, wfName) {
    let workflow = await this._loadWorkflows(cpId, wfName);
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

    workflow = await this._loadWorkflows(-1, wfName);
    return workflow && workflow.data;
  }

  overrideFields(baseFields, fields) {
    const baseFieldsMap       = this._objLookupMap(baseFields);
    const overriddenFieldsMap = this._objLookupMap(fields, 'baseField');
    return fields.map(field => this._overrideField(field, baseFieldsMap, overriddenFieldsMap));
  }

  async _loadWorkflows(cpId, wfName) {
    let workflow = this.workflows[cpId];
    if (!workflow) {
      workflow = await http.get('collection-protocols/' + cpId + '/workflows');
      workflow = this.workflows[cpId] = workflow.workflows;
    }

    return workflow[wfName];
  }

  _objLookupMap(objs, prop) {
    prop = prop || 'name';

    const map = {};
    objs.forEach(
      (obj) => {
        if (obj[prop]) {
          map[obj[prop]] = obj;
        }
      }
    );

    return map;
  }

  _overrideField(field, baseFieldsMap, overriddenFieldsMap) {
    field = util.clone(field);
    if (!field.baseField) {
      return field;
    }

    const baseField = util.clone(baseFieldsMap[field.baseField] || {});
    if (!!baseField.showIf && !field.showIf) {
      baseField.showIf.rules.forEach(
        (rule) => {  
          if (overriddenFieldsMap[rule.field]) {
            rule.field = overriddenFieldsMap[rule.field].name;
          }

          if (overriddenFieldsMap[rule.value]) {
            rule.value = overriddenFieldsMap[rule.value].name;
          }
        }
      );
    }

    field.formatType = baseField.type;

    const result = {...baseField, ...field};
    return {...field, ...result};
  }
}

export default new CpWorkflow();
