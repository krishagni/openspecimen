
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
        type: "pvRadioButton",
        label: "Radio Button",
        allowedInSubForm: false,
        validate: validatePvField
    },
    {
        type: "pvCheckbox",
        label: "Checkbox",
        allowedInSubForm: false,
        validate: validatePvField
    },
    {
        type: "booleanCheckbox",
        label: "Yes/No Checkbox",
    },
    {
        type: "pvField",
        label: "Dropdown",
        validate: validatePvField
    },
    {
        type: "multiSelectListbox",
        label: "Multiselect Dropdown (Deprecated)",
        addable: false,
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
        type: "radiobutton",
        label: "Radio Button (Deprecated)",
        addable: false,
        allowedInSubForm: false
    },
    {
        type: "checkbox",
        label: "Checkbox (Deprecated)",
        addable: false,
        allowedInSubForm: false
    },
    {
        type: "combobox",
        label: "Dropdown (Deprecated)",
        addable: false,
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

function validatePvField(field) {
    if (!field.attribute) {
        return { status: false, error: 'PV attribute is required' };
    }

    return { status: true };
}

const fr = new FieldsRegistry();
fields.forEach(field => fr.registerType(field));

export default fr;
