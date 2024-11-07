package com.krishagni.catissueplus.core.de.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.de.domain.Filter.Op;
import com.krishagni.catissueplus.core.de.domain.QueryExpressionNode.LogicalOp;
import com.krishagni.catissueplus.core.de.domain.QueryExpressionNode.Parenthesis;
import com.krishagni.catissueplus.core.de.domain.SelectField.Function;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.DataType;
import edu.common.dynamicextensions.domain.nui.LookupControl;
import edu.common.dynamicextensions.domain.nui.SubFormControl;
import edu.common.dynamicextensions.query.QuerySpace;

@Configurable
public class AqlBuilder {
	private static final Pattern TEMPORAL_EXPR = Pattern.compile("[<=>!]|\\sany\\s*$|\\sexists\\s*$|\\snot exists\\s*$|\\sbetween\\s");

	@Autowired
	private DaoFactory daoFactory;


	
	private AqlBuilder() {
		
	}
	
	public static AqlBuilder getInstance() {
		return new AqlBuilder();
	}

	public String getQuery(Object[] selectList, Filter[] filters, QueryExpressionNode[] queryExprNodes, String havingClause) {
		return getQuery(selectList, filters, queryExprNodes, havingClause, null);
	}

	public String getQuery(Object[] selectList, Filter[] filters, QueryExpressionNode[] queryExprNodes, String havingClause, ReportSpec rptSpec) {
		return getQuery(selectList, filters, StringUtils.EMPTY, queryExprNodes, havingClause, rptSpec);
	}

	public String getQuery(Object[] selectList, Filter[] filters, Filter[] conjunctionFilters, QueryExpressionNode[] queryExprNodes, String havingClause) {
		return getQuery(selectList, filters, conjunctionFilters, queryExprNodes, havingClause, null);
	}

	public String getQuery(Object[] selectList, Filter[] filters, Filter[] conjunctionFilters, QueryExpressionNode[] queryExprNodes, String havingClause, ReportSpec rptSpec) {
		Context ctx = new Context();
		return getQuery(ctx, selectList, filters, getConjunction(ctx, conjunctionFilters), queryExprNodes, havingClause, null, rptSpec);
	}

	public String getQuery(Object[] selectList, Filter[] filters, String conjunction, QueryExpressionNode[] queryExprNodes, String havingClause) {
		return getQuery(selectList, filters, conjunction, queryExprNodes, havingClause);
	}

	public String getQuery(Object[] selectList, Filter[] filters, String conjunction, QueryExpressionNode[] queryExprNodes, String havingClause, ReportSpec rptSpec) {
		return getQuery(new Context(), selectList, filters, conjunction, queryExprNodes, havingClause, null, rptSpec);
	}

	public String getQuery(SavedQuery query, Filter[] conjunctions) {
		Context ctx = new Context();
		ctx.qs = query.getQuerySpace();
		return getQuery(ctx, query.getSelectList(), query.getFilters(), getConjunction(ctx, conjunctions), query.getQueryExpression(), query.getHavingClause(), null, query.getReporting());
	}

	public String getQuery(SavedQuery query, String conjunction) {
		return getQuery(query, conjunction, null);
	}

	public String getQuery(SavedQuery query, String conjunction, String orderBy) {
		Context ctx = new Context();
		ctx.qs = query.getQuerySpace();
		return getQuery(ctx, query.getSelectList(), query.getFilters(), conjunction, query.getQueryExpression(), query.getHavingClause(), orderBy, query.getReporting());
	}

	private String getConjunction(Context ctx, Filter[] filters) {
		StringBuilder conjunctionExpr = new StringBuilder();
		if (filters != null) {
			for (int i = 0; i < filters.length; ++i) {
				if (i > 0) {
					conjunctionExpr.append(" and ");
				}

				conjunctionExpr.append(buildFilterExpr(ctx, filters[i]));
			}
		}

		return conjunctionExpr.toString();
	}

