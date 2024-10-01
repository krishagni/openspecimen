
angular.module('os.common')
  .config(function($stateProvider) {
    $stateProvider.state('file-download', {
      url: '/file-download?downloadUrl',
      controller: 'FileDownloadCtrl',
      parent: 'signed-in'
    })
  })
  .controller('FileDownloadCtrl',
    function($stateParams, VueApp) {
      VueApp.setVueView('download-file', {downloadUrl: $stateParams.downloadUrl});
    }
  );
