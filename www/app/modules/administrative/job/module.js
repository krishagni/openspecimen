angular.module('os.administrative.job',
  [ 
    'ui.router',
    'os.administrative.job.list',
    'os.administrative.job.runlog',
    'os.administrative.job.addedit'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('job-list', {
        url: '/jobs',
        template: '<div></div>',
        controller: function($stateParams, VueApp) {
          VueApp.setVueView('scheduled-jobs', $stateParams);
        },
        parent: 'signed-in'
      })
  })

  .run(function(UrlResolver) {
    UrlResolver.regUrlState('job-run-log', 'job-run-log', 'jobId');
  });
