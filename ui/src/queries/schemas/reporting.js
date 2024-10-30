export default {
  layout: {
    rows: [
      {
        fields: [
          {
            type: 'textarea',
            name: 'havingClause',
            labelCode: 'queries.aggregate_filters',
            rows: 2
          }
        ]
      },
      {
        fields: [
          {
            type: 'dropdown',
            name: 'reporting.type',
            labelCode: 'queries.report_type',
            listSource: {
              selectProp: 'name',
              displayProp: 'label',
              options: [
                { name: 'none', label: window.osSvc.i18nSvc.msg('common.none') },
                { name: 'crosstab', label: window.osSvc.i18nSvc.msg('queries.pivot_table') },
                { name: 'columnsummary', label: window.osSvc.i18nSvc.msg('queries.column_summary') },
                { name: 'specimenqty', label: window.osSvc.i18nSvc.msg('queries.limit_specimen_qty') }
              ]
            }
          }
        ]
      },
      {
        fields: [
          {
            type: 'multiselect',
            name: 'reporting.params.groupRowsBy',
            labelCode: 'queries.group_rows',
            listSource: {
              displayProp: (option) => option.value || option.displayLabel || (option.field.formCaption + ': ' + option.field.label),
              loadFn: ({context}) => context.formData.getSelectedFields('row_fields')
            },
            showWhen: 'reporting.type == "crosstab"'
          }
        ]
      },
      {
        fields: [
          {
            type: 'dropdown',
            name: 'reporting.params.groupColBy',
            labelCode: 'queries.group_columns',
            listSource: {
              displayProp: (option) => option.value || option.displayLabel || (option.field.formCaption + ': ' + option.field.label),
              loadFn: ({context}) => context.formData.getSelectedFields('column_fields')
            },
            showWhen: 'reporting.type == "crosstab"'
          }
        ]
      },
      {
        fields: [
          {
            type: 'multiselect',
            name: 'reporting.params.summaryFields',
            labelCode: 'queries.value_fields',
            listSource: {
              displayProp: (option) => option.value || option.displayLabel || (option.field.formCaption + ': ' + option.field.label),
              loadFn: ({context}) => context.formData.getSelectedFields('value_fields')
            },
            showWhen: 'reporting.type == "crosstab"'
          }
        ]
      },
      {
        fields: [
          {
            type: 'multiselect',
            name: 'reporting.params.rollupExclFields',
            labelCode: 'queries.exclude_rollup_for',
            listSource: {
              displayProp: (option) => option.value || option.displayLabel || (option.field.formCaption + ': ' + option.field.label),
              loadFn: ({context}) => context.formData.getValueFields()
            },
            showWhen: 'reporting.type == "crosstab"'
          }
        ]
      },
      {
        fields: [
          {
            type: 'booleanCheckbox',
            name: 'reporting.params.includeSubTotals',
            inlineLabelCode: 'queries.include_sub_totals',
            showWhen: 'reporting.type == "crosstab"'
          }
        ]
      },
      {
        fields: [
          {
            type: 'multiselect',
            name: 'reporting.params.sum',
            labelCode: 'queries.total_fields',
            listSource: {
              displayProp: (option) => option.value || option.displayLabel || (option.field.formCaption + ': ' + option.field.label),
              loadFn: ({context}) => context.formData.getColumnSummaryTableFields('sum')
            },
            showWhen: 'reporting.type == "columnsummary"'
          }
        ]
      },
      {
        fields: [
          {
            type: 'multiselect',
            name: 'reporting.params.avg',
            labelCode: 'queries.avg_fields',
            listSource: {
              displayProp: (option) => option.value || option.displayLabel || (option.field.formCaption + ': ' + option.field.label),
              loadFn: ({context}) => context.formData.getColumnSummaryTableFields('avg')
            },
            showWhen: 'reporting.type == "columnsummary"'
          }
        ]
      },
      {
        fields: [
          {
            type: 'radio',
            name: 'reporting.params.restrictBy',
            labelCode: 'queries.restrict_by',
            options: [
              { captionCode: 'queries.participant', value: 'participant' },
              { captionCode: 'queries.parent_specimen', value: 'parent_specimen' }
            ],
            optionsPerRow: 2,
            showWhen: 'reporting.type == "specimenqty"'
          }
        ]
      },
      {
        fields: [
          {
            type: 'number',
            name: 'reporting.params.minQty',
            labelCode: 'queries.min_qty',
            showWhen: 'reporting.type == "specimenqty"'
          }
        ]
      },
      {
        fields: [
          {
            type: 'number',
            name: 'reporting.params.maxQty',
            labelCode: 'queries.max_qty',
            showWhen: 'reporting.type == "specimenqty"'
          }
        ]
      }
    ]
  }
}
