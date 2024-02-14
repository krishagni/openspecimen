
angular.module('os.biospecimen.participant')
  .factory('ParticipantsHolder', function() {
     this.participants = undefined;

     return {
       getParticipants: function() {
         return this.participants;
       },

       setParticipants: function(participants) {
         this.participants = participants;
         if (!this.participants) {
           localStorage.removeItem('os.participants');
         } else {
           var json = JSON.stringify(participants.map(function(p) { return {id: p.id}; }));
           localStorage.setItem('os.participants', json);
         }
       }
     }     
  });
