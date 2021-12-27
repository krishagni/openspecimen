angular.module('openspecimen')
  .directive('osEditableItemList', function($q, $timeout) {
    return {
      restrict: 'E',

      scope: {
        items: '=',
        allowEdit: '=',

        itemPvs:    '=',

        textAttr: '@',
        listTitle: '@',
        addCaption: '@',

        listChanged: '&',
        refresh: '&'
      },

      link: function(scope, element, attrs) {
        scope.editItemIdx = undefined;
        scope.newItem = {text: ''};
        scope.editItem = {text: ''};

        scope.refreshItems = function(searchTerm) {
          return scope.refresh()(searchTerm);
        },

        scope.showAddItem = function() {
          if (scope.saving) {
            return;
          }

          scope.addMode = true;
          scope.newItem.text = '';
          scope.editItemIdx = undefined;
        };

        scope.addItem = function() {
          if (scope.newItem.text.trim().length == 0) {
            return;
          }

          var item = {};
          if (scope.itemPvs) {
            item.itemKey = scope.newItem.text;
          } else {
            item[scope.textAttr] = scope.newItem.text;
          }

          if (scope.listChanged) {
            scope.saving = true;
            $q.when(scope.listChanged()('add', item)).then(
              function(result) {
                if (result) {
                  scope.items.push(result);
                  scope.newItem.text = '';
                }

                scope.saving = false;

                /** This is trick to enable auto focus on add */
                scope.addMode = false;
                $timeout(function() { scope.addMode = true; }, 0);
              },

              function() {
                scope.saving = false;
              } 
            );
          } else {
            scope.items.push(item);
            scope.newItem.text = '';
          }
        };

        scope.openForEdit = function(idx) {
          if (scope.saving) {
            return;
          }

          var prop = !scope.itemPvs ? scope.textAttr : 'itemKey';
          scope.editItemIdx = idx;

          var text = scope.items[idx][prop];
          scope.editItem = {text: text};
          if (scope.itemPvs) {
            if (!scope.itemPvs.find(function(itemPv) { return itemPv[prop] == text; })) {
              scope.refreshItems(text);
            }
          }

          scope.addMode = false;
        };

        scope.removeItem = function(idx) {
          if (scope.saving) {
            return;
          }

          scope.addMode = false;
          scope.editItemIdx = undefined;

          var item = scope.items[idx];
          if (scope.listChanged) {
            scope.saving = true;
            $q.when(scope.listChanged()('remove', item)).then(
              function(result) {
                scope.items.splice(idx, 1);
                scope.saving = false;
              },

              function(result) {
                scope.saving = false;
              }
            );
          } else {
            scope.items.splice(idx, 1);
          } 
        };

        scope.revertEdit = function() {
          if (scope.saving) {
            return;
          }

          scope.editItemIdx = undefined;
        };

        scope.revertAdd = function() {
          if (scope.saving) {
            return;
          }

          scope.addMode = false;
        };

        scope.updateItem = function() {
          if (scope.editItem.text.trim().length == 0) {
            return;
          }

          var item = angular.copy(scope.items[scope.editItemIdx]);
          item[scope.textAttr] = scope.editItem.text;

          if (scope.listChanged) {
            scope.saving = true;
            $q.when(scope.listChanged()('update', item)).then(
              function(result) {
                if (result) {
                  scope.items[scope.editItemIdx] = result;
                  scope.editItem.text = '';
                }

                scope.editItemIdx = undefined;
                scope.saving = false;
              },

              function(result) {
                scope.saving = false;
              }
            );
          } else {
            scope.items[scope.editItemIdx] = item;
            scope.editItem.text = '';
            scope.editItemIdx = undefined;
          }
        };
      },

      templateUrl: 'modules/common/editable-items-list.html'
    }
  });