	private String getQuery(Context ctx, Object[] selectList, Filter[] filters, String conjunction, QueryExpressionNode[] queryExprNodes, String havingClause, String orderClause, ReportSpec rptSpec) {
		Map<Integer, Filter> filterMap = new HashMap<>();
		for (Filter filter : filters) {
			filterMap.put(filter.getId(), filter);
		}

		String selectClause = buildSelectClause(filterMap, selectList);
		String whereClause = buildWhereClause(ctx, filterMap, queryExprNodes);
		if (StringUtils.isNotBlank(conjunction)) {
			whereClause = "(" + whereClause + ") and (" + conjunction + ")";
		}

		String query = "";
		if (StringUtils.isNotBlank(selectClause)) {
			query = "select " + selectClause + " where ";
		}

		query += whereClause;
		if (StringUtils.isNotBlank(havingClause)) {
			havingClause = havingClause.replaceAll("count\\s*\\(", "count(distinct ");
			havingClause = havingClause.replaceAll("c_count\\s*\\(", "c_count(distinct ");
			query += " having " + havingClause;
		}

		if (StringUtils.isNotBlank(orderClause)) {
			query += " order by " + orderClause;
		}

		query += " " + buildReportExpr(selectList, rptSpec);
		return query;
	}

	private String buildSelectClause(Map<Integer, Filter> filterMap, Object[] selectList) {
		if (selectList == null || selectList.length == 0) {
			return "";
		}

		StringBuilder select = new StringBuilder();
		for (Object field : selectList) {
			SelectField aggField = null;
			if (field instanceof String) {
				aggField = new SelectField();
				aggField.setName((String) field);
			} else if (field instanceof Map) {
				aggField = new ObjectMapper().convertValue(field, SelectField.class);
			} else if (field instanceof SelectField) {
				aggField = (SelectField) field;
			}

			if (aggField == null) {
				continue;
			}

			if (aggField.getAggFns() == null || aggField.getAggFns().isEmpty()) {
				select.append(getFieldExpr(filterMap, aggField, true)).append(", ");
			} else {
				String fieldExpr = getFieldExpr(filterMap, aggField, false);
					
				StringBuilder fnExpr = new StringBuilder();
				for (Function fn : aggField.getAggFns()) {
					if (fnExpr.length() > 0) {
						fnExpr.append(", ");
					}

					if (fn.getName().equals("count")) {
						fnExpr.append("count(distinct ");
					} else if (fn.getName().equals("c_count")) {
						fnExpr.append("c_count(distinct ");
					} else {
						fnExpr.append(fn.getName()).append("(");
					}

					fnExpr.append(fieldExpr).append(") as \"").append(fn.getDesc()).append(" \"");
				}

				select.append(fnExpr.toString()).append(", ");
			}
		}

		int endIdx = select.length() - 2;
		return select.substring(0, Math.max(endIdx, 0));
	}
	
	private String getFieldExpr(Map<Integer, Filter> filterMap, SelectField field, boolean includeDesc) {
		String fieldName = field.getName();
		if (!fieldName.startsWith("$temporal.")) {
			String alias = StringUtils.EMPTY;
			if (includeDesc && StringUtils.isNotBlank(field.getDisplayLabel())) {
				alias = " as \"" + field.getDisplayLabel() + "\"";
			}

			return fieldName + alias;
		}

		Integer filterId = Integer.parseInt(fieldName.substring("$temporal.".length()));
		Filter filter = filterMap.get(filterId);

		String expr = getLhs(filter.getExpr());
		if (includeDesc) {
			if (StringUtils.isNotBlank(field.getDisplayLabel())) {
				expr += " as \"" + field.getDisplayLabel() + "\"";
			} else {
				expr += " as \"" + filter.getDesc() + "\"";
			}
		}

		return expr;
	}

