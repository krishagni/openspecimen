angular.module('os.biospecimen.common.specimendesc', [])
  .directive('osSpecimenDesc', function($translate, CpConfigSvc) {
    return {
      restrict: 'E',
 
      replace: 'true',

      scope: {
        cp: '=?',
        specimen: '=',
        showReqLabel: '=?',
        showAliquotType: '=?'
      },

      link: function(scope, element, attrs) {
        scope.notSpecified = $translate.instant('pvs.not_specified');
        scope.detailed = attrs.detailed === 'true';

        var cpId = (scope.cp && scope.cp.id) || -1;
        scope.tmpl = CpConfigSvc.getValue(cpId, 'common', 'spmnDescTmpl');
      },

      template:
        '<span class="os-specimen-desc" ng-switch on="!tmpl.value">' +
          '<span ng-switch-when="true">' +
            '<span ng-if="specimen.lineage == \'New\' || detailed">' +
              '<span ng-if="!!specimen.pathology && specimen.pathology != notSpecified">' +
                '{{specimen.pathology}} ' +
              '</span>' +
              '<span>{{specimen.type}} </span>' +
              '<span ng-if="specimen.specimenClass == \'Tissue\' && ' +
                '!!specimen.anatomicSite && specimen.anatomicSite != notSpecified">' +
                '<span translate="specimens.from">from</span> {{specimen.anatomicSite}}' +
              '</span>' +
              '<span ng-if="specimen.specimenClass == \'Fluid\' && ' +
                '!!specimen.collectionContainer && specimen.collectionContainer != notSpecified">' +
                '<span translate="specimens.collected_in">collected in</span> {{specimen.collectionContainer}}' +
              '</span>' +
            '</span>' +
            '<span ng-if="specimen.lineage == \'Aliquot\' && !detailed">' +
              '<span translate="specimens.aliquot">Aliquot</span>' +
              '<span ng-show="showAliquotType"> {{specimen.type}} </span>' +
            '</span>' +
            '<span ng-if="specimen.lineage == \'Derived\' && !detailed">' +
              '<span translate="specimens.derived">Derived</span> {{specimen.type}}' +
            '</span>' +
            '<span ng-if="!!showReqLabel">' +
              '<span ng-switch on="(!!specimen.name || !!specimen.reqLabel) && specimen.lineage != \'Aliquot\'">' +
                '<span ng-switch-when="true"> ({{specimen.name || specimen.reqLabel}}) </span>' +
                '<span ng-switch-default>' +
                  '<span ng-if="!!specimen.code || !!specimen.reqCode"> ({{specimen.code || specimen.reqCode}}) </span>' +
                '</span>' +
              '</span>' +
            '</span>' +
          '</span>' +
          '<span ng-switch-when="false">' +
            '<os-html-template template="tmpl.value"></os-html-template>' +
          '</span>' +
        '</span>'
    };
  });
