angular.module('openspecimen')
  .filter('osArrayJoin', function() {
    return function(collection, fun) {
      if (collection === null || collection === undefined) {
        return '';
      }

      if (!(collection instanceof Array)) {
        return collection;
      }

      if (!fun) {
         return collection.join(", ");
      }

      var result = [];
      angular.forEach(collection, function(item) {
        var value = fun(item);
        if (value) {
          result.push(value);
        }
      });

      return result.join(", ");
    }
  });