	private String buildWhereClause(Context ctx, Map<Integer, Filter> filterMap, QueryExpressionNode[] queryExprNodes) {
		StringBuilder whereClause = new StringBuilder();
		
		for (QueryExpressionNode node : queryExprNodes) {
			switch (node.getNodeType()) {
			  case FILTER:
				  int filterId;
				  if (node.getValue() instanceof Double) {
					  filterId = ((Double)node.getValue()).intValue();
				  } else {
					  filterId = (Integer)node.getValue();
				  }
				  
				  Filter filter = filterMap.get(filterId);
				  String filterExpr = buildFilterExpr(ctx, filter);
				  whereClause.append(filterExpr);				  				  
				  break;
				  
			  case OPERATOR:
				  LogicalOp op = null;
				  if (node.getValue() instanceof String) {
					  op = LogicalOp.valueOf((String)node.getValue());
				  } else if (node.getValue() instanceof LogicalOp) {
					  op = (LogicalOp)node.getValue();
				  } 
				  whereClause.append(op.symbol());
				  break;
				  
			  case PARENTHESIS:
				  Parenthesis paren = null;
				  if (node.getValue() instanceof String) {
					  paren = Parenthesis.valueOf((String)node.getValue());
				  } else if (node.getValue() instanceof Parenthesis) {
					  paren = (Parenthesis)node.getValue();
				  }				  
				  whereClause.append(paren.symbol());
				  break;				  				
			}
			
			whereClause.append(" ");
		}
		
		return whereClause.toString();
	}
	
	private String buildFilterExpr(Context ctx, Filter filter) {
		if (filter.getExpr() != null) {
			Triple<String, String, String> tObj = getTemporalObj(filter.getExpr());
			if (tObj != null && StringUtils.isBlank(tObj.getRight()) && filter.isParameterized()) {
				return tObj.getLeft() + " any ";
			}

			return filter.getExpr();
		}
		
		String field = filter.getField();
		String[] fieldParts = field.split("\\.");
		if (fieldParts.length <= 1) {
			throw OpenSpecimenException.userError(SavedQueryErrorCode.MALFORMED, "Invalid field: " + field);
		}
				
		StringBuilder filterExpr = new StringBuilder(field).append(" ");
		if (filter.getOp().isUnary()) {
			return filterExpr.append(filter.getOp().symbol()).append(" ").toString();
		}

		if (filter.getSubQueryId() != null) {
			SavedQuery query = filter.getSubQuery();
			if (query == null) {
				query = ctx.getQuery(filter.getSubQueryId()).copy();
				filter.setSubQuery(query);
			}

			ctx.addActiveQuery(filter.getSubQueryId());
			String subAql = getQuery(ctx, new Object[] { field }, query.getFilters(), null, query.getQueryExpression(), query.getHavingClause(), null, null);
			ctx.removeActiveQuery(filter.getSubQueryId());
			return filterExpr.append(filter.getOp().symbol())
				.append(" (").append(subAql).append(")")
				.toString();
		}

		if (filter.getValues() == null || Stream.of(filter.getValues()).allMatch(e -> StringUtils.isBlank(e))) {
			if (filter.isParameterized()) {
				return filterExpr.append(" any ").toString();
			}

			throw OpenSpecimenException.userError(SavedQueryErrorCode.MALFORMED, "Invalid filter: " + filter.getId());
		}

		filterExpr.append(filter.getOp().symbol()).append(" ");

		Container form = null;
		String ctrlName = null;
		Control ctrl = null;
		int startIdx;
		if (fieldParts[1].equals("extensions") || fieldParts[1].equals("customFields")) {
			if (fieldParts.length < 4) {
				return "";
			}
			
			form = ctx.getContainer(fieldParts[2]);
			startIdx = 3;
		} else {
			form = ctx.getContainer(fieldParts[0]);
			startIdx = 1;
		}

		if (form == null) {
			throw OpenSpecimenException.userError(SavedQueryErrorCode.MALFORMED, "Invalid field: " + field);
		}

		for (int idx = startIdx; idx < fieldParts.length; ++idx) {
			ctrl = form.getControlByUdn(fieldParts[idx]);
			if (ctrl == null) {
				break;
			}

			boolean isSf = ctrl instanceof SubFormControl;
			boolean leaf = idx == fieldParts.length - 1;
			if ((leaf && isSf) || (!leaf && !isSf)) {
				// (last field & sub form) or (not last field & not sf)
				throw OpenSpecimenException.userError(SavedQueryErrorCode.MALFORMED, "Invalid field: " + field);
			}

			if (fieldParts[idx].equals("extensions") || fieldParts[idx].equals("customFields")) {
				if (idx >= fieldParts.length - 2) {
					// does not have custom fields form name and field name
					throw OpenSpecimenException.userError(SavedQueryErrorCode.MALFORMED, "Invalid field: " + field);
				}

				form = Container.getContainer(fieldParts[idx + 1]);
				if (form == null) {
					throw OpenSpecimenException.userError(
						SavedQueryErrorCode.MALFORMED,
						"Invalid field: " + field + "; Form " + fieldParts[idx + 1] + " does not exists!"
					);
				}

				++idx;
			} else if (isSf) {
				form = ((SubFormControl) ctrl).getSubContainer();
			}
		}

		if (ctrl == null) {
			throw OpenSpecimenException.userError(SavedQueryErrorCode.MALFORMED, "Invalid field: " + field);
		}

		DataType type = ctrl.getDataType();
		if (ctrl instanceof LookupControl) {
			type = ((LookupControl)ctrl).getValueType();
		}
		
		String[] values = Arrays.copyOf(filter.getValues(), filter.getValues().length);
		quoteStrings(type, values);
		
		String value = values[0];
		if (filter.getOp() == Op.IN || filter.getOp() == Op.NOT_IN) {
			value = "(" + join(values) + ")";
		} else if (filter.getOp() == Op.BETWEEN) {
			value =  "(" + values[0] + ", " + values[1] + ")";
		}
		
		return filterExpr.append(value).toString();
	}

