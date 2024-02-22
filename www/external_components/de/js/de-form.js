var edu = edu || {};
edu.common = edu.common || {};
edu.common.de = edu.common.de || {};

edu.common.de.id = 0;
edu.common.de.nextUid = function(el) {
  return ++edu.common.de.id;
}

edu.common.de.RequiredValidator = function(field, dataEl) {
  this.validate = function() {
    var valid = false;
    var el;

    if (dataEl instanceof Array) { // GroupField
      for (var i = 0; i < dataEl.length; ++i) {
        if (dataEl[i].prop('checked')) {
          valid = true;
          break;
        }
      }
      el = field.clusterEl;
    } else {
      if (dataEl.prop('type') == 'checkbox') {
        valid = dataEl.prop('checked');
      } else {
        valid = field.getValue() &&
                (field.getValue().value || field.getValue().value == 0) &&
                field.getValue().value.length != 0;
      }
      el = field.inputEl;
    }

    if (!valid) {
      edu.common.de.Utility.highlightError(el, 'Please fill this field');
    } else {
      edu.common.de.Utility.unHighlightError(el);
    }

    return valid;
  };
};

edu.common.de.RangeValidator = function(field, dataEl, params) {
  this.validate = function() {
    var val = dataEl.val();
    if (!$.isNumeric(val)) {
      return true; // range validator not applicable for non-numeric fields
    }

    var number = Number(val);
    edu.common.de.Utility.unHighlightError(field.inputEl);
    if ($.isNumeric(params.min) && number < Number(params.min)) {
      edu.common.de.Utility.highlightError(
        field.inputEl,
        "This field value is smaller than the permitted min. value: " + params.min
      );
      return false;
    } else if ($.isNumeric(params.max) && number > Number(params.max)) {
      edu.common.de.Utility.highlightError(
        field.inputEl,
        "This field value is larger than the permitted max. value: " + params.max
      );
      return false;
    } else {
      return true;
    }
  };
};

edu.common.de.NumericValidator = function(field, dataEl, params) {
  this.validate = function() {
    var val = dataEl.val();
    if (!val || val.trim().length == 0) { // empty field
      return true; 
    }

    if (!$.isNumeric(val)) {
      edu.common.de.Utility.highlightError(field.inputEl, "This field value is not valid");
      return false;
    }

    var numberParts = val.split(".");
    var noOfDigits = params.noOfDigitsAfterDecimal;
    if (numberParts.length > 1 && noOfDigits != null && noOfDigits != undefined) {
      var realPart = numberParts[1].trim();
      if (realPart.length > noOfDigits) {
        edu.common.de.Utility.highlightError(
          field.inputEl, 
          field.getCaption() + " cannot have more than " + noOfDigits + " digits after decimal point");
        return false;
      }
    } 

    edu.common.de.Utility.unHighlightError(field.inputEl);
    return true;
  };
};

edu.common.de.FieldValidator = function(rules, field, dataEl) {
  if (!dataEl) {
    dataEl = field.inputEl;
  }

  var validators = [];
  if (!rules) {
    rules = [];
  }

  for (var i = 0; i < rules.length; ++i) {
    var validator;
    if (rules[i].name == 'required') {
      validator = new edu.common.de.RequiredValidator(field, dataEl, rules[i].params);
    } else if (rules[i].name == 'range') {
      validator = new edu.common.de.RangeValidator(field, dataEl, rules[i].params);
    } else if (rules[i].name == 'numeric') {
      validator = new edu.common.de.NumericValidator(field, dataEl, rules[i].params);
    }

    if (validator) {
      validators.push(validator);
    }
  }

  this.validate = function() {
    var valid = true;
    for (var i = 0; i < validators.length; ++i) {
      if (!validators[i].validate()) {
        valid = false;
        break;
      }
    }

    return valid;
  }
 
  if (dataEl instanceof Array) {
    for (var i = 0; i < dataEl.length; ++i) {
      dataEl[i].change(this.validate);
    }
    field.clusterEl.focusout(this.validate);
  } else {
    dataEl.change(this.validate);
    if (field.selectField) {
      dataEl.on('select2-blur', this.validate);
    } else {
      dataEl.focusout(this.validate);
    }
  }
};

edu.common.de.SkipLogic = function(form, fieldObj, fieldAttrs) {
  var rule = undefined;
  var dtFnRe = /^\s*dt\(\"(.*)\"\)\s*$/;
  var logicalOpRe = /\s+(and|or)\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)/;
  var relOpRe = /\s+(<|<=|=|!=|>=|>)\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)/;

  function parseRule(logic) {
    if (!logic || logic.trim().length == 0) {
      return;
    }

    var tokens = logic.trim().split(logicalOpRe);
    if ((tokens.length - 1) % 2 != 0) {
      alert("Invalid skip logic: " + logic);
      return;
    }

    var op = undefined;
    for (var i = 1; i < tokens.length; i += 2) {
      if (op == undefined) {
        op = tokens[i].trim();
      } else if (tokens[i].trim() != op) {
        alert("Invalid skip logic: " + logic + " Either all ANDs or ORs allowed");
        return;
      }
    }

    return constructRule(tokens);
  }

  function constructRule(tokens) {
    var op, lexpr, rexpr;

    lexpr = constructExpr(tokens[0].trim());
    if (tokens.length == 1) {
      op = 'identity';
    } else {
      op = tokens[1].trim();
      rexpr = constructRule(tokens.splice(2));
    }

    return {op: op, lexpr: lexpr, rexpr: rexpr};
  }

  function constructExpr(expr) {
    var tokens = expr.split(relOpRe);
    if (tokens.length != 3) {
      alert("Invalid expression: " + expr);
      return undefined;
    }

    var value = tokens[2].trim();
    if (value.charAt(0) == '"' && value.charAt(value.length - 1) == '"') {
      value = value.substring(1, value.length - 1);
    }

    return {op: tokens[1].trim(), field: tokens[0].trim(), value: value}
  }

  function evaluateRule(rule) {
    var lexprResult = evaluateExpr(rule.lexpr);
    if (rule.op == 'identity') {
      return lexprResult;
    }

    if (rule.op == 'or' && lexprResult == true) {
      return true;
    } else if (rule.op == 'and' && lexprResult == false) {
      return false;
    }

    return evaluateRule(rule.rexpr);
  }

  function nullifyTime(dt) {
    dt.setHours(0); dt.setMinutes(0); dt.setSeconds(0); dt.setMilliseconds(0);
    return dt.getTime();
  }

  function evaluateExpr(expr) {
    var fieldValue = form.skipLogicFieldValue(expr.field);
    if (fieldValue === null || fieldValue === undefined) {
      return false;
    }

    var condValue = expr.value;

    var match = condValue.match(dtFnRe);
    if (match != null) {
      condValue = match[1];
      condValue = nullifyTime($.fn.datepicker.DPGlobal.parseDate(condValue, form.dateFmt()));

      if (fieldValue !== undefined && fieldValue !== null) {
        if (fieldValue instanceof Array) {
          fieldValue = fieldValue.map(
            function(v) {
              return nullifyTime(new Date(+v));
            }
          );
        } else {
          fieldValue = nullifyTime(new Date(+fieldValue));
        }
      }
    }

    return edu.common.de.DefFieldSkipLogicCmp(expr.op, fieldValue, condValue);
  }

  this.parseRule = parseRule;

  this.evaluateRule = evaluateRule;

  this.evaluate = function() {
    if (!rule) {
      return;
    }

    if (evaluateRule(rule)) {
      fieldObj.$el.show();

      var existing = fieldObj.getValue(fieldObj.recId);
      existing = existing && existing.value;
      if (existing != null && existing != undefined && existing != '') {
        return;
      }

      var defValue = fieldObj.defaultValue;
      if (defValue == undefined || defValue == null || defValue == '') {
        defValue = fieldObj.$attrs.defaultValue;
        if (defValue == undefined || defValue == null) {
          return;
        }
      }

      if (typeof defValue == 'object' && defValue.value != undefined && defValue.value != null) {
        defValue = defValue.value;
      }

      fieldObj.setValue(fieldObj.recId, fieldObj.isMultiSelect ? [defValue] : defValue);
      fieldObj.postRender();
    } else {
      fieldObj.$el.hide();
      fieldObj.setValue(fieldObj.recId, null); // clear field value
    }
  }

  rule = parseRule(fieldAttrs.showWhen);
};

