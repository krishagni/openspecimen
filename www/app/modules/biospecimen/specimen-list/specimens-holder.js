
angular.module('os.biospecimen.specimenlist.specimensholder', [])
  .factory('SpecimensHolder', function(Specimen) {
     this.specimens = undefined;
     this.extra = undefined;

     return {
       getSpecimens: function() {
         if (this.specimens) {
           return this.specimens;
         }

         var json = localStorage.getItem('os.specimens');
         if (json) {
           return JSON.parse(json).map(function(specimen) { return new Specimen(specimen); });
         }

         return [];
       },

       getExtra: function() {
         return this.extra;
       },

       setSpecimens: function(specimens, extra) {
         this.specimens = specimens;
         this.extra = extra;
         if (!this.specimens) {
           localStorage.removeItem('os.specimens');
         } else {
           var json = JSON.stringify(specimens.map(function(spmn) { return {id: spmn.id}; }));
           localStorage.setItem('os.specimens', json);
         }
       },

       clear: function() {
         this.specimens = null;
         this.extra = null;
         localStorage.removeItem('os.specimens');
       }
     }     
  });
