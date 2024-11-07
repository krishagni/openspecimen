export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'dropdown',
            name: 'filter.op',
            labelCode: 'queries.operator',
            listSource: {
              displayProp: 'label',
              selectProp: 'name',
              loadFn: ({context}) => context.formData.getAllowedOps()
            },
            validations: {
              required: {
                messageCode: 'queries.operator_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'booleanCheckbox',
            name: 'filter.hasSq',
            inlineLabelCode: 'queries.records_of_another_query',
            showWhen: 'filter.op == "IN" || filter.op == "NOT_IN"'
          }
        ]
      },
      {
        fields: [
          {
            type: 'dropdown',
            name: 'filter.subQuery',
            labelCode: 'queries.query',
            listSource: {
              apiUrl: 'saved-queries',
              searchTerm: 'searchString',
              displayProp: 'title',
              queryParams: {
                static: {
                  returnList: true
                }
              }
            },
            showWhen: 'filter.hasSq',
            validations: {
              required: {
                messageCode: 'queries.query_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'multiselect',
            name: 'filter.values',
            labelCode: 'queries.condition_value',
            showWhen: '!filter.hasSq && valueType == "multiselect"',
            listSource: {
              displayProp: 'value',
              selectProp: 'name',
              loadFn: ({context, query}) => context.formData.getPvs(query)
            },
            validations: {
              required: {
                messageCode: 'queries.condition_value_req'
              }
            }
          }
        ],
      },
      {
        fields: [
          {
            type: 'textarea',
            name: 'filter.valuesCsv',
            labelCode: 'queries.condition_value',
            showWhen: 'valueType == "textarea"',
            validations: {
              required: {
                messageCode: 'queries.condition_value_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'dropdown',
            name: 'filter.valuePv',
            labelCode: 'queries.condition_value',
            showWhen: 'valueType == "dropdown"',
            listSource: {
              displayProp: 'value',
              selectProp: 'value',
              loadFn: ({context, query}) => context.formData.getPvs(query)
            },
            validations: {
              required: {
                messageCode: 'queries.condition_value_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'datePicker',
            name: 'filter.minDateValue',
            labelCode: 'queries.min_date',
            showWhen: 'valueType == "dateRange"',
            validations: {
              required: {
                messageCode: 'queries.min_date_req'
              }
            }
          },
          {
            type: 'datePicker',
            name: 'filter.maxDateValue',
            labelCode: 'queries.max_date',
            showWhen: 'valueType == "dateRange"',
            validations: {
              required: {
                messageCode: 'queries.max_date_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'number',
            name: 'filter.minNumValue',
            labelCode: 'queries.min_value',
            showWhen: 'valueType == "numericRange"',
            validations: {
              required: {
                messageCode: 'queries.min_value_req'
              }
            }
          },
          {
            type: 'datePicker',
            name: 'filter.maxNumValue',
            labelCode: 'queries.max_value',
            showWhen: 'valueType == "numericRange"',
            validations: {
              required: {
                messageCode: 'queries.max_value_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'datePicker',
            name: 'filter.dateValue',
            labelCode: 'queries.condition_value',
            showWhen: 'valueType == "date"',
            dateOnly: true,
            validations: {
              required: {
                messageCode: 'queries.condition_value_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'text',
            name: 'filter.value',
            labelCode: 'queries.condition_value',
            showWhen: 'valueType == "text"',
            validations: {
              required: {
                messageCode: 'queries.condition_value_req'
              }
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'booleanCheckbox',
            name: 'filter.parameterized',
            inlineLabelCode: 'queries.parameterized',
            showWhen: "!filter.hasSq"
          },
          {
            type: 'booleanCheckbox',
            name: 'filter.hideOptions',
            inlineLabelCode: 'queries.hide_options',
            showWhen: '!filter.hasSq && filter.fieldObj.type == "STRING"'
          }
        ]
      },
      {
        fields: [
          {
            type: 'text',
            name: 'filter.desc',
            labelCode: 'queries.description',
            showWhen: 'filter.parameterized == true'
          }
        ]
      }
    ]
  }
}
