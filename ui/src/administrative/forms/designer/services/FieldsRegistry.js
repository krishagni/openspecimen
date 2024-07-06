
class FieldsRegistry {
    fields = {};

    constructor() {

    }

    registerType(field) {
        this.fields[field.type] = field;
    }

    getTypes() {
        return this.fields;
    }

    getField(type) {
        return this.fields[type];
    }
}

const fields = [
    {
        type: "stringTextField",
        label: "Text Field",
    },
    {
        type: "textArea",
        label: "Text Area",
    },
    {
        type: "numberField",
        label: "Number Field",
    },
    {
        type: "radiobutton",
        label: "Radio Button",
        allowedInSubForm: false
    },
    {
        type: "checkbox",
        label: "Checkbox",
        allowedInSubForm: false
    },
    {
        type: "booleanCheckbox",
        label: "Yes/No Checkbox",
    },
    {
        type: "combobox",
        label: "Dropdown",
    },
    {
        type: "multiSelectListbox",
        label: "Multiselect Dropdown",
    },
    {
        type: "datePicker",
        label: "Date Picker",
    },
    {
        type: "fileUpload",
        label: "File Upload",
    },
    {
        type: "signature",
        label: "Signature",
    },
    {
        type: "label",
        label: "Note",
        allowedInSubForm: false
    },
    {
        type: "userField",
        label: "User",
    },
    {
        type: "pvField",
        label: "Permissible Value",
        validate: function (field) {
            if (!field.attribute) {
                return { status: false, error: 'PV attribute is required' };
            }

            return { status: true };
        }
    },
    {
        type: "siteField",
        label: "Site",
    },
    {
        type: "storageContainer",
        label: "Storage Container",
    },
    {
        type: "subForm",
        label: "Subform",
        allowedInSubForm: false
    },
];

const fr = new FieldsRegistry();
fields.forEach(field => fr.registerType(field));

export default fr;