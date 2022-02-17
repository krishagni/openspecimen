
angular.module('os.biospecimen.cp')
  .controller('CpFormSettingsCtrl', function($scope, $timeout, cp, forms, formsWf, CpConfigSvc) {

    var allForms, workflow;
    function init() {
      workflow = angular.copy(formsWf || {name: 'forms', data: {}});
     
      //
      // {entityType: [sorted forms]}
      //
      var formsMap = getFormsMap(forms, workflow);
      var entityTypes = ['CommonParticipant', 'Participant', 'SpecimenCollectionGroup', 'Specimen'];
      allForms = entityTypes.map(function(type, idx) { return opts(type, formsMap[type], idx); });
      
      $scope.fctx = {
        forms: allForms
      }
    }

    function getFormsMap(forms, workflow) {
      var formsMap = {};
      angular.forEach(forms,
        function(form) {
          if (!formsMap[form.entityType]) {
            formsMap[form.entityType] = [];
          }
          
          formsMap[form.entityType].push(form);
        }
      );

      angular.forEach(formsMap,
        function(forms, type) {
          formsMap[type] = sort(forms, workflow.data[type]);     
        }
      );

      return formsMap;
    }

    function opts(type, forms, idx) {
      return {
        type: type,
        forms: forms,
        pristine: [].concat(forms),
        display: true,
        modified: false,
        sortOpts: sortOpts(idx)
      }
    }

    function sort(forms, order) {
      if (!order || order.length == 0) {
        return forms;
      }

      var formsMap = {};
      angular.forEach(forms,
        function(form, idx) {
          formsMap[form.formId] = form;
        }
      );

      var result = [];
      angular.forEach(order,
        function(o) {
          var form = formsMap[o.id];
          if (form) {
            result.push(form);
            forms.splice(forms.indexOf(form), 1);
          }
        }
      );

      Array.prototype.push.apply(result, forms);
      return result;
    }

    function sortOpts(idx) {
      return {
        placeholder: 'list-group-item',
        stop: function(event, ui) {
          allForms[idx].display = false;
          $timeout(function() { allForms[idx].display = allForms[idx].modified = true; });
        }
      }
    }

    $scope.save = function(savedForms) {
      workflow.data[savedForms.type] = savedForms.forms.map(function(f) { return {id: f.formId, name: f.name}; });
      CpConfigSvc.saveWorkflow(cp.id, workflow).then(
        function() {
          savedForms.pristine = [].concat(savedForms.forms);
          savedForms.modified = false;
          cp.draftMode = true;
        }
      );
    }

    $scope.cancel = function(entityForms) {
      entityForms.forms = [].concat(entityForms.pristine);
      entityForms.modified = false;
    }

    init();
  });
