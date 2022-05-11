import routerSvc from '@/common/services/Router.js';

export default {
  fields:  [
    {
      "type": "text",
      "label": "Name",
      "name": "type.name",
      "validations": {
        "required": {
          "message": "Container type name is mandatory"
        }
      },
      "summary": true
    },
    {
      "type": "text",
      "label": "Name Format",
      "name": "type.nameFormat",
      "validations": {
        "required": {
          "message": "Container type name format is mandatory"
        }
      }
    },
    {
      "type": "number",
      "label": "Temperature (Celsius)",
      "name": "type.temperature",
    },
    {
      "type": "text",
      "label": "Dimension",
      "name": "type.uiDimension",
      "value": ({type}) => {
        if (type.noOfRows > 0 && type.noOfColumns > 0) {
          return type.noOfRows + ' x ' + type.noOfColumns;
        }

        return null;
      },
    },
    {
      "type": "number",
      "name": "type.noOfRows",
      "label": "Rows",
      "validations": {
        "required": {
          "message": "Rows is mandatory"
        }
      },
      "dataEntry": true
    },
    {
      "type": "number",
      "name": "type.noOfColumns",
      "label": "Columns",
      "validations": {
        "required": {
          "message": "Columns is mandatory"
        }
      },
      "dataEntry": true
    },
    {
      "type": "radio",
      "label": "Position Labeling",
      "name": "type.positionLabelingMode",
      "options": [
        { "caption": "Linear", "value": "LINEAR" },
        { "caption": "Row and Column", "value": "TWO_D" }
      ],
      "optionsPerRow": 2,
      "validations": {
        "required": {
          "message": "Position Labeling is mandatory"
        }
      },
      "summary": true
    },
    {
      "type": "text",
      "label": "Labeling Scheme",
      "name": "type.uiLabelingScheme",
      "value": ({type}) => {
        if (type.positionLabelingMode == 'TWO_D') {
          return type.rowLabelingScheme + ' x ' + type.columnLabelingScheme;
        }

        return null;
      },
      "summary": true
    },
    {
      "type": "dropdown",
      "label": "Row Labeling Scheme",
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
          "message": "Row Labeling Scheme is mandatory"
        }
      },
      "dataEntry": true
    },
    {
      "type": "dropdown",
      "label": "Column Labeling Scheme",
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
          "message": "Column Labeling Scheme is mandatory"
        }
      },
      "dataEntry": true
    },
    {
      "type": "dropdown",
      "label": "Position Assignment",
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
      "validations": {
        "required": {
          "message": "Position Assignment is mandatory"
        }
      }
    },
    {
      "type": "radio",
      "label": "Store Specimens?",
      "name": "type.storeSpecimenEnabled",
      "options": [
        { caption: 'Yes', value: true },
        { caption: 'No', value: false }
      ],
      "optionsPerRow": 2,
      "validations": {
        "required": {
          "message": "Store Specimens? is mandatory"
        }
      },
      "summary": true
    },
    {
      "type": "dropdown",
      "label": "Can Hold",
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
