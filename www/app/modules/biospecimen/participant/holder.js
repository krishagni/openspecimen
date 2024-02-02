
angular.module('os.biospecimen.participant')
  .factory('ParticipantsHolder', function(CollectionProtocolRegistration) {
     this.participants = undefined;

     return {
       getParticipants: function() {
         if (this.participants) {
           return this.participants;
         }

         var json = localStorage.getItem('os.participants');
         if (json) {
           return JSON.parse(json).map(function(cpr) { return new CollectionProtocolRegistration(cpr); });
         }

         return [];
       },

       setParticipants: function(participants) {
         this.participants = participants;
         if (!this.participants) {
           localStorage.removeItem('os.participants');
         } else {
           var json = JSON.stringify(participants.map(function(participant) { return {id: participant.id}; }));
           localStorage.setItem('os.participants', json);
         }
       }
     }
  });