edu.common.de.Form = function(args) {
  if (typeof args.formDiv == "string") {
    this.formDiv = $("#" + args.formDiv);
  } else {
    this.formDiv = args.formDiv;
  }

  this.fieldObjs = [];
 
  this.formId = args.id;
  this.formDef = args.formDef;
  this.formDefUrl = args.formDefUrl;
  this.formDefXhr = null;

  this.recordId = args.recordId;
  this.formData = args.formData;
  this.formDataUrl = args.formDataUrl;
  this.formSaveUrl = args.formSaveUrl;
  this.formDataXhr = null;

  this.fileUploadUrl = args.fileUploadUrl;
  this.fileDownloadUrl = args.fileDownloadUrl;
  this.appData = args.appData;
  this.dateFormat = args.dateFormat;

  this.customHdrs = args.customHdrs || {};

  if (!this.formDef && this.formDefUrl) {
    var url = this.formDefUrl.replace(":formId", this.formId);
    this.formDefXhr = $.ajax({type: 'GET', url: url, headers: this.customHdrs});
  }

  if (this.recordId && !this.formData && this.formDataUrl) {
    var url = this.formDataUrl.replace(":formId", args.id)
                              .replace(":recordId", args.recordId);
    this.formDataXhr = $.ajax({type: 'GET', url: url, headers: this.customHdrs});
  }

  this.render = function() {
    var that = this;
    if (this.formDefXhr && this.formDataXhr) {
      $.when(this.formDefXhr, this.formDataXhr).then(function(formDef, formData) {
        that.formDef = formDef[0];
        that.formData = formData[0];
        that.render0();
      });
    } else if (this.formDefXhr) {
      this.formDefXhr.then(function(formDef) {
        that.formDef = formDef;
        that.render0();
      });
    } else if (this.formDataXhr) {
      this.formDataXhr.then(function(formData) {
        that.formData = formData;
        that.render0();
      });
    } else {
      this.render0();
    }
  };

  this.processFormDef = function(prefix, rows) {
    for (var i = 0; i < rows.length; ++i) {
      var row = rows[i];
      for (var j = 0; j < row.length; ++j) {
        if (row[j].type == 'subForm') {
          this.processFormDef(prefix + row[j].name + ".", row[j].rows);
        } else {
          row[j].fqn = prefix + row[j].name;
        }
      }
    }
  };
        
  this.destroy = function() {
    this.formDiv.children().remove();
  };

  this.render0 = function() {
    if (this.formDef.rows == undefined) {
      this.formDef = JSON.parse(this.formDef);
    }

    var rows = this.formDef.rows;
    this.processFormDef("", rows);

    var pages = [];
    if (this.formDef.layouts && this.formDef.layouts.length > 0) {
      pages = getPages(this.formDef);
    } else {
      pages.push({prev: null, next: null, rows: rows, fieldObjs: []});
    }

    this.pages = pages;
    var formCtrls = $("<div/>");
    for (var pi = 0; pi < pages.length; ++pi) {
      var page = pages[pi];
      var pageEl = page.el = $('<div/>').css({display: 'none'});

      for (var ri = 0; ri < page.rows.length; ++ri) {
        pageEl.append(this.rowCtrls(page, ri));
      }

      formCtrls.append(pageEl);
    }

    this.currentPage = pages[0];
    this.currentPage.el.css({display: 'block'});

    if (args.showActionBtns === undefined || 
        args.showActionBtns === null || 
        args.showActionBtns === true) {
      formCtrls.append(this.getActionButtons());
    }

    var caption = args.showTitle == false ? undefined : this.formDef.caption;
    if (args.showPanel == false) {
      this.formDiv.append(formCtrls);
    } else {
      var panel = edu.common.de.Utility.panel(caption, formCtrls, 'default', args);
      this.formDiv.append(panel);
      this.updatePb();
    }

    this.setValue(this.formData);

    if (this.$hasSkipLogic) {
      var that = this;
      var evalSl = function() { that.evaluateSkipLogic(); };
      this.formDiv.find("input").change(evalSl);
      this.formDiv.find("textarea").change(evalSl);
      this.formDiv.find("select").change(evalSl);

      if (this.$delayedInits && this.$delayedInits.length > 0) {
        $.when.apply($, this.$delayedInits).then(
          function() {
            evalSl();
          }
        );
      } else {
        evalSl();
      }
    }
  };

  function getPages(formDef) {
    var fieldsMap = {};
    for (var i = 0; i < formDef.rows.length; ++i) {
      for (var j = 0; j < formDef.rows[i].length; ++j) {
        fieldsMap[formDef.rows[i][j].name] = formDef.rows[i][j];
      }
    }

    var pages = [];

    var prev = null, next = null;
    var layout = formDef.layouts[0];
    for (var pi = 0; pi < layout.pages.length; ++pi) {
      var page = { prev: prev, next: null, rows: [], fieldObjs: [] };
      if (prev) {
        prev.next = page;
      }

      for (var ri = 0; ri < layout.pages[pi].rows.length; ++ri) {
        var pageRow = [];
        for (var ci = 0; ci < layout.pages[pi].rows[ri].length; ++ci) {
          pageRow.push(fieldsMap[layout.pages[pi].rows[ri][ci].field]);
        }

        page.rows.push(pageRow);
      }

      prev = page;
      pages.push(page);
    }

    return pages;
  }

  this.evaluateSkipLogic = function() {
    for (var i = 0; i < this.fieldObjs.length; ++i) {
      if (this.fieldObjs[i].$skipLogic) {
        this.fieldObjs[i].$skipLogic.evaluate();
      }
    }
  }

  this.setValue = function(formData) {
    formData = formData || {};

    var recId = undefined;
    if (formData) {
      recId = formData.id;
    }

    for (var i = 0; i < this.fieldObjs.length; ++i) {
      var fieldObj = this.fieldObjs[i];
      fieldObj.setValue(recId, formData[fieldObj.getName()]);
      fieldObj.postRender();
    }
  };

  this.getValue = function() {
    var formData = {};

    for (var i = 0; i < this.fieldObjs.length; ++i) {
      var field = this.fieldObjs[i];
      var value = field.getValue();
      if (!value) { // note doesn't have value;
        continue;
      }

      formData[value.name] = value.value;
    }

    return formData;
  };

  this.skipLogicFieldValue = function(fieldUdn) {
    var extnPrefix = "$extendedObj.";
    if (fieldUdn.indexOf(extnPrefix) == 0) {
      if (typeof args.skipLogicFieldValue == 'function') {
        return args.skipLogicFieldValue(fieldUdn.substring(extnPrefix.length));
      }

      return undefined;
    }

    var fields = this.fieldObjs;
    for (var i = 0; i < fields.length; ++i) {
      if (fields[i].$attrs.udn == fieldUdn) {
        return fields[i].skipLogicFieldValue();
      }
    }

    return undefined;
  };

  this.dateFmt = function() {
    return args.dateFormat;
  }

  this.rowCtrls = function(page, rowNum) {
    var fields = page.rows[rowNum];
    var noFields = fields.length;
    var width = noFields == 1 ? 8 : Math.floor(12 / noFields);
    var colWidthCls = "col-xs-" + width;

    var rowDiv = edu.common.de.Utility.row();
    for (var i = 0; i < fields.length; ++i) {
      var field = fields[i];
      var widthCls = (field.type == 'subForm') ? "col-xs-12" : colWidthCls;
      rowDiv.append($("<div/>").addClass(widthCls).append(this.getFieldEl(page, field)));
    }

    return rowDiv;
  };

  this.getFieldEl = function(page, field) {
    var id      = 'de-' + field.name

    var labelEl = undefined;
    if (field.type != 'label' && field.type != 'heading') {
      labelEl = this.fieldLabel(id, field.caption, field);
    }

    var fieldObj = edu.common.de.FieldFactory.getField(field, undefined, args);
    fieldObj.$form = this;
    fieldObj.$attrs = field;

    var inputEl = fieldObj.render();
    this.fieldObjs.push(fieldObj);
    page.fieldObjs.push(fieldObj);

    var fieldEl = fieldObj.$el = $("<div/>").addClass("form-group");
    if (labelEl) {
      fieldEl.append(labelEl);
    }

    if (field.showWhen) {
      fieldObj.$skipLogic = new edu.common.de.SkipLogic(this, fieldObj, field);
      this.$hasSkipLogic = true;
    }

    disableField(field, inputEl);
    return fieldEl.append(inputEl);
  };

  function disableField(field, fieldEl) {
    if (!args.disableFields || args.disableFields.indexOf(field.name) == -1) {
      return;
    }

    var el = fieldEl;
    if (field.type == 'subForm') {
      el = fieldEl.find('fieldset');
    } else if (el[0].tagName != 'INPUT' && el[0].tagName != 'TEXTAREA') {
      el = fieldEl.find('input');
    }

    el.attr('disabled', true);
  };

  this.fieldLabel = function(name, label, field) {
    var labelEl = $("<span/>");
    if (args.allowHtmlCaptions != false && args.allowHtmlCaptions != 'false') {
      labelEl.append(label);
    } else {
      labelEl.text(label);
    }

    labelEl = $("<label/>").addClass("control-label").prop('for', name).append(labelEl);
    if (field.toolTip) {
      labelEl.attr('help', field.toolTip);
      labelEl.append('<span class="fa fa-question-circle" style="display: inline-block; margin-left: 5px;"/>');
    }

    return labelEl;
  };

  this.getActionButtons = function() {
    var btns = $("<div/>").addClass("modal-footer");

    var that = this;
    var cancel = $("<button/>").addClass("btn btn-default cancel").append("Cancel");
    cancel.on("click", function() { that.cancel(); });
    btns.append(cancel);

    if (this.pages.length > 1) {
      var previous = $('<button/>').addClass('btn btn-secondary previous').append('Previous');
      previous.on('click', function() { that.previous(); });
      btns.append(previous);
    }

    if (args.showSaveNext == true || args.showSaveNext == 'true') {
      var saveNext = $("<button/>").addClass("btn btn-primary save-next").append("Save and Next");
      saveNext.on("click", function() { that.save(true); });
      btns.append(saveNext);
    }

    if (args.showSaveDraft == true || args.showSaveDraft == 'true') {
      var saveDraft = $("<button/>").addClass("btn btn-primary save-draft").append("Save as Draft");
      saveDraft.on("click", function() { that.save(false, true); });
      btns.append(saveDraft);
    }

    var save = $("<button/>").addClass("btn btn-primary save").append("Save");
    save.on("click", function() { that.save(); });
    btns.append(save);

    if (this.pages.length > 1) {
      var next = $('<button/>').addClass('btn btn-primary next').append('Next');
      next.on('click', function() { that.next(); });
      btns.append(next);
      btns.addClass('first-page');
    } else {
      btns.addClass('last-page');
    }

    this.btns = btns;
    return edu.common.de.Utility.row().append(btns);
  };

  this.updatePb = function() {
    if (this.pages.length <= 1) {
      return;
    }

    var pb = this.formDiv.find('.pb');
    if (!pb || pb.length == 0) {
      var pt = this.formDiv.find('.panel-title');
      if (!pt || pt.length == 0) {
        return;
      }

      pb = $('<div/>').addClass('pb');
      pt.append(pb);
    }

    var currentIdx = this.pages.indexOf(this.currentPage) + 1;
    pb.text(currentIdx + ' / ' + this.pages.length);
  }

  this.next = function() {
    if (!this.validateCurrentPage()) {
      return;
    }

    if (!this.currentPage.prev) {
      this.btns.removeClass('first-page');
    }

    if (this.currentPage.next) {
      this.currentPage.el.css({display: 'none'});
      this.currentPage = this.currentPage.next;
      this.currentPage.el.css({display: 'block'});
    }

    if (!this.currentPage.next) {
      this.btns.addClass('last-page');
    }

    this.updatePb();
  }

  this.previous = function() {
    if (!this.currentPage.next) {
      this.btns.removeClass('last-page');
    }

    if (this.currentPage.prev) {
      this.currentPage.el.css({display: 'none'});
      this.currentPage = this.currentPage.prev;
      this.currentPage.el.css({display: 'block'});
    }

    if (!this.currentPage.prev) {
      this.btns.addClass('first-page');
    }

    this.updatePb();
  }

  this.save = function(next, draftMode) {
    if (!draftMode && !this.validate()) {
      if (args.onValidationError) {
        args.onValidationError();
      }
      return;
    }

    var appData = this.appData || {};
    appData.formStatus = (draftMode && 'DRAFT' || 'COMPLETE');
    var formData = {appData: appData};
    var url = this.formSaveUrl.replace(":formId", this.formId);
    var method;
    if (this.recordId) {
      url = url.replace(":recordId", this.recordId);
      formData.recordId = this.recordId;
      method = 'PUT';
    } else {
      url = url.replace(":recordId", "");
      method = 'POST';
    }

    $.extend(formData, this.getValue());
    var that = this;
    $.ajax({
      type: method,
      url: url,
      headers: this.customHdrs,
      contentType: 'application/json',
      dataType: 'json',
      data: JSON.stringify(formData)
    }).done(function(data) { 
      that.formDiv.removeClass('de-form-submitting');
      that.recordId = data.id;
      if (args.onSaveSuccess) {
        args.onSaveSuccess(data, next);
      }
    }).fail(function(data) { 
      that.formDiv.removeClass('de-form-submitting');
      if (args.onSaveError) {
        args.onSaveError(data);
      }
    });

    that.formDiv.addClass('de-form-submitting');
  };

  this.cancel = function() {
    if (args.onCancel) {
      args.onCancel();
    }
  };

  this.print = function() {
    if (args.onPrint) {
      args.onPrint(this.getFormPrintHtml());
    }
  };

  this.getFormPrintHtml = function() {
    return $("<div/>").append(
      $("<table/>")
        .addClass("table table-condensed table-bordered")
        .css("width", "100%")
        .append(this.getPrintEl()));
  };

  this.deleteForm = function() {
    if (args.onDelete) {
      args.onDelete();
    }
  };
  
  this.validateCurrentPage = function() {
    if (!this.validate(this.currentPage)) {
      if (args.onValidationError) {
        args.onValidationError();
      }

      return false;
    }

    return true;
  }

  this.validate = function(page) {
    var fieldObjs = (page && page.fieldObjs) || this.fieldObjs;

    var valid = true;
    for (var i = 0; i < fieldObjs.length; ++i) {
      if (!fieldObjs[i].validate()) { // validate all fields
        valid = false;
      }
    }

    return valid;
  };

  this.getPrintEl = function() {
    var els = [];
    for (var i = 0; i < this.fieldObjs.length; ++i) {
      if (this.fieldObjs[i] instanceof edu.common.de.Note) {
        continue;
      }

      var tr = $("<tr/>");

      var captionTd = $("<td/>")
        .addClass("de-print-label")
        .append($("<label/>").append(this.fieldObjs[i].getCaption()));   
      tr.append(captionTd);

      var valueTd = $("<td/>")
        .addClass("de-print-value")
        .append(this.fieldObjs[i].getPrintEl ? this.fieldObjs[i].getPrintEl() : "Undefined Print Fn");
      tr.append(valueTd);

      els.push(tr);
    }

    return $("<tbody/>").append(els);
   };
};

