angular.module('openspecimen')
  .directive('osAddrmFavorite', function($http, $modal, $location, $state, Alerts, ApiUrls, VueApp, Util) {
    return {
      restrict: 'E',

      replace: true,

      templateUrl: 'modules/common/addrm-favorite.html',

      controller: function($scope) {
        $scope.favorites = [];

        function loadFavorites() {
          $http.get(ApiUrls.getBaseUrl() + 'user-favorites').then(
            function(resp) {
              setFavorites(resp.data);
            }
          );
        }

        function setFavorites(favorites) {
          $scope.favorites = favorites;
          angular.forEach(favorites,
            function(favorite) {
              if (!favorite.oldView) {
                favorite.url = VueApp.getVueViewUrl(favorite.viewUrl.substring(2));
              } else {
                favorite.url = favorite.viewUrl;
              }
            }
          );

          setHeartOnFireIfCurrentPageFavorite();
        }

        function setHeartOnFireIfCurrentPageFavorite() {
          $scope.currentPageFavorite = false;
  
          var href = '#' + $location.url();
          for (var idx = 0; idx < $scope.favorites.length; ++idx) {
            var favorite = $scope.favorites[idx];
            if (favorite.viewUrl == href && favorite.oldView) {
              $scope.currentPageFavorite = true;
              break;
            }
          }
        }

        $scope.showAddFavoriteForm = function() {
          if ($state.current.name == 'query-results' && !$state.params.queryId) {
            Alerts.error('common.favorite.unsaved_query_favorite_na');
            return;
          }

          return $modal.open({
            templateUrl: 'modules/common/add-favorite.html',

            controller: function($scope, $modalInstance) {
              $scope.favorite = {oldView: true};

              $scope.add = function() {
                $scope.favorite.viewUrl = '#' + $location.url();
                $http.post(ApiUrls.getBaseUrl() + 'user-favorites', $scope.favorite).then(
                  function(resp) {
                    $modalInstance.close(resp.data);
                  }
                );
              }

              $scope.cancel = function() {
                $modalInstance.close('dismiss');
              }
            }
          }).result.then(
            function(resp) {
              if (resp == 'dismiss') {
                return;
              }

              setFavorites(resp);
            }
          );
        }

        $scope.confirmDeleteFavorite = function() {
          Util.showConfirm({
            title: 'delete_entity.title',

            confirmMsg: 'common.favorite.confirm_delete_favorite',

            ok: function() {
              var href = '#' + $location.url();
              for (var idx = 0; idx < $scope.favorites.length; ++idx) {
                var favorite = $scope.favorites[idx];
                if (favorite.viewUrl == href && favorite.oldView) {
                  $http.delete(ApiUrls.getBaseUrl() + 'user-favorites/' + favorite.id).then(
                    function(resp) {
                      setFavorites(resp.data);
                    }
                  );
                  break;
                }
              }
            }
          });
        }

        loadFavorites();
        $scope.$on('$locationChangeSuccess',
          function() {
            setHeartOnFireIfCurrentPageFavorite();
          }
        );
      }
    }
  });
