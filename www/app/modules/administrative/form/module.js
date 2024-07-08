
angular.module('os.administrative.form', 
  [
    'os.administrative.form.list',
    'os.administrative.form.addedit',
    'os.administrative.form.formctxts',
    'os.administrative.form.entities'
  ])

  .config(function($stateProvider) {
    $stateProvider
      .state('form-root', {
        abstract: true,
        template: '<div ui-view></div>',
        resolve: {
          manageForms: function(currentUser, Util) {
            return Util.booleanPromise(currentUser.admin || currentUser.manageForms);
          }
        },
        parent: 'signed-in'
      })
      .state('form-list', {
        url: '/forms?filters',
        template: '<div></div>',
        controller: function($stateParams, VueApp) {
          VueApp.setVueView('forms/-1', $stateParams);
        },
        parent: 'signed-in'
      })
      .state('form-addedit', {
        url: '/form-addedit/:formId',
        templateUrl: 'modules/administrative/form/addedit.html',
        controller: 'FormAddEditCtrl',
        resolve: {
          form: function($stateParams, Form) {
            if ($stateParams.formId) {
              return new Form({id: $stateParams.formId});
            }

            return new Form();
          }
        },
        parent: 'form-root'
      })
      .state('form-import', {
        url: '/import',
        templateUrl: 'modules/administrative/form/import.html',
        controller: 'FormImportCtrl',
        parent: 'form-root'
      })
      .state('form-preview', {
        url: '/form-preview/:formId?revId',
        templateUrl: 'modules/administrative/form/preview.html',
        controller: 'FormPreviewCtrl',
        resolve: {
          formDef: function($stateParams, Form) {
            if ($stateParams.formId > 0 && $stateParams.revId > 0) {
              return Form.getAtRevision($stateParams.formId, $stateParams.revId);
            }

            return Form.getDefinition($stateParams.formId);
          }
        },
        parent: 'form-root'
      });
  });

