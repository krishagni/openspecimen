import routerSvc from '@/common/services/Router.js';

export default {
  fields:  [
    {
      "type": "text",
      "labelCode": "container_types.name",
      "name": "type.name",
      "validations": {
        "required": {
          "messageCode": "container_types.name_required"
        }
      },
      "summary": true
    },
    {
      "type": "text",
      "labelCode": "container_types.name_format",
      "name": "type.nameFormat",
      "validations": {
        "required": {
          "messageCode": "container_types.name_format_required"
        }
      }
    },
    {
      "type": "number",
      "labelCode": "container_types.temperature",
      "name": "type.temperature",
    },
    {
      "type": "text",
      "labelCode": "container_types.dimension",
      "name": "type.uiDimension",
      "value": ({type}) => {
        if (type.noOfRows > 0 && type.noOfColumns > 0) {
          return type.noOfRows + ' x ' + type.noOfColumns;
        }

        return null;
      },
      "showInOverviewWhen": "type.noOfRows > 0 && type.noOfColumns > 0",
    },
    {
      "type": "number",
      "name": "type.noOfRows",
      "labelCode": "container_types.rows",
      "validations": {
        "required": {
          "messageCode": "container_types.rows_required"
        }
      },
      "dataEntry": true
    },
    {
      "type": "number",
      "name": "type.noOfColumns",
      "labelCode": "container_types.columns",
      "validations": {
        "required": {
          "messageCode": "container_types.columns_required"
        }
      },
      "dataEntry": true
    },
    {
      "type": "radio",
      "labelCode": "container_types.position_labeling",
      "name": "type.positionLabelingMode",
      "options": [
        { "captionCode": "container_types.position_labeling_linear", "value": "LINEAR" },
        { "captionCode": "container_types.position_labeling_two_d",  "value": "TWO_D" }
      ],
      "optionsPerRow": 2,
      "validations": {
        "required": {
          "messageCode": "container_types.position_labeling_required"
        }
      },
      "showInOverviewWhen": "type.noOfRows > 0 && type.noOfColumns > 0",
      "summary": true
    },
    {
      "type": "text",
      "labelCode": "container_types.labeling_scheme",
      "name": "type.uiLabelingScheme",
      "value": ({type}) => {
        if (type.positionLabelingMode == 'TWO_D') {
          return type.rowLabelingScheme + ' x ' + type.columnLabelingScheme;
        }

        return null;
      },
      "showInOverviewWhen": "type.noOfRows > 0 && type.noOfColumns > 0",
      "summary": true
    },
    {
      "type": "dropdown",
      "labelCode": "container_types.row_labeling_scheme",
      "name": "type.rowLabelingScheme",
      "listSource": {
        "options": [
          { "name": "Numbers" },
          { "name": "Alphabets Upper Case" },
          { "name": "Alphabets Lower Case" },
          { "name": "Roman Upper Case" },
          { "name": "Roman Lower Case" },
        ],
        "selectProp": "name",
        "displayProp": "name"
      },
      "showWhen": "type.positionLabelingMode == 'TWO_D'",
      "validations": {
        "required": {
          "messageCode": "container_types.row_labeling_scheme_required"
        }
      },
      "dataEntry": true
    },
    {
      "type": "dropdown",
      "labelCode": "container_types.column_labeling_scheme",
      "name": "type.columnLabelingScheme",
      "listSource": {
        "options": [
          { "name": "Numbers" },
          { "name": "Alphabets Upper Case" },
          { "name": "Alphabets Lower Case" },
          { "name": "Roman Upper Case" },
          { "name": "Roman Lower Case" },
        ],
        "selectProp": "name",
        "displayProp": "name"
      },
      "showWhen": "type.positionLabelingMode == 'TWO_D'",
      "validations": {
        "required": {
          "messageCode": "container_types.column_labeling_scheme_required"
        }
      },
      "dataEntry": true
    },
    {
      "type": "dropdown",
      "labelCode": "container_types.position_assignment",
      "name": "type.positionAssignment",
      "listSource": {
        "options": [
          {"name": "HZ_TOP_DOWN_LEFT_RIGHT",  "value": "Horizontal, top to down, left to right"},
          {"name": "HZ_TOP_DOWN_RIGHT_LEFT",  "value": "Horizontal, top to down, right to left"},
          {"name": "HZ_BOTTOM_UP_LEFT_RIGHT", "value": "Horizontal, bottom to top, left to right"},
          {"name": "HZ_BOTTOM_UP_RIGHT_LEFT", "value": "Horizontal, bottom to top, right to left"},
          {"name": "VT_TOP_DOWN_LEFT_RIGHT",  "value": "Vertical, top to down, left to right"},
          {"name": "VT_TOP_DOWN_RIGHT_LEFT",  "value": "Vertical, top to down, right to left"},
          {"name": "VT_BOTTOM_UP_LEFT_RIGHT", "value": "Vertical, bottom to top, left to right"},
          {"name": "VT_BOTTOM_UP_RIGHT_LEFT", "value": "Vertical, bottom to top, right to left"}
        ],
        "selectProp": "name",
        "displayProp": "value"
      },
      "showInOverviewWhen": "type.noOfRows > 0 && type.noOfColumns > 0",
      "validations": {
        "required": {
          "messageCode": "container_types.position_assignment_required"
        }
      }
    },
    {
      "type": "radio",
      "labelCode": "container_types.store_specimens_q",
      "name": "type.storeSpecimenEnabled",
      "options": [
        { captionCode: 'common.yes', value: true },
        { captionCode: 'common.no',  value: false }
      ],
      "optionsPerRow": 2,
      "validations": {
        "required": {
          "messageCode": "container_types.store_specimens_q_required"
        }
      },
      "summary": true
    },
    {
      "type": "dropdown",
      "labelCode": "container_types.can_hold",
      "name": "type.canHold",
      "listSource": {
        "apiUrl": "container-types",
        "displayProp": "name",
        "searchProp": "name"
      },
      "showWhen": "!type.storeSpecimenEnabled",
      "href": ({type}) => {
        if (!type.canHold) {
          return null;
        }

        return routerSvc.getUrl('ContainerTypeDetail.Overview', {typeId: type.canHold.id});
      }
    }
  ]
}
