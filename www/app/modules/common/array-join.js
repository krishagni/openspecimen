angular.module('openspecimen')
  .filter('osArrayJoin', function() {
    return function(collection, fun) {
      if (collection === null || collection === undefined) {
        return '';
      }

      if (!(collection instanceof Array)) {
        return collection;
      }

      var transformer = undefined;
      if (typeof fun == 'function') {
        transformer = fun;
      } else if (typeof fun == 'string') {
        transformer = function(obj) {
          var parts = fun.split('.');
          for (var i = 0; i < parts.length; ++i) {
            if (!obj || typeof obj != 'object') {
              break;
            }

            obj = obj[parts[i]];
          }

          return obj;
        }
      }

      if (!transformer) {
         return collection.join(", ");
      }

      var result = [];
      angular.forEach(collection, function(item) {
        var value = transformer(item);
        if (value) {
          result.push(value);
        }
      });

      return result.join(", ");
    }
  });


