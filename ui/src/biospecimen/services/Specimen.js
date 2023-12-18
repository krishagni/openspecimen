
import alertsSvc   from '@/common/services/Alerts.js';
import cpSvc       from './CollectionProtocol.js';
import exprUtil    from '@/common/services/ExpressionUtil.js';
import http        from '@/common/services/HttpClient.js';
import formSvc     from '@/forms/services/Form.js';
import formUtil    from '@/common/services/FormUtil.js';
import settingsSvc from '@/common/services/Setting.js';
import ui          from '@/global.js';
import util        from '@/common/services/Util.js';

import specimenSchema from '@/biospecimen/schemas/specimens/specimen.js';

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
        return positions.length > 0 ? positions[0].reservationId : undefined;
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

  async getDict(cpId) {
    return cpSvc.getWorkflow(cpId, 'dictionary').then(
      (dict) => {
        dict = dict || {};

        let spmnFields = (dict.fields || []).filter(field => field.name.indexOf('specimen.') == 0);
        if (spmnFields.length > 0) {
          spmnFields = formUtil.sdeFieldsToDict(spmnFields);
        } else {
          spmnFields = util.clone(specimenSchema.fields);
        }

        if (spmnFields.some(field => field.name.indexOf('specimen.extensionDetail') == 0)) {
          return spmnFields;
        }

        return this.getCustomFieldsForm(cpId).then(
          (formDef) => {
            let customFields = formUtil.deFormToDict(formDef, 'specimen.extensionDetail.attrsMap.');
            return spmnFields.concat(customFields);
          }
        );
      }
    );
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
      const {lineage, storageType, specimenClass, type} = specimen;
      if (storageType == 'Virtual') {
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
