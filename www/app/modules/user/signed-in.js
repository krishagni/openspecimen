angular.module('openspecimen')
  .controller('SignedInCtrl', function(
     $scope, $rootScope, $state, $timeout, $sce, $modal, $filter, currentUser, userUiState, videoSettings,
     AuthService, Alerts, AuthorizationService, SettingUtil, User, VueApp) {

     function init() {
       $scope.alerts = Alerts.messages;
       $rootScope.currentUser = currentUser;
       $rootScope.logout = false;
       ui.os.global.impersonate = !!currentUser.impersonated;

       var ctx = $scope.userCtx = {
         hasPhiAccess: AuthorizationService.hasPhiAccess(),
         state: userUiState,
         showNewStuff: true,
         name: $filter('osUserDisplayName')(currentUser) || '',
         profileFormsUrl: VueApp.getVueViewUrl('users/' + currentUser.id + '/profile-forms/list')
       }

       var revision = ui.os.global.appProps.build_commit_revision;
       if (userUiState && userUiState.notesRead == revision) {
         ctx.showNewStuff = false;
       }

       setSetting('training', 'training_url', 'trainingUrl');
       setSetting('training', 'help_link',    'helpUrl');
       setSetting('training', 'forum_link',   'forumUrl');
       setSetting('auth',     'saml_enable',  'samlEnabled');
       setSetting('auth',     'single_logout','sloEnabled');

       showIntroVideo();
     }

     function setSetting(module, settingName, propName) {
       SettingUtil.getSetting(module, settingName).then(
         function(setting) {
           $scope.userCtx[propName] = setting.value;
         }
       );
     }

     $scope.userCreateUpdateOpts = {resource: 'User', operations: ['Create', 'Update']};
     $scope.cpReadOpts = {resource: 'CollectionProtocol', operations: ['Read']};
     $scope.containerReadOpts = {resource: 'StorageContainer', operations: ['Read']};
     $scope.orderReadOpts = {resource: 'Order', operations: ['Read']};
     $scope.shipmentReadOpts = {resource: 'ShippingAndTracking', operations: ['Read']};
     $scope.scheduledJobReadOpts = {resource: 'ScheduledJob', operations: ['Read']};
     $scope.dpReadOpts = {resource: 'DistributionProtocol', operations: ['Read']};

     function showIntroVideo() {
       if (ui.os.global.impersonate || (ui.os.global.introVideoShown && ui.os.global.introVideoShown[currentUser.id])) {
         return;
       }

       if (userUiState && userUiState.introVideoSeen) {
         return;
       }

       if (videoSettings.welcome_video_source !== 'vimeo' &&
         videoSettings.welcome_video_source !== 'youtube') {
         return;
       }

       $modal.open({
         templateUrl: 'modules/user/welcome-sign-in.html',
         controller: function($scope, $modalInstance) {
           $scope.videoSettings = videoSettings;
           $scope.iframeUrl = $sce.trustAsResourceUrl(videoSettings.welcome_video_url);

           $scope.gotIt = function() {
             $modalInstance.close(true);
           }

           $scope.watchLater = function() {
             $modalInstance.dismiss('dismiss');
           }

           ui.os.global.introVideoShown = ui.os.global.introVideoShown || {};
           ui.os.global.introVideoShown[currentUser.id] = true;
         },
         backdrop: 'static',
         keyboard: false
       }).result.then(
         function() {
           User.saveUiState({introVideoSeen: true}).then(
             function(savedState) {
               angular.extend(loginCtx, {state: savedState});
             }
           );
         }
       );
     }

     $scope.returnToAccount = function() {
       AuthService.impersonate(null).then(
         function() {
           //
           // delayed reloading of state is present to ensure
           // the cookie is removed from the store
           //
           $timeout(function() { $state.go('home', {}, {reload: true}) }, 500);
         }
       );
     }

     init();
  });
