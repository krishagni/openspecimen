angular.module('os.biospecimen.models.specimenlabelprinter', ['os.common.models'])
  .factory('SpecimenLabelPrinter', function(osModel, $http, $q, Util, SettingUtil, Alerts, AuthorizationService) {
    var SpecimenLabelPrinter = osModel('specimen-label-printer');
 
    SpecimenLabelPrinter.getTokens = function() {
      return $http.get(SpecimenLabelPrinter.url() + 'tokens').then(
        function(resp) {
          return resp.data;
        }
      );
    }

    function downloadLabelsPrintFile(job, outputFilename) {
      var url = SpecimenLabelPrinter.url() + 'output-file?jobId=' + job.id;
      if (outputFilename) {
        outputFilename = outputFilename.replace(/\/|\\/g, '_');
        url += '&filename=' + outputFilename;
      }

      Util.downloadFile(url);
      Alerts.info("specimens.labels_print_download");
    }

    SpecimenLabelPrinter.printLabels = function(detail, outputFilename) {
      $http.post(SpecimenLabelPrinter.url(), detail).then(
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
                  Alerts.success("specimens.labels_print_job_created", {jobId: job.id});
                }
              }
            );
          }
        }
      );
    }

    return SpecimenLabelPrinter;
  });
