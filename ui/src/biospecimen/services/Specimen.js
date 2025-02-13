
import alertsSvc   from '@/common/services/Alerts.js';
import cpSvc       from './CollectionProtocol.js';
import exprUtil    from '@/common/services/ExpressionUtil.js';
import http        from '@/common/services/HttpClient.js';
import formSvc     from '@/forms/services/Form.js';
import routerSvc   from '@/common/services/Router.js';
import settingsSvc from '@/common/services/Setting.js';
import ui          from '@/global.js';
import util        from '@/common/services/Util.js';

import specimenSchema from '@/biospecimen/schemas/specimens/specimen.js';
import addEditLayout from '@/biospecimen/schemas/specimens/addedit.js';

class Specimen {
  search(criteria) {
    return http.post('specimens/search', criteria);
  }

  getByIds(ids, includeExtensions, minimalInfo) {
    return this.search({
      ids: ids,
      includeExtensions: includeExtensions || false,
      minimalInfo: minimalInfo || false,
      maxResults: ids.length
    });
  }

  getById(id) {
    return http.get('specimens/' + id);
  }

  saveOrUpdate(specimen) {
    if (specimen.id > 0) {
      return http.put('specimens/' + specimen.id, specimen);
    } else {
      return http.post('specimens/', specimen);
    }
  }

  async deleteSpecimen(specimenId, forceDelete, reason) {
    return http.delete('specimens/' + specimenId, {}, {forceDelete, reason});
  }

  async getDependents(specimen) {
    return http.get('specimens/' + specimen.id + '/dependent-entities');
  }

  clearSpecimens(visit) {
    visit.$specimens = null;
  }

  async printLabels(input, outputFilename) {
    const job = await http.post('specimen-label-printer', input);

    let downloadEnabled = ui.currentUser.downloadLabelsPrintFile;
    if (!downloadEnabled) {
      const setting = await settingsSvc.getSetting('administrative', 'download_labels_print_file');
      downloadEnabled = util.isTrue(setting[0].value);
    }

    if (downloadEnabled) {
      let url = http.getUrl('specimen-label-printer/output-file', {query: {jobId: job.id}});
      if (outputFilename) {
        outputFilename = outputFilename.replace(/\/|\\/g, '_');
        url += '&filename=' + outputFilename;
      }

      http.downloadFile(url);
      alertsSvc.info('Downloading labels print CSV file...');
    } else {
      alertsSvc.success('Specimen labels print job ' + job.id + ' created.');
    }

    return job;
  }

  async bulkUpdate(specimens) {
    return http.put('specimens', specimens);
  }

  /* Used by bulk edit UI. Should not be used by other components. Use above bulkUpdate() function */
  async bulkEdit(spmnIds, specimen) {
    const payload = {ids: spmnIds, detail: specimen};
    return http.put('specimens/bulk-update', payload);
  }

  async bulkDelete(specimenIds, reason) {
    return http.delete('specimens', {}, {id: specimenIds, reason: reason})
  }

  async bulkUpdateStatus(specs) {
    return http.put('specimens/status', specs);
  }

  async allocatePositions(ctxt, items, reservationToCancel) {
    const op = await this._getReservePositionsOp(ctxt, items);
    op.reservationToCancel = reservationToCancel;
    return http.post('storage-containers/reserve-positions', op).then(
      (positions) => {
        this._assignReservedPositions(op, positions);

        const position = positions.find(position => position && position.reservationId);
        return position && position.reservationId;
      }
    );
  }

  async cancelReservation(reservationId) {
    return http.delete('storage-containers/reserve-positions?reservationId=' + reservationId);
  }