edu.common.de.FieldFactory = {

  getField: function(field, idx, args) {
    var fieldObj;
    idx = idx || "";

    var id = 'de-' + field.name + "-" + edu.common.de.nextUid();
    if (field.type == 'stringTextField') {
      fieldObj = new edu.common.de.TextField(id, field, args);
    } else if (field.type == 'numberField') {
      fieldObj = new edu.common.de.NumberField(id, field, args);
    } else if (field.type == 'textArea') {
      fieldObj = new edu.common.de.TextArea(id, field, args);
    } else if (field.type == 'datePicker') {
      fieldObj = new edu.common.de.DatePicker(id, field, args);
    } else if (field.type == 'booleanCheckbox') {
      fieldObj = new edu.common.de.BooleanCheckbox(id, field, args);
    } else if (field.type == 'combobox' || field.type == 'listbox' || field.type == 'multiSelectListbox') {
      fieldObj = new edu.common.de.SelectField(id, field, args);
    } else if (field.type == 'radiobutton' || field.type == 'checkbox') {
      fieldObj = new edu.common.de.GroupField(id, field, args);
    } else if (field.type == 'subForm' && !field.singleEntry) {
      fieldObj = new edu.common.de.SubFormField(id, field, args);
    } else if (field.type == 'subForm' && field.singleEntry) {
      fieldObj = new edu.common.de.ComponentForm(id, field, args);
    } else if (field.type == 'fileUpload') {
      fieldObj = new edu.common.de.FileUploadField(id, field, args);
    } else if (field.type == 'label' || field.type == 'heading') {
      fieldObj = new edu.common.de.Note(id, field, args);
    } else if (field.type == 'signature') {
      fieldObj = new edu.common.de.Signature(id, field, args);
    } else {
      var params = {id: id, field: field, args: args};
      fieldObj = edu.common.de.FieldManager.getInstance().getField(field.type, params);
      if (!fieldObj) {
        fieldObj = new edu.common.de.UnknownField(id, field, args);
      }
    }

    return fieldObj;
  }
};

edu.common.de.DefValueComparator = function(op, leftOperand, rightOperand) {
  switch(op) {
    case '<':
      return leftOperand < rightOperand;
    case '<=':
      return leftOperand <= rightOperand;
    case '>':
      return leftOperand > rightOperand;
    case '>=':
      return leftOperand >= rightOperand;
    case '=':
      return leftOperand == rightOperand;
    case '!=':
      return leftOperand != rightOperand;
    case 'exists':
      return leftOperand !== undefined && leftOperand !== null;
    case 'not_exists':
      return leftOperand === undefined || leftOperand === null;
  }
}

edu.common.de.DefFieldSkipLogicCmp = function(op, fieldValue, condValue) {
  if (!(fieldValue instanceof Array)) {
    fieldValue = [fieldValue];
  }

  for (var i = 0; i < fieldValue.length; ++i) {
    if (edu.common.de.DefValueComparator(op, fieldValue[i], condValue)) {
      return true;
    }
  }

  return false;
}

edu.common.de.DefSkipLogicFieldValue = function() {
  return this.getValue().value;
}

edu.common.de.TextField = function(id, field) {
  this.inputEl = null;

  this.validator = null;

  this.render = function() {
    this.inputEl = $("<input/>")
      .prop({id: id, type: 'text', title: field.toolTip})
      .addClass("form-control");

    this.validator = new edu.common.de.FieldValidator(field.validationRules, this);
    return this.inputEl;
  };

  this.postRender = function() {
    if (!this.recId && this.inputEl.val().trim().length == 0 && field.defaultValue) {
      this.inputEl.val(field.defaultValue);
    }
  };

  this.getName = function() {
    return field.name;
  };

  this.getCaption = function() {
    return field.caption;
  };

  this.getTooltip = function() {
    return field.toolTip ? field.toolTip : field.caption;
  };
	  
  this.getValue = function() {
    return {name: field.name, value: this.inputEl.val()};
  };

  this.setValue = function(recId, value) {
    this.recId = recId;
    this.inputEl.val(value);
  };

  this.getDisplayValue = function() {
    return {name: field.name, value: this.inputEl.val()};
  };

  this.skipLogicFieldValue = edu.common.de.DefSkipLogicFieldValue;
  
  this.validate = function() {
    return this.validator.validate();
  };

  this.getPrintEl = function() {
    return edu.common.de.Utility.getPrintEl(this);
  };
};

edu.common.de.NumberField = function(id, field) {
  this.inputEl = null;

  this.validator;

  this.render = function() {
    this.inputEl = $("<input/>") 
      .prop({id: id, type: 'text', title: field.toolTip, value: field.defaultValue})
      .addClass("form-control");

    var rules = field.validationRules.concat({name: 'numeric', params: {noOfDigitsAfterDecimal: field.noOfDigitsAfterDecimal}});
    this.validator = new edu.common.de.FieldValidator(rules, this);
    return this.inputEl;
  };

  this.postRender = function() {
    if (!this.recId && this.inputEl.val().trim().length == 0 && field.defaultValue) {
      this.inputEl.val(field.defaultValue);
    }
  };

  this.getName = function() {
    return field.name;
  };

  this.getCaption = function() {
    return field.caption;
  };

  this.getTooltip = function() {
    return field.toolTip ? field.toolTip : field.caption;
  };
	  
  this.getValue = function() {
    return {name: field.name, value: this.inputEl.val()};
  };

  this.setValue = function(recId, value) {
    this.recId = recId;
    this.inputEl.val(value);
  };

  this.getDisplayValue = function() {
    return {name: field.name, value: this.inputEl.val()};
  };

  this.skipLogicFieldValue = edu.common.de.DefSkipLogicFieldValue;

  this.validate = function() {
    return this.validator.validate();
  };

  this.getPrintEl = function() {
    return edu.common.de.Utility.getPrintEl(this);
  };
};

