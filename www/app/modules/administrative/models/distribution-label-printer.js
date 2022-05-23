angular.module('os.administrative.models.order')
  .factory('DistributionLabelPrinter', function(osModel, $http, $q, Util, SettingUtil, Alerts, AuthorizationService) {
    var DistributionLabelPrinter = osModel('distribution-label-printer');
 
    DistributionLabelPrinter.getTokens = function() {
      return $http.get(DistributionLabelPrinter.url() + 'tokens').then(
        function(resp) {
          return resp.data;
        }
      );
    }

    function downloadLabelsPrintFile(job, outputFilename) {
      var url = DistributionLabelPrinter.url() + 'output-file?jobId=' + job.id;
      if (outputFilename) {
        outputFilename = outputFilename.replace(/\/|\\/g, '_');
        url += '&filename=' + outputFilename;
      }

      Util.downloadFile(url);
      Alerts.info("orders.labels_print_download");
    }

    DistributionLabelPrinter.printLabels = function(detail, outputFilename) {
      $http.post(DistributionLabelPrinter.url(), detail).then(
        function(resp) {
          var job = resp.data;
          if (AuthorizationService.currentUser().downloadLabelsPrintFile) {
            downloadLabelsPrintFile(job, outputFilename);
          } else {
            SettingUtil.getSetting('administrative', 'download_labels_print_file').then(
              function(setting) {
                if (setting.value == 'true' || setting.value == true) {
                  downloadLabelsPrintFile(job, outputFilename);
                } else {
                  Alerts.success("orders.labels_print_job_created", {jobId: job.id});
                }
              }
            );
          }
        }
      );
    }

    return DistributionLabelPrinter;
  });
