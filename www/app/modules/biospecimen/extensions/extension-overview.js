angular.module("os.biospecimen.extensions")
  .directive("osExtensionOverview", function(ExtensionsUtil, Util) {

     function processAttrs(formId, recordId, prefix, attrs) {
       angular.forEach(attrs,
         function(attr) {
           if (attr.type == 'subForm') {
             prefix = prefix || '';
             var sfPrefix = prefix + attr.name + '.';

             if (attr.value instanceof Array) {
               angular.forEach(attr.value,
                 function(sfAttrs) {
                   processAttrs(formId, recordId, sfPrefix, sfAttrs);
                 }
               );
             }
           } else {
             if (attr.type == 'fileUpload') {
               attr.$$fileDownloadUrl = ExtensionsUtil.getFileDownloadUrl(
                 formId, recordId, prefix + attr.name, attr.value && attr.value.fileId);
             } else if (attr.type == 'signature') {
               attr.$$imageUrl = ExtensionsUtil.getFileDownloadUrl(
                 formId, recordId, prefix + attr.name, attr.value);
             } else if (attr.type == 'textArea' || attr.type == 'stringTextField') {
               attr.displayValue = Util.linkify(attr.value);
             }
           }
         }
       );
     }

     return {
       restrict: 'A',

       templateUrl: 'modules/biospecimen/extensions/extension-overview.html',

       scope: {
         extObject : "=",
         showColumn: "="
       },

       link: function(scope, element, attrs) {
         processAttrs(scope.extObject.formId, scope.extObject.id, '', scope.extObject.attrs);
       }
     }
  });