edu.common.de.TextArea = function(id, field) {
  this.inputEl = null;

  this.validator;

  this.render = function() {
    var noOfRows = 2;
    if (field.noOfRows && field.noOfRows > 0) {
      noOfRows = field.noOfRows;
    }

    this.inputEl = $("<textarea/>")
      .prop({id: id, rows: noOfRows, title: field.toolTip, value: field.defaultValue})
      .addClass("form-control");
    this.validator = new edu.common.de.FieldValidator(field.validationRules, this);
    return this.inputEl;
  };

  this.postRender = function() {
    if (!this.recId && this.inputEl.val().trim().length == 0 && field.defaultValue) {
      this.inputEl.val(field.defaultValue);
    }
  };

  this.getName = function() {
    return field.name;
  };

  this.getCaption = function() {
    return field.caption;
  };
  
  this.getTooltip = function() {
    return field.toolTip ? field.toolTip : field.caption;
  };
	  
  this.getValue = function() {
    return {name: field.name, value: this.inputEl.val()};
  };

  this.getDisplayValue = function() {
    return {name: field.name, value: this.inputEl.val()};
  }

  this.setValue = function(recId, value) {
    this.recId = recId;
    this.inputEl.val(value);
  };

  this.skipLogicFieldValue = edu.common.de.DefSkipLogicFieldValue;

  this.validate = function() {
    return this.validator.validate();
  };

  this.getPrintEl = function() {
    return edu.common.de.Utility.getPrintEl(this);
  };
};

edu.common.de.DatePicker = function(id, field, args) {
  this.inputEl = null;
  this.dateEl = null;
  this.timeEl = null;
  this.validator;

  this.minWidth = 225;

  this.render = function() {
    this.dateEl = $("<input/>")
      .prop({id: id, type: 'text', title: field.toolTip})
      .addClass("form-control");

    var dateField = $("<div/>").addClass("de-addon de-addon-input-right de-date-picker")
      .append(this.dateEl)
      .append($("<span/>").addClass("glyphicon glyphicon-calendar"));

    this.inputEl = $("<div/>"); 
    this.inputEl.append(dateField);

    var format = field.format;
    if (format && format.indexOf('HH:mm') != -1) {
      dateFmt = args.dateFormat.concat(" HH:mm");
      this.timeEl = $("<input/>")
        .prop({id: 'time', type: 'text', title: field.toolTip})
        .addClass("form-control");

      dateField.css("width","59%");

      var timeField = $("<div/>").addClass("de-addon de-addon-input-right de-time-picker")
        .append(this.timeEl)
        .append($("<span/>").addClass("glyphicon glyphicon-time"));

      this.inputEl.append(timeField);
    }

    this.inputEl.prop({title: field.toolTip});
    this.validator = new edu.common.de.FieldValidator(field.validationRules, this, this.dateEl);

    if (format && format.length != 0) {
      format = format.toUpperCase();
      if (format.indexOf("D") != -1 && format.indexOf("M") != -1 && format.indexOf("Y") != -1) {
        format = 0;
      } else if (format.indexOf("M") != -1 && format.indexOf("Y") != -1) {
        format = 1;
      } else if (format.indexOf("Y") != -1) {
        format = 2;
      } else {
        format = 0;
      }
    } else {
      format = 0; // minViewMode = 0 days
    }

    this.dateEl.datepicker({
      format: typeof args.dateFormat == "undefined" ? format : args.dateFormat,
      autoclose: true,
      minViewMode: format});

    if (this.timeEl) {
      this.timeEl.timepicker({
        showMeridian: false,
        minuteStep: 1,
        defaultTime: (field.defaultType == 'CURRENT_DATE') ? 'current' : false
      });
    }

    return this.inputEl;
  };

  this.postRender = function() {
    if (field.defaultType == 'CURRENT_DATE' && this.dateEl.val().trim().length == 0) {
      this.dateEl.datepicker('setDate', new Date()).datepicker('fill')
    }
  };

  this.getName = function() {
    return field.name;
  };

  this.getCaption = function() {
    return field.caption;
  };
	  
  this.getTooltip = function() {
    return field.toolTip ? field.toolTip : field.caption;
  };
	  
  this.getValue = function() {
    if (this.dateEl.val().trim().length == 0) {
      //
      // Date input field is left blank
      //
      return {name: field.name, value: null};
    }

    var val = this.dateEl.datepicker('getDate');
    if (isNaN(val.getTime())) {
      //
      // This scenario should never occur. If it occurs,
      // take safe path of showing invalid date and exit
      //
      alert("Invalid date");
      return {name: field.name, value: null};
    }

    if (this.timeEl) {
      var time = this.timeEl.val().trim();
      var parts = time.split(":");
      if (parts.length == 2) {
        val.setHours(parseInt(parts[0].trim()));
        val.setMinutes(parseInt(parts[1].trim()));
      }
    }

    return {name: field.name, value: '' + val.getTime()};
  };

  this.setValue = function(recId, value) {
    this.recId = recId;
    if (value === null || value === undefined || (typeof value == 'string' && value.trim().length == 0)) {
      this.dateEl.val(null);
      return;
    }

    try {
      var dt = new Date(parseInt(value));
      this.dateEl.datepicker('setDate', dt);
      if (this.timeEl) {
        this.timeEl.timepicker('setTime', dt);
      }
    } catch (e) {
      var format = field.format;
      if (format.indexOf('HH:mm') != -1) { // TODO: Fix this
        var dateTime = value.split(" ");
        this.dateEl.val(dateTime[0]);
        this.timeEl.val(dateTime[1]);
      } else {
        this.dateEl.val(value);
      }
    }

    this.dateEl.datepicker('update');
  };

  this.getDisplayValue = function() {
    return this.getValue();
  }

  this.skipLogicFieldValue = function() {
    var value = this.getValue().value;
    if (!value) {
      return value;
    }

    var dt = new Date(+value);
    dt.setHours(0); dt.setMinutes(0); dt.setSeconds(0); dt.setMilliseconds(0);
    return dt;
  }

  this.validate = function() {
    return this.validator.validate();
  };

  this.getPrintEl = function() {
    return edu.common.de.Utility.getPrintEl(this);
  };
};

edu.common.de.BooleanCheckbox = function(id, field) {
  this.inputEl = null;
  this.dataEl = null;
  this.validator;

  this.render = function() {
    this.dataEl = $("<input/>")
      .prop({type: 'checkbox', id: id, value: 'true', title: field.toolTip});
    this.inputEl = $("<div/>")
      .append(this.dataEl).css("padding", "6px 0px");

    this.validator = new edu.common.de.FieldValidator(field.validationRules, this, this.dataEl);
    this.setValue(undefined, field.defaultChecked)
    return this.inputEl;
  };

  this.postRender = function() {
    if (!this.recId && this.dataEl.val() != 'true' && field.defaultChecked == true) {
      this.dataEl.val('true');
      this.dataEl.prop('checked', true);
    }
  };

  this.getName = function() {
    return field.name;
  };

  this.getCaption = function() {
    return field.caption;
  };
	  
  this.getTooltip = function() {
    return field.toolTip ? field.toolTip : field.caption;
  };
	  
  this.getValue = function() {
    var value = this.dataEl.prop('checked') ? "1" : "0";
    return {name: field.name, value: value};
  };

  this.setValue = function(recId, value) {
    this.recId = recId; 
    if (value == "1" || value == true) {
      this.dataEl.val("true");
      this.dataEl.prop('checked', true);
    } else {
      this.dataEl.val("false");
      this.dataEl.prop('checked', false);
    }
  };

  this.getDisplayValue = function() {
    var value = this.dataEl.prop('checked') ? "true" : "false";
    return {name: field.name, value: value};
  };
  
  this.skipLogicFieldValue = edu.common.de.DefSkipLogicFieldValue

  this.validate = function() {
    return this.validator.validate();
  };

  this.getPrintEl = function() {
    return edu.common.de.Utility.getPrintEl(this);
  };
};

edu.common.de.SelectFieldOptions = function(field, args) {
  var baseUrl;

  var defaultList = [];

  var maxResults = 100;

  var reload = true;

  function init() {
    if (args.formPvUrl) {
      baseUrl = args.formPvUrl.replace(":formId", args.id) + "/";
    }

    reload = args.formPvUrl && field.pvs.length >= maxResults;

    $.each(field.pvs, function(key, option) {
      defaultList.push({id: option.value, text: option.value});
    })
  }

  this.getOptions = function(id, queryTerm, callback) {
    if (!queryTerm && defaultList.length > 0) {
      callback(defaultList);
      return;
    }

    if (!reload) {
      callback(defaultList.filter(
        function(option) {
          return option.text.toLowerCase().includes(queryTerm.toLowerCase());
        }
      ));
      return;
    }

    var xhr;
    if (queryTerm) {
      var  params = {controlName: id, searchString: queryTerm};
      xhr = $.ajax({type: 'GET', url: baseUrl, data: params, headers: args.customHdrs});
    } else if (this.getAllOptionsXhr) {
      xhr = this.getAllOptionsXhr;
    } else {
      xhr = this.getAllOptionsXhr = $.ajax({type: 'GET', url: baseUrl, data: {controlName: id}, headers: args.customHdrs});
    }
   
    xhr.done(
      function(options) {
        var result = [];
        for (var i = 0; i < options.length; ++i) {
          result.push({id: options[i].value, text: options[i].value});
        }

        if (!queryTerm) {
          reload = result.length >= maxResults;
          defaultList = result;
        }

        callback(result);
      }
    ).fail(
      function(data) {
        alert("Failed to load option list");
      }
    );
  };

  init();
};

