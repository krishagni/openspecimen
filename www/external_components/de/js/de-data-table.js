edu.common.de.DataTable = function(args) {
  if (typeof args.formDiv == 'string') {
    this.formDiv = $("#" + args.formDiv);
  } else {
    this.formDiv = args.formDiv;
  }
  
  this.formId = args.formId;
  this.formDef = args.formDef;
  this.formDefUrl = args.formDefUrl;
  this.tableData = args.tableData;
  this.appColumns = args.appColumns;
  this.mode = args.mode;
  this.allowRowSelection = args.allowRowSelection;
  this.dateFormat = args.dateFormat;

  this.tableRowsData = [];
  this.fieldObjs = [];
  this.formDefXhr = null;
  
  this.customHdrs = args.customHdrs || {};
  
  if (!this.formDef && this.formDefUrl) {
    var url = this.formDefUrl.replace(":formId", this.formId);
    this.formDefXhr = $.ajax({type: 'GET', url: url, headers: this.customHdrs});
  }

  this.setMode = function(mode) {
    this.mode = mode;
  }

  this.getMode = function() {
    return this.mode;
  };

  this.clear = function() {
    this.formDiv.empty();
  };

  this.render = function() {
    var that = this;
    if (this.formDefXhr) {
      this.formDefXhr.then(function(formDef) {
        that.formDef = formDef;
        if (that.tableData != null && that.tableData != undefined) {
          that.render0();
        }
      });
    } else {
      if (this.tableData != null && this.tableData != undefined) {
        this.render0();
      }
    }
  };

  this.render0 = function() {
    this.formDiv.empty();

    if (this.formDef.rows == undefined) {
      this.formDef = JSON.parse(this.formDef);
    }

    
    // first let's render table body
    this.tableRowsData = [];
    var trs = [];
    for (var i = 0; i < this.tableData.length; i++) {
      var records = this.renderEntityRecords(this.tableData[i]);
      trs = trs.concat(records);
    }
    var tableBody = $("<tbody/>").append(trs);

   
    // Now rendering the header
    var width = 0;
    var tr = $("<tr/>");
    if (this.mode == 'add' && this.allowRowSelection) {
      tr.append($("<th/>").append().addClass("table-th ").css("width", "30px"));
      width += 30;
    }

    tr.append($("<th/>")
      .append( $("<div/>").append(args.idColumnLabel)
        .addClass("truncate-small"))
      .addClass("table-th").css("width", "110px")
      .attr('title', args.idColumnLabel));
    width += 110;

    for (var i = 0; i < this.appColumns.length; i++) {
      tr.append($("<th/>")
        .append( $("<div/>").append(this.appColumns[i].label)
          .addClass("truncate-small") )
        .addClass("table-th").css("width", "110px")
        .attr('title', this.appColumns[i].label));
      width += 110;
    }

    var rows = this.formDef.rows;
    var fieldCnt = 0;
    for (var j =0 ; j < rows.length; j++) {
      var row = rows[j];
      for (k = 0 ; k < row.length; k++) {
        var minWidth = this.tableRowsData[0].fieldObjs[fieldCnt].minWidth;
        if (!minWidth) {
          minWidth = 200;
        }

        tr.append($("<th/>")
          .append($("<div/>").append(row[k].caption).addClass("truncate"))
          .css("width", minWidth + "px")
          .attr('title', row[k].caption));
        width += minWidth;

        ++fieldCnt;
      }
    }

    var tableHeader = $("<thead/>").append(tr);

    var tbl = $("<table/>").attr("id", "data-table")
      .addClass("table table-striped table-bordered")
      .css("width", width + "px")
      .css("min-width", "100%")
      .css("max-width",  width + "px")
      .append(tableHeader)
      .append(tableBody);

    this.formDiv.append(tbl);
  };

  this.renderEntityRecords = function(entityRecs) {
    var trs = [];
    var records = entityRecs.records;
    if (records.length > 0) {
      for (var i = 0; i < records.length; i++) {
        trs.push(this.renderTableRow(this.mode, entityRecs.key, entityRecs.appColumnsData, records[i]));
      }
    } else {
      trs.push(this.renderTableRow(this.mode, entityRecs.key, entityRecs.appColumnsData));
    }

    return trs;
  };

  this.renderTableRow = function(mode, key, appColumnsData, formData) {
    var recordId = undefined;
    if (formData) {
      recordId = (formData.id || formData.id == 0) ? formData.id : formData.recordId;
    }

    this.fieldObjs = [];

    var tr = $("<tr/>");
    var that = this;

    if (this.mode == 'add' && this.allowRowSelection) {
      var selectRow = $("<input/>").prop({type : 'checkbox', name: 'selectRow', value: key.id , title: key.label});
      selectRow.on("click", function() { that.onRowSelect();});
      tr.append($("<td/>").append(selectRow));
    }

    tr.append($("<td/>")
      .append(
        $("<div/>")
        .addClass("truncate-small")
        .append(key.label)
      )
      .attr('title', key.label));

    var appColumns = this.appColumns;
    for (var i = 0; i < appColumns.length; i++) {
      tr.append($("<td/>")
        .append(
          $("<div/>")
          .addClass("truncate-small")
          .append(appColumnsData[appColumns[i].id])
        )
      );
    }

    var rows = this.formDef.rows;
    for (var i = 0; i < rows.length; i++) {
      var row = rows[i];
      for (j = 0; j < row.length; j++ ) {
        tr.append($("<td/>").append(this.createFieldEl(mode, key, row[j], recordId, formData)));
      }
    }

    for (var i = 0; i < this.fieldObjs.length; ++i) {
      this.fieldObjs[i].postRender();
    }


    this.tableRowsData.push({
      fieldObjs: this.fieldObjs, 
      recordId: recordId, 
      rowId: key.id, 
      rowLabel: key.label, 
      appColumnsData: appColumnsData 
    });

    return tr;
  };

  this.createFieldEl = function(mode, key, field, recId, formData) {
    if (field.type == 'checkbox') { 
      field.type = 'listbox'; 
    } else if (field.type == 'radiobutton') { 
      field.type = 'combobox'; 
    }

    var value = formData ? formData[field.name] : null;
    var appData = {objectId: key.objectId};
    var fieldObj = edu.common.de.FieldFactory.getField(field, undefined, $.extend({appData: appData}, args));

    var inputEl = fieldObj.render(); // TODO: Do we need to render in view mode?

    if (value) {
      fieldObj.setValue(recId, value);
    }
    this.fieldObjs.push(fieldObj);

    if (mode == 'view') {
      return fieldObj.getDisplayValue().value;
    } else {
      return inputEl;
    }
  };

  this.setData = function(tableData) {
    this.tableData =  tableData;
    this.render0();
  };

  this.getData = function() {
    var tblRowsFormData = [];
    if (!this.tableRowsData || this.tableRowsData.length == 0) {
      return tblRowsFormData;
    }

    for (var i = 0; i < this.tableRowsData.length; i++) {
      var fieldObjs = this.tableRowsData[i].fieldObjs;

      var appData = $.extend({}, args.appData); // TODO: Why do we need to copy entire app data?
      appData.id = this.tableRowsData[i].rowId;
      appData.label = this.tableRowsData[i].rowLabel;

      if (!this.validate(fieldObjs) && args.onValidationError) {
        args.onValidationError();
        return;
      }

      var formData = {
        appData: appData,
        recordId: this.tableRowsData[i].recordId
      };
      
      for (var j = 0; j < fieldObjs.length; ++j) {
        var value = fieldObjs[j].getValue();
        if (!value) { // note doesn't have value;
          continue;
        }

        formData[value.name] = value.value;
      }

      tblRowsFormData.push(formData);
    }

    return tblRowsFormData;
  };

  this.validate = function(fieldObjs) {
    var valid = true;
    for (var i = 0; i < fieldObjs.length; ++i) {
      if (!fieldObjs[i].validate()) { // validate all fields
        valid = false;
      }
    }

    return valid;
  };

  this.copyFirstToAll = function() {
    if (!this.tableRowsData || this.tableRowsData.length == 0) {
      return;
    }

    var firstRow = this.tableRowsData[0].fieldObjs;
    for (var i = 1; i < this.tableRowsData.length; i++) {
      var fieldObjs = this.tableRowsData[i].fieldObjs;
      var recordId = this.tableRowsData[i].recordId;
      for (var j = 0; j < fieldObjs.length; j++) {
        var value = firstRow[j].getValue(true);
        if (value != undefined) {
          fieldObjs[j].setValue(recordId, value.value);
        }
      }
    }
  };

  this.deleteRows = function() {
    var selectedRows = [];
    jQuery("input[name='selectRow']").each(function() {
      if (this.checked) {
        selectedRows.push(this);
      }
    });

    var lastIdx = 0;
    for (var i = 0; i < selectedRows.length; ++i) {
      for (var j = lastIdx; j < this.tableRowsData.length; ++j) {
        if (selectedRows[i].value != this.tableRowsData[j].rowId) {
          continue;
        }

        this.tableRowsData.splice(j, 1);
        $(selectedRows[i]).closest("tr").remove();
        lastIdx = j;
        break;
      }
    }
  };

  this.onRowSelect = function() {
    if (!args.onRowSelect) {
      return;
    }

    var checked = false;
    var cbEls = jQuery("input[name='selectRow']");
    for (var i = 0; i < cbEls.length; ++i) {
      if (cbEls[i].checked) {
        checked = true;
        break;
      }
    }

    args.onRowSelect(checked);
  };
};