  async getLabelFormat(ctxt, specimen) {
    if (specimen.labelFmt) {
      return specimen.labelFmt;
    }

    ctxt.labelFormatRules = ctxt.labelFormatRules || {};

    let rules = ctxt.labelFormatRules[specimen.cpId];
    if (!rules) {
      let wf = await cpSvc.getWorkflow(specimen.cpId, 'labelSettings');
      rules = (wf && wf.specimen && wf.specimen.rules) || [];
      rules = rules.map(
        rule => ({criteria: rule.criteria && rule.criteria.replace(/#(specimen)|#(cpId)/g, '$1'), format: rule.format})
      );

      ctxt.labelFormatRules[specimen.cpId] = rules;
    }

    let input = {cpId: specimen.cpId, specimen};
    for (let rule of rules) {
      if (!rule.criteria || exprUtil.eval(input, rule.criteria)) {
        return rule.format;
      }
    }

    return null;
  }

  getEvents({id}) {
    return http.get('specimens/' + id + '/events').then(
      records => {
        const result = [];
        for (let ev of records) {
          for (let record of ev.records) {
            let user = null, time = null;
            for (let {name, value} of record.fieldValues) {
              if (name == 'user') {
                user = value;
              } else if (name == 'time') {
                time = value && new Date(value);
              }
            }

            result.push({
              id: record.recordId,
              formId: ev.id,
              formCtxtId: record.fcId,
              sysForm: record.sysForm,
              name: ev.caption,
              updatedBy: record.user,
              updateTime: record.updateTime,
              user: user,
              time: time,
              isEditable: !record.sysForm || ev.name == 'SpecimenCollectionEvent' || ev.name == 'SpecimenReceivedEvent'
            });
          }
        }

        result.sort(
          (e1, e2) => {
            const t1 = e1.time || 0;
            const t2 = e2.time || 0;
            const diff = t2 - t1;
            return diff != 0 ? diff : (e2.id - e1.id);
          }
        );

        return result;
      }
    );
  }

  async getDict(cpId) {
    const aliases = ['specimen', 'calcSpecimen'];
    return cpSvc.getDictFor(
      cpId, aliases, 'specimen.extensionDetail',
      specimenSchema, this.getCustomFieldsForm
    ).then(
      fields => {
        fields = fields.filter(field => field.name.indexOf('specimen.events') == -1)
        for (let field of fields) {
          if (field.href) {
            continue;
          }

          if (field.name == 'specimen.label' || field.name == 'specimen.barcode') {
            field.href = ({specimen: {cpId, cprId, visitId, id, eventId, reqId}}) =>
              id && routerSvc.getUrl(
                'ParticipantsListItemSpecimenDetail.Overview',
                {cpId, cprId, visitId, specimenId: id},
                {eventId, reqId}
              );
          } else if (field.name == 'specimen.parentLabel') {
            field.href = ({specimen: {cpId, cprId, visitId, parentId, eventId}}) =>
              parentId && routerSvc.getUrl(
                'ParticipantsListItemSpecimenDetail.Overview',
                {cpId, cprId, visitId, specimenId: parentId},
                {eventId}
              );
          } else if (field.name == 'specimen.storageLocation') {
            field.href = ({specimen: {storageLocation}}) => {
              if (storageLocation && storageLocation.id > 0) {
                return routerSvc.getUrl('ContainerDetail.Locations', {containerId: storageLocation.id});
              } else {
                return null;
              }
            }
          }
        }

        return fields;
      }
    );
  }

  async getLayout(cpId, specimenFields) {
    return cpSvc.getLayoutFor(cpId, 'specimen', 'specimen.extensionDetail', addEditLayout.layout, specimenFields);
  }

  async getCustomFieldsForm(cpId) {
    return http.get('specimens/extension-form', { cpId }).then(
      (resp) => {
        if (!resp || !resp.formId) {
          return null;
        }

        return formSvc.getDefinition(resp.formId);
      }
    );
  }

  async getEventForms(spmnId) {
    return http.get('specimens/' + spmnId + '/forms', {entityType: 'SpecimenEvent'});
  }

  async getFormDataEntryRules(cpId) {
    return cpSvc.getWorkflow(cpId, 'formDataEntryRules').then(wf => (wf && wf['specimen']) || []);
  }

  async getEventFormRules(cpId) {
    return cpSvc.getWorkflow(cpId, 'formDataEntryRules').then(wf => (wf && wf['specimenEvent']) || []);
  }

  async getFormsOrderSpec(cpId) {
    return cpSvc.getWorkflow(cpId, 'forms').then(
      wf => {
        if (!wf) {
          wf = {};
        }

        return [ {type: 'Specimen', forms: wf['Specimen'] || []} ];
      }
    );
  }

  getForms({id: specimenId}) {
    return http.get('specimens/' + specimenId + '/forms');
  }

  getFormRecords({id: specimenId}) {
    return http.get('specimens/' + specimenId + '/extension-records').then(
      (formRecords) => {
        const result = [];
        for (let {id, caption, records} of formRecords) {
          for (let record of records || []) {
            record.formId = id;
            record.formCaption = caption;
            result.push(record);
          }
        }

        result.sort(({updateTime: t1}, {updateTime: t2}) => +t2 - +t1);
        return result;
      }
    );
  }

  async _getAllocRule(ctxt, specimen) {
    ctxt.allocRules = ctxt.allocRules || {};

    let rules = ctxt.allocRules[specimen.cpId];
    if (!rules) {
      let allocRules = await cpSvc.getWorkflow(specimen.cpId, 'auto-allocation');
      if (!allocRules || !allocRules.rules) {
        allocRules = {rules: []};
      }

      rules = ctxt.allocRules[specimen.cpId] = allocRules.rules;
    }

    let result = -1;
    for (let i = 0; i < rules.length; ++i) {
      if (exprUtil.eval({ specimen }, rules[i].criteria)) {
        result = i;
        break;
      }
    }

    return {index: result, rule: result != -1 ? rules[result] : undefined};
  }

  async _getReservePositionsOp(ctxt, items) {
    const aliquots = {}, result = [];

    for (let {specimen} of items) {
      const {lineage, storageType, specimenClass, type, status} = specimen;
      if (storageType == 'Virtual' || status != 'Collected') {
        continue;
      }

      let selectorCrit;
      const allocRule = await this._getAllocRule(ctxt, specimen);
      if (lineage == 'Aliquot') {
        let st = (specimenClass || 'u') + '-' + (type || 'u');
        let key = 'u-' + st + '-' + allocRule.index;
        if (specimen.parentId) {
          key = specimen.parentId + '-' + st + '-' + allocRule.index;
        } else if (specimen.parentUid) {
          key = specimen.parentUid + '-' + st + '-' + allocRule.index;
        }

        selectorCrit = aliquots[key];
        if (!selectorCrit) {
          aliquots[key] = selectorCrit = this._getSelectorCriteria(allocRule.rule, specimen);
          result.push(selectorCrit);
        }
      } else {
        selectorCrit = this._getSelectorCriteria(allocRule.rule, specimen);
        result.push(selectorCrit);
      }

      selectorCrit.minFreePositions++;
      selectorCrit.$$group.push(specimen);
    }

    return {criteria: result};
  }

  _getSelectorCriteria(allocRule, specimen) {
    return {
      cpId            : specimen.cpId,
      specimen        : util.clone(specimen),
      minFreePositions: 0,
      ruleName        :   (allocRule && allocRule.name) || undefined,
      ruleParams      : (allocRule && allocRule.params) || undefined,
      '$$group'       : []
    };
  }

  _assignReservedPositions(op, positions) {
    if (!positions || positions.length <= 0) {
      return;
    }

    let idx = 0;
    op.criteria.forEach(
      selectorCriteria =>
        selectorCriteria.$$group.forEach(
          specimen => specimen.storageLocation = positions[idx++]
        )
    );
  }
}

export default new Specimen();
