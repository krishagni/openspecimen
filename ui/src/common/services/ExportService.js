
import http   from '@/common/services/HttpClient.js'
import alerts from '@/common/services/Alerts.js'

class ExportService {

  exportRecords(input) {
    alerts.info('Export records has been initiated. Records file download should start in a few moments..', -1, 'ei1');
    return http.post('export-jobs', input).then(
      function(savedJob) {
        alerts.remove('ei1');
        if (savedJob.status == 'COMPLETED') {
          alerts.info('Downloading records file');
          http.downloadFile(http.getUrl('export-jobs/') + savedJob.id + '/output');
        } else if (savedJob.status == 'FAILED') {
          alerts.error('Export job ' + savedJob.id + ' failed with errors. Please contact system administrator for help!');
        } else {
          alerts.info('Export records job ' + savedJob.id + ' is taking longer time to finish. Link to download records file will be sent to you by e-mail');
        }

        return savedJob;
      }
    );
  }

}

export default new ExportService();
