angular.module('openspecimen')
  .factory('ItemsHolder', function() {
     var itemsMap = {};

     return {
       getItems: function(type) {
         var items = itemsMap[type];
         if (!items && localStorage.getItem('os.' + type)) {
           items = JSON.parse(localStorage.getItem('os.' + type));
         }

         return items || [];
       },

       setItems: function(type, items, storeLocally) {
         if (!items) {
           delete itemsMap[type];
           localStorage.removeItem('os.' + type);
         } else {
           itemsMap[type] = items;
           if (storeLocally) {
             localStorage.setItem('os.' + type, JSON.stringify(items));
           }
         }
       }
     }
  });