	private String buildReportExpr(Object[] selectList, ReportSpec rptSpec) {
		if (rptSpec == null || StringUtils.isBlank(rptSpec.getType()) || rptSpec.getType().equals("none")) {
			return StringUtils.EMPTY;
		}

		List<Map<String, String>> rptFields = getReportFields(selectList);
		if (rptSpec.getType().equals("columnsummary")) {
			return getColumnSummaryRptExpr(rptSpec, rptFields);
		} else if (rptSpec.getType().equals("crosstab")) {
			return getCrossTabRptExpr(rptSpec, rptFields);
		} else {
			return rptSpec.getType();
		}
	}

	private String getColumnSummaryRptExpr(ReportSpec rpt, List<Map<String, String>> rptFields) {
		Map<String, Object> params = rpt.getParams();
		if (params == null || params.isEmpty()) {
			return StringUtils.EMPTY;
		}

		StringBuilder result = new StringBuilder("columnsummary(");
		boolean addComma = false;

		Object sumParamsObj = params.get("sum");
		if (sumParamsObj instanceof List && !((List) sumParamsObj).isEmpty()) {
			List<Map<String, String>> sumFields = (List<Map<String, String>>) sumParamsObj;
			result.append("\"sum\",\"").append(sumFields.size()).append("\",");
			List<Integer> sumFieldIndices = getFieldIndices(rptFields, sumFields);
			result.append(sumFieldIndices.stream().map(i -> "\"" + i + "\"").collect(Collectors.joining(",")));
			addComma = true;
		}

		Object avgParamsObj = params.get("avg");
		if (avgParamsObj instanceof List && !((List) avgParamsObj).isEmpty()) {
			if (addComma) {
				result.append(",");
			}

			List<Map<String, String>> avgFields = (List<Map<String, String>>) avgParamsObj;
			result.append("\"avg\",\"").append(avgFields.size()).append("\",");
			List<Integer> avgFieldIndices = getFieldIndices(rptFields, avgFields);
			result.append(avgFieldIndices.stream().map(i -> "\"" + i + "\"").collect(Collectors.joining(",")));
		}

		return result.append(")").toString();
	}