//
// This looks very similar to LookupField now.
// TODO: Need to merge both implementations to avoid code duplication
//
edu.common.de.SelectField = function(id, field, args) {
  this.selectField = true;

  this.inputEl = null;

  this.control = null;
  
  this.isMultiSelect = (field.type == 'listbox' || field.type == 'multiSelectListbox');

  this.defaultValue = field.defaultValue ? field.defaultValue.value : '';

  this.value = this.defaultValue;
  if (this.isMultiSelect) {
    this.value = this.defaultValue ? [this.defaultValue] : [];
  }

  this.validator;

  var optionsSvc = new edu.common.de.SelectFieldOptions(field, args);

  var timeout = undefined;

  var that = this;

  var qFunc = function(qTerm, qCallback) {
    var timeInterval = 500;
    if (qTerm.length == 0) {
      timeInterval = 0;
    }

    if (timeout != undefined) {
      clearTimeout(timeout);
    }

    timeout = setTimeout(
      function() { 
        optionsSvc.getOptions(field.fqn, qTerm, qCallback);
      }, 
      timeInterval);
  };

  var onChange = function(selected) { 
    if (selected instanceof Array) {
      that.value = selected.map(function(opt) {return opt.id});
    } else if (selected) {
      that.value = selected.id;
    }

    that.validator.validate();
  };

  var initSelection =  function(elem, callback) {
    if (that.value instanceof Array) {
      callback(that.value.map(
        function(opt) {
          return {id: opt, text: opt};
        }
      ));
    } else {
      callback({id: that.value, text: that.value});
    }
  }

  this.render = function() {
    this.inputEl = $("<input/>")
      .prop({id: id, title: field.toolTip, value: field.defaultValue})
      .css("border", "0px").css("padding", "0px")
      .val("")
      .addClass("form-control");
    this.validator = new edu.common.de.FieldValidator(field.validationRules, this);
    return this.inputEl;
  };

  this.postRender = function() {
    var control = this.control = new Select2Search(this.inputEl, {multiple: this.isMultiSelect});
    this.control.onQuery(qFunc).onChange(onChange);
    this.control.onInitSelection(initSelection).render();

    if (field.toolTip) {
      var select2Container = this.control.element.parent().find('.select2-container');
      if (select2Container) {
        select2Container.prop({title: field.toolTip});
      }
    }

    if (!this.isMultiSelect && (field.mandatory != true && field.mandatory != 'true')) {
      var clear = $('<a class="clear-option glyphicon glyphicon-remove"></a>');
      clear.insertAfter(this.inputEl);
      var that = this;
      clear.click(function() {
        that.setValue(that.recId, null);
        control.setValue(null);
      });
    }

    var hasValue = false;
    if (this.isMultiSelect) {
      if (this.value instanceof Array && this.value.length > 0) {
        this.setValue(this.recId, this.value);
        hasValue = true;
      }
    } else if (this.value) {
      this.setValue(this.recId, this.value);
      hasValue = true;
    }

    var defValue = field.defaultValue && field.defaultValue.value;
    if (!hasValue && !this.recId && defValue) {
      this.setValue(this.recId, this.isMultiSelect ? [defValue] : defValue);
    }
  };

  this.getName = function() {
    return field.name;
  };
  
  this.getCaption = function() {
    return field.caption;
  };
  
  this.getTooltip = function() {
    return field.toolTip ? field.toolTip : field.caption;
  };
	  
  this.getValue = function() {
    return {name: field.name, value: this.value};
  };

  this.getDisplayValue = function() {
    if (!this.control) {
      this.postRender();
    }

    var value = (this.value instanceof Array) ? this.value.join() : this.value;
    return {name: field.name, value: value};
  }

  this.setValue = function(recId, value) {
    this.recId = recId;
    this.value = value ? value : (this.isMultiSelect ? [] : "");
    if (this.control) {
      this.control.setValue(value);
    }
  };

  this.skipLogicFieldValue = edu.common.de.DefSkipLogicFieldValue

  this.validate = function() {
    return this.validator.validate();
  };

  this.getPrintEl = function() {
    return edu.common.de.Utility.getPrintEl(this);
  };
};

edu.common.de.GroupField = function(id, field) {
  this.clusterEl = [];
  this.inputEls = [];

  this.render = function() {
    var currentDiv;
    var count = 0;
    var optionsPerRow = field.optionsPerRow || 3;
    var width = "" + Math.floor(95 / optionsPerRow) - 1 + "%";
    var type = field.type == 'radiobutton' ? 'radio' : 'checkbox';
    var typeclass = field.type == 'radiobutton' ? 'radio-inline' : 'checkbox-inline';

    for (var i = 0; i < field.pvs.length; ++i) {
      var pv = field.pvs[i];
      var defaultVal = false;
      if (count % optionsPerRow == 0) {
        if (currentDiv) {
          this.clusterEl.push(currentDiv);
        }

        currentDiv = $("<div/>");
      }
      
      if (field.defaultValue != undefined &&  field.defaultValue.value == pv.value) {
        defaultVal = true;
      }

      var btn = $("<input/>").prop({
        type: type,
        name: field.name + id,
        value: pv.value,
        title: field.toolTip,
        checked: defaultVal});

      var option = $("<span/>").text(pv.value);
      currentDiv.append($("<label/>").addClass(typeclass).append(btn).append(option).css("width", width));
      this.inputEls.push(btn);
      ++count;
    }

    this.clusterEl.push(currentDiv);
    this.clusterEl = $("<div/>").addClass("form-inline").append(this.clusterEl);
    this.validator = new edu.common.de.FieldValidator(field.validationRules, this, this.inputEls);
    return this.clusterEl;
  };

  this.postRender = function() {
    if (!this.recId && field.defaultValue && field.defaultValue.value) {
      var value = this.getValue().value;
      if (value instanceof Array) {
        if (value.length == 0) {
          this.setValue(this.recId, [field.defaultValue.value]);
        }
      } else if (!value || value.trim().length == 0) {
        this.setValue(this.recId, field.defaultValue.value);
      }
    }
  };

  this.getName = function() {
    return field.name;
  };

  this.getCaption = function() {
    return field.caption;
  };
	  
  this.getTooltip = function() {
    return field.toolTip ? field.toolTip : field.caption;
  };
	  
  this.getValue = function() {
    var checked = [];
    for (var i = 0; i < this.inputEls.length; ++i) {
      if (this.inputEls[i].prop('checked')) {
        checked.push(this.inputEls[i].val());
      }
    }

    var value = checked;
    if (field.type == 'radiobutton') {
      value = checked.length > 0 ? checked[0] : null;
    }

    return {name: field.name, value: value};
  };

  this.setValue = function(recId, value) {
    this.recId = recId;
    value = edu.common.de.Utility.getValueByDataType(field, value);
    var checked;
    if (field.type == 'radiobutton') {
      checked = [value];
    } else {
      checked = value;
    }

    for (var i = 0; i < this.inputEls.length; ++i) {
      if ($.inArray(this.inputEls[i].val(), checked) != -1) {
        this.inputEls[i].prop('checked', true);
      } else {
        this.inputEls[i].prop('checked', false);
      }
    }
  };

  this.getDisplayValue = function() {
    var value = this.getValue().value;
    value = (value instanceof Array) ? value.join().substring(1) : value;
    return {name: field.name, value: value};
  };
  
  this.skipLogicFieldValue = edu.common.de.DefSkipLogicFieldValue

  this.validate = function() {
    return this.validator.validate();
  };

  this.getPrintEl = function() {
    return edu.common.de.Utility.getPrintEl(this);
  };
};

edu.common.de.ComponentForm = function(id, field, args) {
  this.render = function() {
    this.el = $("<div/>");
    this.form = new edu.common.de.Form({
      formDef: field, 
      formDiv: this.el, 
      showActionBtns: false,
      showTitle: false
    });
    this.form.render();
    return this.el;
  }

  this.postRender = function() {
  };

  this.getCaption = function() {
    return field.caption;
  };

  this.getName = function() {
    return field.name;
  };

  this.getValue = function() {
    return {name: field.name, value: [this.form.getValue()]};
  };

  this.setValue = function(recId, value) {
    var formData = {}
    if (value && value instanceof Array) {
      formData = value[0];
    }
    this.form.setValue(formData);
  };

  this.validate = function() {
    return this.form.validate();
  };

  this.getPrintEl = function() {
    return $("<table/>").addClass("table table-condensed").append(this.form.getPrintEl());
  };
};

