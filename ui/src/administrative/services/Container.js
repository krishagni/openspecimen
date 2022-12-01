
import formUtil from '@/common/services/FormUtil.js';
import http     from '@/common/services/HttpClient.js';
import util     from '@/common/services/Util.js';
import formSvc  from '@/forms/services/Form.js';

import addEditLayout      from '@/administrative/schemas/containers/addedit.js';
import containerSchema    from '@/administrative/schemas/containers/container.js';
import specimenSchema     from '@/administrative/schemas/containers/specimen.js';
import transferFormSchema from '@/administrative/schemas/containers/transfer.js';
import defragFormSchema   from '@/administrative/schemas/containers/defragment.js';
import locationsSchema    from '@/administrative/schemas/containers/locations.js';

import taskAddEditLayout  from '@/administrative/schemas/container-tasks/addedit.js';
import taskSchema         from '@/administrative/schemas/container-tasks/task.js';

class Container {
  async getContainers(filterOpts) {
    return http.get('storage-containers', filterOpts || {});
  }

  async getContainersCount(filterOpts) {
    return http.get('storage-containers/count', filterOpts || {});
  }

  async getContainer(containerId, includeStats) {
    return http.get('storage-containers/' + containerId, {includeStats: includeStats});
  }

  async getContainerByName(name) {
    return http.get('storage-containers/byname', { name });
  }

  async saveOrUpdate(container) {
    if (!container.id) {
      return http.post('storage-containers', container);
    } else {
      return http.put('storage-containers/' + container.id, container);
    }
  }

  async bulkUpdate(containers) {
    return http.put('storage-containers/bulk-update', containers);
  }

  async createContainers(containers) {
    return http.post('storage-containers/multiple', containers);
  }

  async createHierarchy(container) {
    return http.post('storage-containers/create-hierarchy', container);
  }

  async delete(container, forceDelete = false) {
    return http.delete('storage-containers/' + container.id, {}, {forceDelete: forceDelete});
  }

  async getDependents(container) {
    return http.get('storage-containers/' + container.id + '/dependent-entities');
  }

  async getCustomFieldsForm() {
    const extnInfo = await http.get('storage-containers/extension-form');
    if (!extnInfo || !extnInfo.formId) {
      return null;
    }

    return formSvc.getDefinition(extnInfo.formId);
  }

  async getOccupiedPositions(container) {
    return http.get('storage-containers/' + container.id + '/occupied-positions');
  }

  async printLabels(containerIds) {
    return http.post('container-label-printer', {containerIds});
  }

  async bulkDelete(containerIds, forceDelete = true) {
    return http.delete('storage-containers', {}, {id: containerIds, forceDelete: forceDelete});
  }

  async getChildContainers(container) {
    return http.get('storage-containers/' + container.id + '/child-containers');
  }

  async getAncestorsHierarchy(container) {
    return http.get('storage-containers/' + container.id + '/ancestors-hierarchy');
  }

  async star(containerId) {
    return http.post('storage-containers/' + containerId + '/labels');
  }

  async unstar(containerId) {
    return http.delete('storage-containers/' + containerId + '/labels');
  }

  async exportMap(container) {
    return http.post('storage-containers/' + container.id + '/export-map', {});
  }

  async exportEmptyPositions(container) {
    return http.post('storage-containers/' + container.id + '/export-empty-positions', {});
  }

  async exportUtilisation(container) {
    return http.post('storage-containers/' + container.id + '/export-utilisation', {});
  }

  async exportDefragReport(container, defragDetail) {
    return http.post('storage-containers/' + container.id + '/defragment', defragDetail);
  }

  downloadReport(fileId) {
    http.downloadFile(http.getUrl('storage-containers/report', {query: {fileId}}));
  }

  blockPositions(container, positions) {
    return http.put('storage-containers/' + container.id + '/block-positions', positions);
  }

  unblockPositions(container, positions) {
    return http.put('storage-containers/' + container.id + '/unblock-positions', positions);
  }

  assignPositions(container, positions) {
    return http.post('storage-containers/' + container.id + '/occupied-positions', positions);
  }

  getVacantPositions({name, positionY, positionX, position}, count) {
    const params = {
      name: name,
      startRow: positionY,
      startColumn: positionX,
      startPosition: position,
      numPositions: count
    };
    return http.get('storage-containers/vacant-positions', params);
  }

  generateSpecimensReport(container) {
    return http.get('storage-containers/' + container.id + '/report');
  }

  getTransferEvents(container) {
    return http.get('storage-containers/' + container.id + '/transfer-events');
  }

  getScheduledActivities(container) {
    return http.get('scheduled-container-activities', {containerId: container.id});
  }

  saveOrUpdateScheduledActivity(activity) {
    if (activity.id) {
      return http.put('scheduled-container-activities/' + activity.id, activity);
    } else {
      return http.post('scheduled-container-activities', activity);
    }
  }

  archiveScheduledActivity(activity) {
    const payload = {...activity};
    payload.activityStatus = 'Closed';
    return http.put('scheduled-container-activities/' + payload.id, payload);
  }

  getActivityLogs(container) {
    return http.get('container-activity-logs', {containerId: container.id});
  }

  saveOrUpdateActivityLog(activityLog) {
    if (activityLog.id) {
      return http.put('container-activity-logs/' + activityLog.id, activityLog);
    } else {
      return http.post('container-activity-logs', activityLog);
    }
  }

  archiveActivityLog(activityLog) {
    const payload = {...activityLog};
    payload.activityStatus = 'Closed';
    return http.put('container-activity-logs/' + payload.id, payload);
  }

  getTasks(filterOpts) {
    return http.get('container-tasks', filterOpts || {});
  }

  getTasksCount(filterOpts) {
    return http.get('container-tasks/count', filterOpts || {});
  }

  getTask(taskId) {
    return http.get('container-tasks/' + taskId);
  }

  saveOrUpdateTask(task) {
    if (task.id) {
      return http.put('container-tasks/' + task.id, task);
    } else {
      return http.post('container-tasks', task);
    }
  }

  archiveTask(task) {
    const payload = {...task};
    payload.activityStatus = 'Closed';
    return http.put('container-tasks/' + payload.id, payload);
  }

  async getSpecimen(specimenId) {
    return http.get('specimens/' + specimenId);
  }

  async getDict() {
    const result  = util.clone(containerSchema.fields);
    const formDef = await this.getCustomFieldsForm();
    const customFields = formUtil.deFormToDict(formDef, 'container.extensionDetail.attrsMap.');
    return result.concat(customFields);
  }

  getAddEditFormSchema() {
    return this.getCustomFieldsForm().then(
      function(formDef) {
        const addEditFs = formUtil.getFormSchema(containerSchema.fields, addEditLayout.layout);
        const { schema, defaultValues } = formUtil.fromDeToStdSchema(formDef, 'container.extensionDetail.attrsMap.');
        addEditFs.rows = addEditFs.rows.concat(schema.rows);
        return { schema: addEditFs, defaultValues };
      }
    );
  }

  getSummaryDict() {
    return util.clone(containerSchema.fields.filter(field => field.summary == true));
  }

  getSpecimenDict() {
    return util.clone(specimenSchema.fields);
  }

  getTransferFormSchema() {
    return transferFormSchema;
  }

  getDefragFormSchema() {
    return defragFormSchema;
  }

  getLocationsSchema() {
    return locationsSchema;
  }

  getTaskAddEditFormSchema() {
    const addEditFs = formUtil.getFormSchema(taskSchema.fields, taskAddEditLayout.layout);
    return { schema: addEditFs };
  }
}

export default new Container();
