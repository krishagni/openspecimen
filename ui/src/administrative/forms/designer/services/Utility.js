
class Utility {
    getOptionRows(options, optsPerRow) {
        if (!options) {
            return [];
        }

        optsPerRow = +optsPerRow || 1;

        let rows = [];
        let currentRow = [];
        for (let option of options) {
            if (!option.value) {
                continue;
            }

            if (currentRow.length >= optsPerRow) {
                rows.push(currentRow);
                currentRow = [];
            }

            currentRow.push(option);
        }

        if (currentRow.length > 0) {
            rows.push(currentRow);
        }

        return rows;
    }

    getOptionWidth(optsPerRow) {
        optsPerRow = +optsPerRow || 1;
        let result = 100 / optsPerRow;
        if (result < 16.66) {
            result = 16.66;
        }

        return result;
    }

    toSnakeCase(input) {
        return input.replaceAll(/^\d+|\W+/g, ' ') // convert whitespaces and other special character into a single space
            .replace(/([a-z])([A-Z])/g, '$1 $2') // camelCaseID = camel Case ID
            .trim()
            .split(' ')
            .join('_')
            .toLowerCase();
    }

    toPvAttributeName(formName, caption) {
        return (formName + '_' + caption).normalize('NFD')
            .replaceAll(/\p{M}/gu, '')
            .replaceAll(/[^A-Za-z0-9]+/g, '_')
            .replaceAll(/^_+|_+$/g, '')
            .toLowerCase() || 'form_dropdown';
    }

    getInterchangeableTypes(field) {
        if (field.type == 'radiobutton') {
          return [
            { name: 'combobox', caption: 'Dropdown' },
            { name: 'stringTextField', caption: 'Text Field' },
            { name: 'pvField', caption: 'Permissible Value', pvConversion: true }
          ]
        } else if (field.type == 'combobox') {
          return [
            { name: 'radiobutton', caption: 'Radio Button' },
            { name: 'stringTextField', caption: 'Text Field' },
            { name: 'pvField', caption: 'Permissible Value', pvConversion: true }
          ]
        } else if (field.type == 'checkbox') {
          return [
            { name: 'multiSelectListbox', caption: 'Multiselect Dropdown' },
            { name: 'pvField', caption: 'Permissible Value', pvConversion: true }
          ]
        } else if (field.type == 'multiSelectListbox') {
          return [
            { name: 'checkbox', caption: 'Checkbox' },
            { name: 'pvField', caption: 'Permissible Value', pvConversion: true }
          ]
        } else if (field.type == 'pvRadioButton') {
          return [
            { name: 'pvField', caption: 'Permissible Value', multiple: false }
          ]
        } else if (field.type == 'pvCheckbox') {
          return [
            { name: 'pvField', caption: 'Permissible Value', multiple: true }
          ]
        }

        return [];
    }
}

export default new Utility();