edu.common.de.SubFormField = function(id, sfField, args) {
  this.subFormEl = null;
  this.sfFieldsEl = null;
  this.rowIdx = 0;
  this.fieldObjsRows = [];
  this.recIds = [];
  this.getFields = function() {
    var fields = [];
    for (var i = 0; i < sfField.rows.length; ++i) {
      for (var j = 0; j < sfField.rows[i].length; ++j) {
        fields.push(sfField.rows[i][j]);
      }
    }

    return fields;
  };

  this.getFieldWidth = function(numFields) {
    var width = Math.floor(95 / numFields) - 1;
    if (width < 20) {
      width = 20;
    }

    return width;
  };

  this.fields = this.getFields();

  this.fieldWidth = this.getFieldWidth(this.getFields().length);

  this.render = function() {
    this.sfFieldsEl = $("<div/>");
  
    var subFormContent = $("<div/>").addClass("de-sf-content");
    subFormContent
      .append(this.getHeading())
      .append(this.sfFieldsEl);


    this.subFormEl = $("<div/>").prop({id: id, title: sfField.toolTip})
      .append($("<fieldset/>").append(subFormContent).append(this.getAddButton()));

    return this.subFormEl;
  };

  this.postRender = function() {
  };  

  this.getName = function() {
    return sfField.name;
  };

  this.getCaption = function() {
    return sfField.caption;
  };

  this.getValue = function() {
    var values = [];
    for (var i in this.fieldObjsRows) {
      var fieldObjs = this.fieldObjsRows[i];
      var sfInstance = {id: this.recIds[i]};
      for (var j = 0; j < fieldObjs.length; ++j) {
        var value = fieldObjs[j].getValue();
        if (!value) { // note doesn't have value
          continue;
        }
        sfInstance[value.name] = value.value;
      }

      values.push(sfInstance);
    }
    return {name: sfField.name, value: values};
  };

  this.setValue = function(recId, value) {
    value = value || [];

    this.recId = recId;
    this.sfFieldsEl.children().remove();
    this.fieldObjsRows = [];
    this.rowIdx = 0;

    for (var i = 0; i < value.length; ++i) {
      var sfInst = value[i];
      this.addSubFormFieldsRow(false, sfInst.id);
      
      var fieldObjs = this.fieldObjsRows[i];
      for (var j = 0; j < fieldObjs.length; ++j) {
        var fieldValue = sfInst[fieldObjs[j].getName()];
        fieldObjs[j].setValue(sfInst.id, fieldValue);
        fieldObjs[j].postRender();
      }
    }
  };

  this.getDisplayValue = function() {
    return sfField.name;
  };

  var getSfFieldMinWidth = function(field) {
    if (field.type == 'datePicker') {
      if (field.format && field.format.indexOf('HH:mm') != -1) {
        //
        // Minimum container width required to display both date and time picker
        //
        return '250px';
      } else {
        //
        // Minimum container width required to display only date picker
        //
        return '150px';
      }
    } else {
      return undefined;
    }
  };

  this.getHeading = function() {
    var heading = $("<div/>").addClass("form-group clearfix").css("white-space", "nowrap");

    var captionElFactory;
    if (args.allowHtmlCaptions != 'false' && args.allowHtmlCaptions != false) {
      captionEl = function(caption) { return $("<span/>").append(caption); };
    } else {
      captionEl = function(caption) { return $("<span/>").text(caption); };
    }

    for (var i = 0; i < this.fields.length; ++i) {
      var field = this.fields[i];
      var column = $("<div/>").css("width", this.fieldWidth + '%')
        .css("min-width", getSfFieldMinWidth(field))
        .addClass("de-sf-cell")
        .append(captionEl(field.caption));
      heading.append(column);
    }

    return heading;
  };

  this.getAddButton = function() {
    var addBtn = $("<a/>").css('text-decoration', 'none').text("Add Another");
    var that = this;
    addBtn.on("click", function() { that.addSubFormFieldsRow(); });
    return $("<div/>").addClass("form-group clearfix").append(addBtn);
  };

  this.getRemoveButton = function(rowDiv, fieldObjs) {
    var removeBtn = edu.common.de.Utility.iconButton({btnClass: 'btn btn-default', icon: 'trash'});
    var that = this;
    removeBtn.on("click", function() {
      rowDiv.remove();

      var idx = that.fieldObjsRows.indexOf(fieldObjs);
      if (idx >= 0) {
        that.fieldObjsRows.splice(idx, 1);
        that.recIds.splice(idx, 1);
        that.rowIdx--;
      }
    });

    return removeBtn;
  };

  this.addSubFormFieldsRow = function(postRender, recId) {
    var fieldObjs = [];

    var rowDiv = $("<div/>").addClass("form-group clearfix").css("white-space", "nowrap");
    for (var i = 0; i < this.fields.length; ++i) {
      var field = this.fields[i];
      if (field.type == 'subForm') {
        continue;
      }

      var fieldObj = edu.common.de.FieldFactory.getField(field, this.rowIdx, args);
      fieldObj.$attrs = field;

      var fieldEl = fieldObj.render();
      rowDiv.append(this.cell(fieldEl, this.fieldWidth, getSfFieldMinWidth(field)));
      fieldObjs.push(fieldObj);
    }

    var removeButton = this.getRemoveButton(rowDiv, fieldObjs);
    rowDiv.append(this.cell(removeButton, "3%"));

    this.sfFieldsEl.append(rowDiv);

    postRender = typeof postRender == 'undefined' || postRender;
    if (postRender) {
      for (var i = 0; i < fieldObjs.length; ++i) {
        fieldObjs[i].postRender();
      }
    }

    this.fieldObjsRows[this.rowIdx] = fieldObjs;
    this.recIds[this.rowIdx] = recId;
    this.rowIdx++;
  };

  this.cell = function(el, width, minWidth) {
    if (!width) {
      width = this.fieldWidth;
    }

    return $("<div/>").css("width", width + '%')
      .css("min-width", minWidth)
      .addClass("de-sf-cell")
      .append(el);
  };
  
  this.validate = function() {
    var valid = true;
    for (var i = 0; i < this.fieldObjsRows.length; ++i) {
      var fieldObjs = this.fieldObjsRows[i];
      for (var j = 0; j < fieldObjs.length; ++j) {
        if (!fieldObjs[j].validate()) {
          valid = false;
        }
      }
    }

    return valid;
  };

  this.getPrintEl = function() {
    if (!this.fieldObjsRows || this.fieldObjsRows.length == 0) {
      return $("<span/>").append("N/A");
    }

    var theadRow = $("<tr/>");
    for (var i = 0; i < this.fieldObjsRows[0].length; ++i) {
      var field = this.fieldObjsRows[0][i];
      if (field instanceof edu.common.de.Note) {
        continue;
      }

      theadRow.append($("<th/>").append(field.getCaption()));
    }

    var thead = $("<thead/>").append(theadRow);

    var tbody = $("<tbody/>");
    for (var i = 0; i < this.fieldObjsRows.length; ++ i) {
      var tr = $("<tr/>");
      for (var j = 0; j < this.fieldObjsRows[i].length; ++j) {
        var field = this.fieldObjsRows[i][j];
        if (field instanceof edu.common.de.Note) {
          continue;
        }
        tr.append($("<td/>").append(edu.common.de.Utility.getPrintEl(field)));
      }
      tbody.append(tr);
    }

    return $("<table/>").addClass("table table-condensed").append(thead).append(tbody);
  };
};

edu.common.de.FileUploadField = function(id, field, args) {
  this.value = {filename: undefined, contentType: undefined, fileId: undefined};

  this.validator;

  this.inputEl = null;       /** The outer div of file input element **/

  this.uploadBtn = null;     /** The file upload button i.e. input[type=file] **/

  this.removeBtn = null;     /** File remove button **/

  this.fileNameSpan = null;  /** Span to contain name of uploaded file **/

  this.progressBar = null;   /** Progress bar div **/

  this.fileNameInput = null;

  this.dirty = false;

  this.render = function() {
    var that = this;
    this.inputEl  = $("<div/>") 
      .prop({title: field.toolTip})
      .addClass("de-fileupload form-control");
    this.fileNameSpan = $("<span/>");
    this.inputEl.append(this.fileNameSpan);

    this.fileNameInput = $("<input/>").prop({type: 'text'}).addClass("hidden");
    this.inputEl.append(this.fileNameInput);

    this.removeBtn = $("<span/>").addClass("glyphicon glyphicon-remove de-input-addon hidden");
    this.removeBtn.on("click", function() {
      var disabled = that.fileNameInput.attr('disabled');
      if (typeof disabled !== typeof undefined && disabled !== false) {
        return;
      }

      that.setValue(this.recId, {filename: undefined, contentType: undefined, fileId: undefined});
    });

    var uploadIcon = $("<span/>").addClass("glyphicon glyphicon-paperclip de-input-addon");
    var uploadUrl
    if (typeof args.fileUploadUrl == "function") {
      uploadUrl = args.fileUploadUrl(id, field, args);
    } else if (typeof args.fileUploadUrl == "string") {
      uploadUrl = args.fileUploadUrl;
    }

    this.uploadBtn = $("<input/>").attr({name: "file", type: "file", 'data-url': uploadUrl});
    uploadIcon.append(this.uploadBtn);

    this.uploadBtn.fileupload({
      dataType: 'json',

      headers: args.customHdrs || {},

      done: function(e, data) {
        var value = {
          filename: data.result.filename, 
          contentType: data.result.contentType,
          fileId: data.result.fileId
        };
        that.dirty = true;
        that.setValue(that.recId, value);
        that.progressBar.addClass("hidden");
        that.fileNameSpan.removeClass("hidden");
      },

      progress: function(e, data) {
        var pct = Math.floor(data.loaded / data.total * 100);
        if (pct != 100) {
          that.fileNameSpan.addClass("hidden");
          that.progressBar.removeClass("hidden");
          that.progressBar.children().css('width', pct + '%');
        }
      },

      error: function(e, data) {
        if (e) {
          alert(e.responseText);
        } else {
          alert("Unknown file upload error");
        }
      }
    });

    this.progressBar  = $("<div/>").addClass("progress pull-left hidden").css('width', '90%');
    this.progressBar.append($("<div/>").addClass("progress-bar progress-bar-default"));

    var btns = $("<div/>").addClass("pull-right").append(uploadIcon).append(this.removeBtn);
    this.validator = new edu.common.de.FieldValidator(field.validationRules, this, this.fileNameInput);
    return this.inputEl.append(this.progressBar).append(btns);
  };

  this.postRender = function() {
  };

  this.getName = function() {
    return field.name
  };

  this.getCaption = function() {
    return field.caption;
  };
	  
  this.getTooltip = function() {
    return field.toolTip ? field.toolTip : field.caption;
  };
	  
  this.getValue = function() {
    return {name: field.name, value: this.value}; 
  };
  
  this.setValue = function(recId, value) {
    this.recId = recId;
    this.value = value;

    if (this.value && this.value.filename) {
      if (this.dirty) {
        this.fileNameSpan.text(this.value.filename);
      } else {
        var url = "#";
        if (typeof args.fileDownloadUrl == "function") {
          url = args.fileDownloadUrl(args.id, this.recId, field.fqn, this.value.fileId);
        } else {
          url = args.fileDownloadUrl;
        }

        var link = $("<a/>").attr({href: url, target: "_blank"}).text(this.value.filename);
        this.fileNameSpan.append(link);
      }

      this.removeBtn.removeClass("hidden");
      this.fileNameInput.val(this.value.filename).change();
    } else {
      this.fileNameSpan.text("");
      this.fileNameSpan.children().remove();
      this.removeBtn.addClass("hidden");

      if (!!this.fileNameInput.val()) {
        this.fileNameInput.val("").change();
      } else {
        this.fileNameInput.val("");
      }
    }

    return this;
  };

  this.getDisplayValue = function() {
    var filename = undefined;
    if (this.value) {
      filename = this.value.filename;
    }
    return {name: field.name, value: filename};
  };

  this.skipLogicFieldValue = function() {
    var value = this.getValue().value;
    if (value) {
      value = value.filename;
    }

    return value;
  }

  this.validate = function() {
    return this.validator.validate();
  };

  this.getPrintEl = function() {
    return edu.common.de.Utility.getPrintEl(this);
  };
};

