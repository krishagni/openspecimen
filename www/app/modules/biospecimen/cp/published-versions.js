angular.module('os.biospecimen.cp')
  .controller('CpPublishedVersionsCtrl', function($scope, $modal, cp, CollectionProtocol) {
    var lctx = {};

    function init() {
      lctx = $scope.lctx = {
        totalVersions: 0,
        currPage: 1,
        versionsPerPage: 25
      };

      $scope.$watch('lctx.currPage', function() {
        loadVersions();
      });
    }

    function loadVersions() {
      var startAt = (lctx.currPage - 1) * lctx.versionsPerPage;
      var maxResults = lctx.versionsPerPage + 1;
      cp.getPublishedVersions(startAt, maxResults).then(
        function(versions) {
          lctx.totalVersions = (lctx.currPage - 1) * lctx.versionsPerPage + versions.length;
          if (versions.length >= maxResults) {
            versions.splice(versions.length - 1, 1);
          }

          lctx.versions = versions;
          angular.forEach(versions, function(version) {
            version.cpDefUrl = CollectionProtocol.url() + cp.$id() + '/published-versions/' + version.id;
          });
        }
      );
    }

    $scope.showVersionDetails = function(version) {
      $modal.open({
        templateUrl: 'modules/biospecimen/cp/published-version-detail.html',
        controller: function($scope, $modalInstance) {
          $scope.version = version;

          $scope.close = function() {
            $modalInstance.dismiss('cancel');
          }
        }
      });
    }

    init();
  });
