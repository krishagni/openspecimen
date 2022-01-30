
angular.module('openspecimen')
  .directive('osFileUpload', function($timeout, $q, $http, $cookies, Alerts) {
    return {
      restrict: 'A',
      replace: true,
      scope: {
        ctrl: '=',

        onUpload: '&'
      },      
      controller: function() {
        this.data = null;
        this.q = null;

        this.isFileSelected = function() {
          return !!this.data
        }

        this.submit = function() {
          this.q = $q.defer();
          if (this.data) {
            this.submitting = true;
            this.data.submit();
          } else {
            Alerts.error('common.no_file_selected');
            this.q.reject();
          }

          return this.q.promise;
        };

        this.done = function(resp) {
          this.submitting = false;
          this.q.resolve(resp.result);
        };

        this.fail = function(resp) {
          this.submitting = false;

          var xhr = resp.xhr('responseText');
          var status = Math.floor(xhr.status / 100);

          var responses = eval(xhr.response);
          var errMsgs = [];
          angular.forEach(responses, function(err) {
            errMsgs.push(err.message + "(" + err.code + ")");
          });

          if (errMsgs.length > 0) {
            Alerts.errorText(errMsgs);
          } else if (status == 4) {
            Alerts.error('common.ui_error');
          } else if (status == 5) {
            Alerts.error('common.server_error');
          }

          this.q.reject(resp);
        }
      },
      link: function(scope, element, attrs, ctrl) {
        attrs.$observe('disabled', function() {
          if (attrs.disabled == true) {
            element.find('input').attr('disabled', 'disabled');
          } else {
            element.find('input').removeAttr('disabled');
          }
        });

        $timeout(function() {
          scope.ctrl = ctrl;
          scope.caption = attrs.caption || 'No File Selected';

          element.find('input').fileupload({
            dataType: 'json',
            beforeSend: function(xhr) {
              if ($http.defaults.headers.common['X-OS-API-TOKEN']) {
                xhr.setRequestHeader('X-OS-API-TOKEN', $http.defaults.headers.common['X-OS-API-TOKEN']);
              }

              if (ui.os.global.impersonate && $cookies.get('osImpersonateUser')) {
                xhr.setRequestHeader('X-OS-IMPERSONATE-USER', $cookies.get('osImpersonateUser'));
              }
            },
            add: function (e, data) {
              element.find('span').text(data.files[0].name);
              ctrl.data = data;
              if (attrs.uploadOnSelect == 'true' || attrs.uploadOnSelect == true) {
                ctrl.submit();
              }
            },
            done: function(e, data) {
              var filename = "Unknown";
              if (data.originalFiles && data.originalFiles.length > 0) {
                filename = data.originalFiles[0].name;
              }

              ctrl.done(data);
              scope.onUpload({filename: filename, result: data.result});
            },
            fail: function(e, data) {
              ctrl.fail(data);
            }
          });

          element.find('input').bind('change', function() { $timeout(angular.noop); });
        });
      },
      template: 
        '<div class="os-file-upload">' +
          '<input class="form-control" name="file" type="file">' +
          '<span class="name">' +
             '{{caption}}' +
          '</span>' +
        '</div>'
    };
  });