edu.common.de.Note = function(id, field, args) {
  this.inputEl = null;

  this.render = function() {
    var captionEl = $("<span/>");
    if (args.allowHtmlCaptions != 'false' && args.allowHtmlCaptions != false) {
      captionEl.append(field.caption);
    } else {
      captionEl.text(field.caption);
    }

    this.inputEl = $("<div class='de-note'/>").append(captionEl);
    if (field.heading == true || field.type == 'heading') {
      this.inputEl.addClass('de-heading');
    }
    return this.inputEl;
  };

  this.postRender = function() {
  };

  this.getName = function() {
    return field.name;
  };

  this.getCaption = function() {
    return field.caption;
  };

  this.getTooltip = function() {
    return "";
  };
	  
  this.getValue = function() {
    return undefined;
  };

  this.setValue = function(value) {
  };
  
  this.validate = function() {
    return true;
  };

  this.getDisplayValue = function() {
    return {name: field.name, value: field.caption};
  }
};

edu.common.de.UnknownField = function(id, field) {
  this.inputEl = $("<div/>").append("Field type " + field.type + " unknown");

  this.render = function() {
    return this.inputEl;
  }

  this.postRender = function() {
  };

  this.getName = function() {
    return field.name;
  };

  this.getValue = function(value) {
    return {name: field.name, value: "Unknown"};
  };

  this.setValue = function(recId, value) {
  };
  
  this.validate = function() {
    return false;
  };

  this.getPrintEl = function() {
    return edu.common.de.Utility.getPrintEl(this);
  };
};


edu.common.de.Utility = {
  /**
   * Valid values for context are primary, success, info, warning, danger
   */
  panel: function(title, content, context, opts) {
    if (!context) {
      context = "default";
    }

    var panelDiv = $("<div/>").addClass("panel").addClass("panel-" + context);
    if (title) {
      var textEl = $("<span/>");
      if (opts.allowHtmlCaptions == 'false' || opts.allowHtmlCaptions == false) {
        textEl.text(title);
      } else {
        textEl.append(title);
      }

      var titleDiv = $("<div/>").addClass("panel-title").append(textEl);

      var panelHeading = $("<div/>").addClass("panel-heading");
      panelHeading.append(titleDiv);
      panelDiv.append(panelHeading);
    }

    var panelBody = $("<div/>").addClass("panel-body").append(content);
    panelDiv.append(panelBody);
    return panelDiv.addClass('de-form');
  },

  row: function() {
    return $("<div/>").addClass("row");
  },

  label: function(caption, attrs) {
    return $("<label/>").append(caption).prop(attrs);
  },

  iconButton: function(options) {
    return $("<button/>").addClass(options.btnClass)
             .append($("<span/>").addClass("glyphicon glyphicon-" + options.icon));
  },

  highlightError: function(el, errorMessage) {
    var errEl = el.next(".alert-danger");
    if (!errEl.length) {
      el.after($("<div/>").addClass("alert alert-danger de-form-err-msg").append(errorMessage));
    }
  },

  unHighlightError: function(el) {
    el.next('.alert-danger').remove();
  },

  getValueByDataType: function(field, value) {
    if (value == undefined || field.dataType == "STRING" || field.dataType == "BOOLEAN") {
      return value;
    }
	
    var parsedVal;
    if (value instanceof Array) {
      parsedVal = [];
      for (var i = 0 ; i < value.length ; i++) {
        if (field.dataType == "INTEGER") {
          parsedVal[i] = parseInt(value[i]).toString();
        } else if (field.dataType == "FLOAT") {
          parsedVal[i] = parseFloat(value[i]).toString();
        }
      }
    } else {
      if (field.dataType == "INTEGER") {
        parsedVal = parseInt(value);
      } else if (field.dataType == "FLOAT") {
        parsedVal = parseFloat(value);
      }
      parsedVal = parsedVal.toString();
    }
    return parsedVal;
  },

  getPrintEl: function(fieldObj) {
    var val = fieldObj.getDisplayValue().value;
    if (val == undefined || val == null) {
      val = "N/A";
    }

    if (val instanceof Array) {
      val = val.join(", ");
    }

    return $("<span/>").append(val);
  }
};

edu.common.de.Extend = function(props) {
  var parent = this;
  var child = function() { return parent.apply(this, arguments); };

  $.extend(child, parent);
  if (props) {
    $.extend(child.prototype, props);
  }

  child.__super__ = parent.prototype;
  return child;
};

edu.common.de.LookupField = function(params, callback) {
  this.selectField = true;

  this.inputEl = null;

  this.control = null;

  this.value = '';

  this.validator;

  this.params = params;

  var timeout = undefined;

  var field = params.field;
 
  var id = params.id;

  var that = this;

  var qFunc = function(qTerm, qCallback) {
    var timeInterval = 500;
    if (qTerm.length == 0) {
      timeInterval = 0;
    }

    if (timeout != undefined) {
      clearTimeout(timeout);
    }

    timeout = setTimeout(
      function() { 
        var promise = that.search(qTerm);
        $.when(promise).done(
          function(results) {
            qCallback(results);
          }
        );
      }, 
      timeInterval);
  };

  var onChange = function(selected) {
    if (selected) {
      that.value = selected.id;
    } else {
      that.value = '';
    }

    if (typeof callback == "function") {
      callback(selected);
    }
  };

  var initSelection = function(elem, callback) {
    var initQs = [];
    if (that.$form) {
      initQs = that.$form.$delayedInits = that.$form.$delayedInits || [];
    }

    if (!that.value && field.defaultType != 'none') {
      var dd = that.getDefaultValue();
      initQs.push(dd);

      $.when(dd).done(
        function(result) {
          field.defaultValue = that.value = result.id;
          that.control.setValue(result.id);
          callback(result);
        }
      );
    } else if (!!that.value) {
      var dd = that.lookup(that.value);
      initQs.push(dd);

      $.when(dd).done(
        function(result) {
          callback(result);
        }
      );
    }
  };

  this.render = function() {
    this.inputEl = $("<input/>")
      .prop({id: id, title: field.toolTip, value: field.defaultValue})
      .css("border", "0px").css("padding", "0px")
      .val("")
      .addClass("form-control");
    this.validator = new edu.common.de.FieldValidator(field.validationRules, this);
    return this.inputEl;
  };

  this.postRender = function() {
    var control = this.control = new Select2Search(this.inputEl);
    this.control.onQuery(qFunc).onChange(onChange);
    this.control.onInitSelection(initSelection).render();

    if (field.toolTip) {
      var select2Container = this.control.element.parent().find('.select2-container');
      if (select2Container) {
        select2Container.prop({title: field.toolTip});
      }
    }

    if (!this.isMultiSelect && (field.mandatory != true && field.mandatory != 'true')) {
      var clear = $('<a class="clear-option glyphicon glyphicon-remove"></a>');
      clear.insertAfter(this.inputEl);
      var that = this;
      clear.click(function() {
        that.setValue(that.recId, null);
        control.setValue(null);
      });
    }

    if (this.value) {
      this.control.setValue(this.value);
    }
  };

  this.getName = function() {
    return field.name;
  };

  this.getCaption = function() {
    return field.caption;
  };

  this.getTooltip = function() {
    return field.toolTip ? field.toolTip : field.caption;
  };

  this.getValue = function() {
    var val = this.control.getValue();
    if (val) {
      val = val.id;
    }

    return {name: field.name, value: val};
  };

  this.getDisplayValue = function() {
    if (!this.control) {
      this.postRender();
    }
    var val = this.control.getValue();
    if (val) {
      var displayValue = val.text;
    }
    return {name: field.name, value: displayValue ? displayValue : '' };
  }

  this.setValue = function(recId, value) {
    this.recId = recId;
    this.value = value; // ? value : '';
    if (this.control) {
      this.control.setValue(value);
    }
  };

  this.skipLogicFieldValue = function() {
    if (!this.control) {
      this.postRender();
    }

    var value = this.control.getValue();
    if (!(value instanceof Array)) {
      value = [value];
    }

    return value.map(
      function(val) {
        return !!val ? val.text : undefined;
      }
    );
  }

  this.validate = function() {
    return this.validator.validate();
  };

  this.getPrintEl = function() {
    return edu.common.de.Utility.getPrintEl(this);
  };

  this.getDefaultValue = function() {
    return this.svc.getDefaultEntity(this);
  };

  this.lookup = function(id) {
    return this.svc.getEntity(id);
  };

  this.search = function(qTerm) {
    var searchFilters = {};
    if (params) {
      searchFilters = params.searchFilters || {};
    }
    return this.svc.getEntities(qTerm, searchFilters, field);
  };
};

edu.common.de.LookupField.extend = edu.common.de.Extend;