	private String getCrossTabRptExpr(ReportSpec rpt, List<Map<String, String>> rptFields) {
		Map<String, Object> params = rpt.getParams();
		if (params == null || params.isEmpty()) {
			return StringUtils.EMPTY;
		}

		List<Integer> rowIndices = null;
		if (params.get("groupRowsBy") instanceof List) {
			rowIndices = getFieldIndices(rptFields, (List<Map<String, String>>) params.get("groupRowsBy"));
		}

		if (rowIndices == null || rowIndices.isEmpty()) {
			return StringUtils.EMPTY;
		}

		List<Integer> colIndices = null;
		if (params.get("groupColBy") instanceof Map) {
			colIndices = getFieldIndices(rptFields, Collections.singletonList((Map<String, String>) params.get("groupColBy")));
		}

		if (colIndices == null || colIndices.isEmpty()) {
			return StringUtils.EMPTY;
		}

		List<Integer> summaryIndices = null;
		if (params.get("summaryFields") instanceof List) {
			summaryIndices = getFieldIndices(rptFields, (List<Map<String, String>>) params.get("summaryFields"));
		}

		if (summaryIndices == null || summaryIndices.isEmpty()) {
			return StringUtils.EMPTY;
		}

		List<Integer> rollupExcl = null;
		if (params.get("rollupExclFields") instanceof List) {
			rollupExcl = getFieldIndices(rptFields, (List<Map<String, String>>) params.get("rollupExclFields"));
		}

		String includeSubTotals = "";
		if (Boolean.TRUE.equals(params.get("includeSubTotals"))) {
			includeSubTotals = ", true";
		}

		List<Integer> rue = rollupExcl;
		summaryIndices = summaryIndices.stream()
			.map(si -> rue != null && rue.indexOf(si) != -1 ? -si : si)
			.collect(Collectors.toList());

		return new StringBuilder("crosstab(")
			.append("(").append(StringUtils.join(rowIndices, ",")).append("), ")
			.append(StringUtils.join(colIndices, ",")).append(",")
			.append("(").append(StringUtils.join(summaryIndices, ",")).append(")")
			.append(includeSubTotals)
			.append(")")
			.toString();
	}

	private List<Map<String, String>> getReportFields(Object[] selectList) {
		List<Map<String, String>> rptFields = new ArrayList<>();

		for (int i = 0; i < selectList.length; ++i) {
			Object selectFieldObj = selectList[i];
			SelectField selectField = null;
			if (selectFieldObj instanceof  String) {
				selectField = new SelectField();
				selectField.setName((String) selectFieldObj);
			} else if (selectFieldObj instanceof Map) {
				selectField = new ObjectMapper().convertValue(selectFieldObj, SelectField.class);
			} else if (selectFieldObj instanceof SelectField) {
				selectField = (SelectField) selectFieldObj;
			}

			List<SelectField.Function> aggFns = selectField.getAggFns();
			if (aggFns == null || aggFns.isEmpty()) {
				Map<String, String> rptField = new HashMap<>();
				rptField.put("id", selectField.getName());
				rptField.put("name", selectField.getName());
				rptField.put("value", selectField.getName()); // TODO
				rptFields.add(rptField);
				continue;
			}

			for (int j = 0; j < aggFns.size(); ++j) {
				Map<String, String> rptField = new HashMap<>();
				rptField.put("id", selectField.getName() + '$' + aggFns.get(j).name);
				rptField.put("name", selectField.getName());
				rptField.put("value", aggFns.get(j).desc);
				rptField.put("aggFn", aggFns.get(j).name);
				rptFields.add(rptField);
			}
		}

		return rptFields;
	}

	private List<Integer> getFieldIndices(List<Map<String, String>> fields, List<Map<String, String>> rptFields) {
		List<Integer> result = new ArrayList<>();
		if (rptFields == null || rptFields.isEmpty()) {
			return result;
		}

		for (int i = 0; i < rptFields.size(); ++i) {
			Map<String, String> rptField = rptFields.get(i);
			for (int j = 0; j < fields.size(); ++j) {
				Map<String, String> field = fields.get(j);
				if (StringUtils.equals(field.get("id"), rptField.get("id"))) {
					result.add(j + 1);
					break;
				}
			}
		}

		return result;
	}
	
