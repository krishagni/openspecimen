
import alertsSvc   from '@/common/services/Alerts.js';
import http        from '@/common/services/HttpClient.js';
import settingsSvc from '@/common/services/Setting.js';
import ui          from '@/global.js';
import util        from '@/common/services/Util.js';

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

  bulkUpdate(specimens) {
    return http.put('specimens', specimens);
  }

  bulkDelete(specimenIds, reason) {
    return http.delete('specimens', {}, {id: specimenIds, reason: reason})
  }

  bulkUpdateStatus(specs) {
    return http.put('specimens/status', specs);
  }
}

export default new Specimen();