edu.common.de.LookupSvc = function(params) {
  var entitiesMap = {};
 
  var defaultList = {};

  var xhrMap = {};

  this.defaultCacheKey = function(queryTerm, searchFilters, field) {
    var resultKey = '_default';
    if (!queryTerm) {
      if (searchFilters) {
        var keys = Object.keys(searchFilters).sort();
        if (keys.length > 0) {
          resultKey = '';
        }

        for (var i = 0; i < keys.length; ++i) {
          resultKey += keys[i] + "__" + searchFilters[keys[i]] + '__';
        }
      }
    }

    return resultKey;
  }

  this.getEntities = function(queryTerm, searchFilters, field) {
    var deferred = $.Deferred();

    var resultKey = '';
    if (typeof this.getCacheKey == 'function') {
      resultKey = this.getCacheKey(queryTerm, searchFilters, field);
    } else {
      resultKey = this.defaultCacheKey(queryTerm, searchFilters, field);
    }

    if (!queryTerm && defaultList[resultKey]) {
      deferred.resolve(defaultList[resultKey]);
      return deferred.promise();
    }

    var baseUrl = this.getApiUrl();
    var xhr;
    if (queryTerm) {      
      xhr = $.ajax({
        type: 'GET', 
        url: baseUrl, 
        headers: this.getHeaders(), 
        data: this.searchRequest(queryTerm, searchFilters, field)
      });
    } else if (xhrMap[resultKey]) {
      xhr = xhrMap[resultKey];
    } else {
      xhr = xhrMap[resultKey] = $.ajax({
        type: 'GET', 
        url: baseUrl, 
        headers: this.getHeaders(), 
        data: this.searchRequest(queryTerm, searchFilters, field)});
    }
   
   
    var that = this;
    xhr.done(
      function(data) {
        var result = that.formatResults(data, queryTerm);
        if (!queryTerm) {
          defaultList[resultKey] = result;
        }

        deferred.resolve(result);
        delete xhrMap[resultKey];
      }
    ).fail(
      function(data) {
        alert("Failed to load entities list");
        deferred.resolve([]);
        delete xhrMap[resultKey];
      }
    );

    return deferred.promise();
  };

  this.getEntity = function(id) {
    var deferred = $.Deferred(); 
    var entity = entitiesMap[id];
    if (entity) {
      deferred.resolve(entity);
      return deferred.promise();
    }

    for (var i = 0; i < defaultList.length; ++i) {
      if (defaultList[i].id == id) { 
        deferred.resolve(defaultList[i]);
        return deferred.promise();
      }
    }

    var xhr = entitiesMap[id + '_xhr'];
    if (!xhr) {
      var baseUrl = this.getApiUrl();
      xhr = $.ajax({type: 'GET', url: baseUrl + id, headers: this.getHeaders()})
      entitiesMap[id + '_xhr'] = xhr;
    }

    var that = this;
    xhr
      .done(function(data) {
        var result = that.formatResult(data);
        entitiesMap[id] = result;
        deferred.resolve(result);
      })
      .fail(function(data) {
        alert("Failed to retrieve entity")
        deferred.resolve(undefined);
      });

    return deferred.promise();
  };

  this.getDefaultEntity = function(luField) {
    var deferred = $.Deferred(); 

    var that = this;
    this.getDefaultValue(luField).done(
      function(data) {
        var result = that.formatResult(data);
        entitiesMap[result.id] = result;
        deferred.resolve(result);
      })
      .fail(function(data) {
        alert("Failed to retrieve default value");
        deferred.resolve(undefined);
      });

    return deferred.promise();
  };
};

edu.common.de.LookupSvc.extend = edu.common.de.Extend;

edu.common.de.SignatureWidget = function(canvas) {
  var ctxt = canvas.getContext("2d");

  ctxt.fillStyle = "#fff";
  ctxt.strokeStyle = "#444";
  ctxt.lineWidth = 1.5;
  ctxt.lineCap = "round";
  ctxt.fillRect(0, 0, canvas.width, canvas.height);

  function getPosition(canvas, e) {
    var pos = {x: undefined, y: undefined};
    var bounds = canvas.getBoundingClientRect();

    if (e.changedTouches && e.changedTouches[0]) {
      pos.x = e.changedTouches[0].clientX - bounds.x;
      pos.y = e.changedTouches[0].clientY - bounds.y;
    } else if (e.layerX || 0 == e.layerX) {
      pos.x = e.layerX;
      pos.y = e.layerY;
    } else if (e.offsetX || 0 == e.offsetX) {
      pos.x = e.offsetXtop
      pos.y = e.offsetY;
    }

    pos.x /= bounds.width;
    pos.x *= canvas.width;

    pos.y /= bounds.height;
    pos.y *= canvas.height;

    return pos;
  }

  function onMouseDown(evt) {
    if (evt.target == canvas) {
      evt.preventDefault();
    }

    lastPos = getPosition(canvas, evt);
    ctxt.beginPath();
    ctxt.moveTo(lastPos.x, lastPos.y);

    canvas.addEventListener('mousemove', onMouseMove, false);
    canvas.addEventListener('touchmove', onMouseMove, false);

    canvas.addEventListener('mouseup', onMouseUp, false);
    canvas.addEventListener('touchend', onMouseUp, false);
  }

  function onMouseMove(evt) {
    if (evt.target == canvas) {
      evt.preventDefault();
    }

    if (!lastPos) {
      return;
    }

    var pos = getPosition(canvas, evt);
    var mid = {x: (lastPos.x + pos.x) / 2, y: (lastPos.y + pos.y) / 2};

    ctxt.quadraticCurveTo(lastPos.x, lastPos.y, mid.x, mid.y);
    ctxt.stroke();
    ctxt.beginPath();
    ctxt.moveTo(mid.x, mid.y);
    lastPos = pos;
  }

  function onMouseUp(evt) {
    if (evt.target == canvas) {
      evt.preventDefault();
    }

    ctxt.stroke();
    lastPos = undefined;

    canvas.removeEventListener('mousemove', onMouseMove, false);
    canvas.removeEventListener('touchmove', onMouseMove, false);

    canvas.removeEventListener('mouseup', onMouseUp, false);
    canvas.removeEventListener('touchend', onMouseUp, false);
  }

  canvas.addEventListener('mousedown', onMouseDown, false);
  canvas.addEventListener('touchstart', onMouseDown, false);

  this.clear = function() {
    ctxt.clearRect(0, 0, canvas.width, canvas.height);
    ctxt.fillRect(0, 0, canvas.width, canvas.height);
  }

  this.getData = function() {
    var data = ctxt.getImageData(0, 0, canvas.width, canvas.height).data;
    var allWhite = true;
    for (var i = 0; i < data.length; ++i) {
      if (data[i] < 255) {
        allWhite = false;
        break;
      }
    }

    return allWhite ? null : canvas.toDataURL("image/jpeg");
  }

  this.setData = function(dataUrl) {
    this.clear();

    var img = new Image();
    img.onload = function() {
      ctxt.drawImage(img, 0, 0);
    }

    img.src = dataUrl;
  }
}

edu.common.de.Signature = function(id, field, args) {
  this.canvasEl = null;

  this.widget = null;

  this.inputEl = null;

  this.clearBtn = null;

  this.validator;

  this.value = null;

  this.render = function() {
    this.canvasEl = $('<canvas/>').prop({title: field.toolTip});

    this.imageEl = $('<img/>');

    this.removeBtn = $('<button/>')
      .append($('<span/>').append('Remove'))
      .addClass('reset');

    this.saveBtn = $('<button/>')
      .append($('<span/>').append('Save'))
      .addClass('edit');

    this.clearBtn = $('<button/>')
      .append($('<span/>').append('Clear'))
      .addClass('edit');

    var buttons = $('<div/>').addClass('actions')
      .append(this.removeBtn)
      .append(this.saveBtn)
      .append(this.clearBtn);

    this.inputEl = $('<div/>').addClass('de-signature').append(this.canvasEl).append(this.imageEl).append(buttons);
    this.validator = new edu.common.de.FieldValidator(field.validationRules, this);
    return this.inputEl;
  };

  this.postRender = function() {
    if (!this.recId && !this.value && field.defaultValue) {
      this.value = field.defaultValue;
    }

    this.widget = new edu.common.de.SignatureWidget(this.canvasEl[0]);
    this.setValue(this.recId, this.value);

    var that = this;
    this.clearBtn.on('click', function() {
      that.widget.clear(); 
      that.setValue(that.recId, that.widget.getData());
    });

    this.removeBtn.on('click', function() {
      that.widget.clear();
      that.setValue(that.recId, null);
    });

    this.saveBtn.on('click', function() {
      var dataUrl = that.widget.getData();
      if (dataUrl) {
        $.ajax({
          type: 'POST',
          url: args.fileUploadUrl + '/images',
          headers: args.customHdrs,
          contentType: 'application/json',
          dataType: 'json',
          data: JSON.stringify({dataUrl: dataUrl})
        }).done(function(data) {
          that.setValue(that.recId, data.fileId, dataUrl);
        }).fail(function(data) {
          if (args.onSaveError) {
            args.onSaveError(data);
          }
        });
      } else {
        that.setValue(that.recId, null);
      }
    });
  };

  this.getName = function() {
    return field.name;
  };

  this.getCaption = function() {
    return field.caption;
  };
  
  this.getTooltip = function() {
    return field.toolTip ? field.toolTip : field.caption;
  };
	  
  this.getValue = function() {
    return {name: field.name, value: this.value};
  };

  this.getDisplayValue = function() {
    return {name: field.name, value: this.widget.getData()};
  }

  this.setValue = function(recId, value, dataUrl) {
    this.recId = recId;
    this.value = value;

    if (this.value) {
      var url = "#";
      if (dataUrl) {
        url = dataUrl;
      } else if (typeof args.fileDownloadUrl == "function") {
        url = args.fileDownloadUrl(args.id, this.recId, field.fqn, this.value);
      } else {
        url = args.fileDownloadUrl;
      }

      this.imageEl.attr('src', url);
      this.inputEl.addClass("view-mode");
    } else {
      this.inputEl.removeClass("view-mode");
    }
  };

  this.skipLogicFieldValue = edu.common.de.DefSkipLogicFieldValue;

  this.validate = function() {
    return this.validator.validate();
  };

  this.getPrintEl = function() {
    return edu.common.de.Utility.getPrintEl(this);
  };
};