	private void quoteStrings(DataType type, String[] values) {
		if (type != DataType.STRING && type != DataType.DATE) {
			return;
		}
		
		for (int i = 0; i < values.length; ++i) {
			values[i] = "\"" + values[i] + "\"";   
		}		
	}
	
	private String join(String[] values) {
		StringBuilder result = new StringBuilder();
		for (String val : values) {
			result.append(val).append(", ");
		}
        
		int endIdx = result.length() - 2;
		return result.substring(0, endIdx < 0 ? 0 : endIdx);
	}
	
	private String getLhs(String temporalExpr) {
		Triple<String, String, String> tObj = getTemporalObj(temporalExpr);
		if (tObj == null) {
			return temporalExpr;
		}

		return tObj.getLeft();
	}

	private Triple<String, String, String> getTemporalObj(String temporalExpr) {
		Map<String, String> placeholders = new HashMap<>();
		temporalExpr = insertPlaceholders(temporalExpr, '"', "DQ", placeholders);
		temporalExpr = insertPlaceholders(temporalExpr, '\'', "SQ", placeholders);

		Matcher matcher = TEMPORAL_EXPR.matcher(temporalExpr);
		if (!matcher.find()) {
			return null;
		}

		String lhs = replacePlaceholders(temporalExpr.substring(0, matcher.start()), placeholders);
		String op  = replacePlaceholders(temporalExpr.substring(matcher.start(), matcher.end()), placeholders);
		String rhs = replacePlaceholders(temporalExpr.substring(matcher.end()), placeholders);
		return Triple.of(lhs.trim(), op.trim(), rhs.trim());
	}

	private String insertPlaceholders(String input, char ch, String varPrefix, Map<String, String> placeholders) {
		int matches = 0;

		int lastIdx = 0;
		while (true) {
			int startIdx = input.indexOf(ch, lastIdx);
			if (startIdx == -1) {
				break;
			}

			int endIdx = input.indexOf(ch, startIdx + 1);
			if (endIdx == -1) {
				break;
			}

			String expr = input.substring(startIdx, endIdx + 1);
			String placeholder = varPrefix + matches;
			placeholders.put(placeholder, expr);
			input = input.substring(0, startIdx) + placeholder + input.substring(endIdx + 1);
			++matches;
		}

		return input;
	}

	private String replacePlaceholders(String input, Map<String, String> placeholders) {
		if (StringUtils.isNotBlank(input)) {
			for (Map.Entry<String, String> entry : placeholders.entrySet()) {
				input = input.replaceAll(entry.getKey(), entry.getValue());
			}
		}

		return input;
	}
	
	private class Context {
		private QuerySpace qs;

		private Map<Long, SavedQuery> queryMap = new HashMap<>();

		private Set<Long> activeQueries = new HashSet<>();

		Container getContainer(String formName) {
			if (qs != null) {
				return qs.getForm(formName);
			}

			return Container.getContainer(formName);
		}

		SavedQuery getQuery(Long queryId) {
			SavedQuery query = queryMap.get(queryId);
			if (query == null) {
				query = daoFactory.getSavedQueryDao().getQuery(queryId);
				if (query == null) {
					throw OpenSpecimenException.userError(SavedQueryErrorCode.NOT_FOUND, queryId);
				}

				queryMap.put(queryId, query);
			}

			return query;
		}

		void addActiveQuery(Long queryId) {
			boolean added = activeQueries.add(queryId);
			if (!added) {
				throw OpenSpecimenException.userError(SavedQueryErrorCode.MALFORMED, "One or more cyclic sub-queries found: " + queryId + "!");
			}
		}

		void removeActiveQuery(Long queryId) {
			activeQueries.remove(queryId);
		}
	}
}
