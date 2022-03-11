
angular.module('os.biospecimen.cpgroups')
  .controller('CpGroupFormsCtrl', function($scope, $state, $modal, group, CollectionProtocolGroup, Form, CheckList, Util) {

    function init() {
      $scope.fctx = { };
      loadForms();
    }

    function loadForms() {
      group.getForms().then(
        function(forms) {
          var fctx = {};

          angular.forEach(forms,
            function(levelForms) {
              fctx[levelForms.level] = levelForms.forms;
            }
          );

          $scope.fctx = fctx;
          if (fctx.Participant) {
            fctx.partCheckList = new CheckList(fctx.Participant);
          }

          if (fctx.SpecimenCollectionGroup) {
            fctx.visitCheckList = new CheckList(fctx.SpecimenCollectionGroup);
          }

          if (fctx.Specimen) {
            fctx.specimenCheckList = new CheckList(fctx.Specimen);
          }
        }
      );
    }

    function addForm(level, showMultiRecord) {
      var fctx = $scope.fctx;
      $modal.open({
        templateUrl: 'modules/biospecimen/cp-groups/add-form.html',
        controller: function($scope, $modalInstance) {
          var mctx = $scope.mctx = { forms: [], showMultiRecord: showMultiRecord, allowMultipleRecords: false };

          var existingForms = (fctx[level] || []).map(function(form) { return form.formId; });
          $scope.searchForms = function(searchTerm) {
            Form.query({name: searchTerm, excludeSysForms: true}).then(
              function(forms) {
                $scope.mctx.forms = forms.filter(function(form) { return existingForms.indexOf(form.formId) == -1; });
              }
            );
          }

          $scope.submit = function() {
            var forms = [{formId: mctx.form.formId, multipleRecords: mctx.allowMultipleRecords}];
            group.addForms({level: level, forms: forms}).then(
              function(resp) {
                if (resp.status == true) {
                  loadForms();
                }

                $modalInstance.close('dismiss');
              }
            );
          }

          $scope.cancel = function() {
            $modalInstance.close('dismiss');
          }
        }
      });
    }

    function removeForms(level, forms) {
      Util.showConfirm({
        title: 'cp_groups.remove_forms',
        confirmMsg: 'cp_groups.remove_forms_q',
        input: {forms: forms},
        ok: function() {
          var formIds = forms.map(function(f) { return f.formId; });
          group.removeForms(level, formIds).then(
            function(resp) {
              loadForms();
            }
          );
        }
      });
    }

    $scope.addForm = addForm;

    $scope.removeForms = removeForms;

    init();
  });
