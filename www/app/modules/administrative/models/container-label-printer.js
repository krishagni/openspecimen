angular.module('os.administrative.models')
  .factory('ContainerLabelPrinter', function(osModel, $http, $q, Util, SettingUtil, Alerts, AuthorizationService) {
    var ContainerLabelPrinter = osModel('container-label-printer');
 
    ContainerLabelPrinter.getTokens = function() {
      return $http.get(ContainerLabelPrinter.url() + 'tokens').then(
        function(resp) {
          return resp.data;
        }
      );
    }

    function downloadLabelsPrintFile(job, outputFilename) {
      var url = ContainerLabelPrinter.url() + 'output-file?jobId=' + job.id;
      if (outputFilename) {
        outputFilename = outputFilename.replace(/\/|\\/g, '_');
        url += '&filename=' + outputFilename;
      }

      Util.downloadFile(url);
      Alerts.info("container.labels_print_download");
    }

    ContainerLabelPrinter.printLabels = function(detail, outputFilename) {
      $http.post(ContainerLabelPrinter.url(), detail).then(
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
                  Alerts.success("container.labels_print_job_created", {jobId: job.id});
                }
              }
            );
          }
        }
      );
    }

    return ContainerLabelPrinter;
  });
