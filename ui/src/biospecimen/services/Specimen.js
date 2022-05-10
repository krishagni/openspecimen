
import alertsSvc   from '@/common/services/Alerts.js';
import http        from '@/common/services/HttpClient.js';
import settingsSvc from '@/common/services/Setting.js';
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

  printLabels(input, outputFilename) {
    const printQ   = http.post('specimen-label-printer', input);
    const settingQ = settingsSvc.getSetting('administrative', 'download_labels_print_file');
    return Promise.all([printQ, settingQ]).then(
      (resps) => {
        const job = resps[0];
        if (util.isTrue(resps[1][0].value)) {
          let url = http.getUrl('specimen-label-printer/output-file', {query: {jobId: job.id}});
          if (outputFilename) {
            outputFilename = outputFilename.replace(/\/|\\/g, '_');
            url += '&filename=' + outputFilename;
          }

          http.downloadFile(url);
          alertsSvc.info('Downloading labels print CSV file...');
        } else {
          alertsSvc.success('Specimen labels print job ' + job.id + ' created successfully');
        }

        return job;
      }
    );
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
