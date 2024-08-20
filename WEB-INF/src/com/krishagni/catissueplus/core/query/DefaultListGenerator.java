package com.krishagni.catissueplus.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.FacetDetail;
import com.krishagni.catissueplus.core.de.events.GetFacetValuesOp;
import com.krishagni.catissueplus.core.de.events.QueryExecResult;
import com.krishagni.catissueplus.core.de.services.QueryService;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.DataType;
import edu.common.dynamicextensions.domain.nui.LookupControl;

public class DefaultListGenerator implements ListGenerator {

	private QueryService querySvc;

	public void setQuerySvc(QueryService querySvc) {
		this.querySvc = querySvc;
	}

	@Override
	@PlusTransactional
	public ListDetail getList(ListConfig cfg, List<Column> searchCriteria, Column orderBy) {
		if (CollectionUtils.isEmpty(cfg.getColumns())) {
			throw OpenSpecimenException.userError(ListError.NO_COLUMNS);
		}

		return getListDetail(cfg, searchCriteria, orderBy);
	}

	@Override
	@PlusTransactional
	public int getListSize(ListConfig cfg, List<Column> searchCriteria) {
		return getListSize(cfg, getCriteria(cfg, searchCriteria));
	}

	@Override
	public List<Column> getFilters(ListConfig cfg) {
		Map<String, Container> formsCache = new HashMap<>();
		return cfg.getFilters().stream().map(filter -> getFilterDetail(filter, formsCache)).collect(Collectors.toList());
	}

	@Override
	@PlusTransactional
	public Collection<Object> getExpressionValues(ListConfig cfg, String expr, String searchTerm) {
		GetFacetValuesOp op = new GetFacetValuesOp();
		op.setCpId(cfg.getCpId());
		op.setFacets(Collections.singletonList(expr));
		op.setSearchTerm(searchTerm);
		op.setRestriction(getCriteria(cfg, null));
		op.setDisableAccessChecks(true);

		ResponseEvent<List<FacetDetail>> resp = querySvc.getFacetValues(new RequestEvent<>(op));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload().get(0).getValues();
	}

	private String getDataAql(ListConfig cfg, String criteria, Column orderBy) {
		StringBuilder aql = new StringBuilder()
			.append("select ").append(getDistinctExpr(cfg, orderBy)).append(" ").append(getSelectExpr(cfg))
			.append(" where ").append(criteria);

		String orderByExpr = orderBy != null ? getOrderExpr(orderBy) : getOrderExpr(cfg);
		if (StringUtils.isNotBlank(orderByExpr)) {
			aql.append(" order by ").append(orderByExpr);
		}

		aql.append(" limit ").append(cfg.getStartAt()).append(", ").append(cfg.getMaxResults());
		return aql.toString();
	}

	private String getCountAql(ListConfig cfg, String criteria) {
		return new StringBuilder()
			.append("select count(distinct ").append(cfg.getPrimaryColumn().getExpr()).append(")")
			.append(" where ").append(criteria)
			.toString();
	}

	private String getDistinctExpr(ListConfig cfg, Column orderBy) {
		if (!cfg.isDistinct()) {
			return StringUtils.EMPTY;
		}

		List<Column> toAdd = new ArrayList<>();
		if (orderBy != null) {
			toAdd.add(orderBy);
		} else if (cfg.getOrderBy() != null) {
			toAdd.addAll(cfg.getOrderBy());
		}

		StringBuilder distinct = new StringBuilder("distinct ");
		if (!toAdd.isEmpty()) {
			distinct.append(toAdd.stream().map(this::getSelectExpr).collect(Collectors.joining(", ")))
				.append(", ");
		}

		return distinct.toString();
	}

	private String getSelectExpr(ListConfig cfg) {
		StringBuilder select = new StringBuilder();

		String hidden = getSelectExpr(cfg.getHiddenColumns());
		if (StringUtils.isNotBlank(hidden)) {
			select.append(hidden).append(", ");
		}

		return select.append(getSelectExpr(cfg.getColumns())).toString();
	}

	private String getSelectExpr(List<Column> columns) {
		return columns.stream().map(this::getSelectExpr).collect(Collectors.joining(", "));
	}

	private String getSelectExpr(Column column) {
		String expr = column.getExpr();
		if (StringUtils.isNotBlank(column.getCaption())) {
			expr += " as \"" + column.getCaption() + "\"";
		}

		return expr;
	}

	private String getCriteria(ListConfig cfg, List<Column> searchCriteria) {
		StringBuilder criteria = new StringBuilder();

		if (StringUtils.isNotBlank(cfg.getCriteria())) {
			criteria.append("(").append(cfg.getCriteria()).append(")");
		}

		if (StringUtils.isNotBlank(cfg.getRestriction())) {
			if (criteria.length() != 0) {
				criteria.append(" and ");
			}

			criteria.append("(").append(cfg.getRestriction()).append(")");
		}

		String searchCriteriaAql = getSearchCriteria(cfg, searchCriteria);
		if (StringUtils.isNotBlank(searchCriteriaAql)) {
			if (criteria.length() != 0) {
				criteria.append(" and ");
			}

			criteria.append("(").append(searchCriteriaAql).append(")");
		}

		if (criteria.length() == 0) {
			throw OpenSpecimenException.userError(ListError.NO_CRITERIA);
		}

		return criteria.toString();
	}

	private String getSearchCriteria(ListConfig cfg, List<Column> searchCriteria) {
		if (CollectionUtils.isEmpty(searchCriteria)) {
			return StringUtils.EMPTY;
		}

		Map<String, Column> filtersMap = Utility.toLinkedMap(cfg.getFilters(), Column::getExpr, column -> column);

		List<String> invalidFilters = new ArrayList<>();
		Map<String, Container> formsCache = new HashMap<>();
		List<String> aqls = new ArrayList<>();
		for (Column criterion : searchCriteria) {
			Column criterionCfg = filtersMap.get(criterion.getExpr());
			if (criterionCfg == null) {
				if (cfg.getPrimaryColumn() == null || !criterion.getExpr().equals(cfg.getPrimaryColumn().getExpr())) {
					invalidFilters.add(criterion.getExpr());
					continue;
				}

				criterionCfg = cfg.getPrimaryColumn();
			}

			String criterionAql = getSearchCriterion(criterionCfg, criterion, formsCache);
			if (StringUtils.isBlank(criterionAql)) {
				continue;
			}

			aqls.add(criterionAql);
		}

		if (CollectionUtils.isNotEmpty(invalidFilters)) {
			throw OpenSpecimenException.userError(ListError.INVALID_FILTERS, invalidFilters);
		}

		return String.join(" and ", aqls);
	}

	private String getSearchCriterion(Column criterionCfg, Column criterion, Map<String, Container> formsCache) {
		if (CollectionUtils.isEmpty(criterion.getValues())) {
			return null;
		}

		if (criterionCfg.isTemporal()) {
			return getRangeAql(criterion.getExpr(), criterion.getValues());
		}

		DataType type;
		if (criterion.isConcatExpr()) {
			//
			// concat expression is always of string type
			//
			type = DataType.STRING;
		} else {
			Control field = getField(criterion.getExpr(), formsCache);
			if (field instanceof LookupControl) {
				type = ((LookupControl) field).getValueType();
			} else {
				type = field.getDataType();
			}
		}

		switch (type) {
			case INTEGER:
			case FLOAT:
			case DATE:
			case DATE_INTERVAL:
				return getRangeAql(criterion.getExpr(), criterion.getValues());

			default:
				String searchType = criterionCfg.getSearchType();
				if (StringUtils.equals(searchType, "contains") || criterionCfg.isConcatExpr()) {
					return getContainsAql(criterion.getExpr(), criterion.getValues());
				} else {
					return getInAql(criterion.getExpr(), criterion.getValues());
				}
		}
	}

	private String getRangeAql(String expr, List<Object> values) {
		String min = null, max = null;
		if (values.size() == 1) {
			Object minVal = values.get(0);
			if (minVal != null) {
				min = minVal.toString();
			}
		} else {
			Object minVal = values.get(0);
			if (minVal != null) {
				min = minVal.toString();
			}

			Object maxVal = values.get(1);
			if (maxVal != null) {
				max = maxVal.toString();
			}
		}

		if (StringUtils.isBlank(min) && StringUtils.isBlank(max)) {
			return null;
		} else if (StringUtils.isBlank(max)) {
			return expr + " >= " + min;
		} else if (StringUtils.isBlank(min)) {
			return expr + " <= " + max;
		} else {
			return expr + " between(" + min + ", " + max + ")";
		}
	}

	private String getContainsAql(String expr, List<Object> values) {
		Object value = values.get(0);
		if (value == null) {
			return null;
		}

		String strValue = value.toString();
		if (StringUtils.isBlank(strValue)) {
			return null;
		}

		return expr + " contains " + stringLiteral(strValue);
	}

	private String getInAql(String expr, List<Object> values) {
		String inVals = values.stream()
			.map(value -> stringLiteral(value.toString()))
			.filter(StringUtils::isNotBlank)
			.collect(Collectors.joining(", "));

		if (StringUtils.isBlank(inVals)) {
			return null;
		}

		return expr + " in (" + inVals + ")";
	}

	private String stringLiteral(String input) {
		if (StringUtils.isBlank(input)) {
			return StringUtils.EMPTY;
		}

		return "\"" + escapeQuotes(input) + "\"";
	}

	private String escapeQuotes(String input) {
		return input.replaceAll("\"", "\\\\\"");
	}

	private String getOrderExpr(ListConfig cfg) {
		return cfg.getOrderBy().stream().map(this::getOrderExpr).collect(Collectors.joining(", "));
	}

	private String getOrderExpr(Column column) {
		String expr = column.getExpr();
		if ("desc".equals(column.getDirection())) {
			expr += " desc";
		}

		return expr;
	}

	private ListDetail getListDetail(ListConfig cfg, List<Column> searchCriteria, Column orderBy) {
		String criteria = getCriteria(cfg, searchCriteria);

		ListDetail result = getListDetail(cfg, orderBy, getListData(cfg, criteria, orderBy));
		if (result.getRows().size() == cfg.getMaxResults()) {
			if (cfg.isIncludeCount()) {
				result.setSize(getListSize(cfg, criteria));
			}
		} else {
			result.setSize(result.getRows().size());
		}

		return result;
	}

	private QueryExecResult getListData(ListConfig cfg, String criteria, Column orderBy) {
		return executeQuery(getDataAql(cfg, criteria, orderBy), cfg.getCpId(), cfg.getDrivingForm());
	}

	private int getListSize(ListConfig cfg, String criteria) {
		QueryExecResult result = executeQuery(getCountAql(cfg, criteria), cfg.getCpId(), cfg.getDrivingForm());
		return Integer.parseInt(result.getRows().get(0)[0]);
	}

	private QueryExecResult executeQuery(String aql, Long cpId, String drivingForm) {
		ExecuteQueryEventOp op = new ExecuteQueryEventOp();
		op.setAql(aql);
		op.setCpId(cpId);
		op.setRunType("Data");
		op.setDrivingForm(StringUtils.isBlank(drivingForm) ? "Participant" : drivingForm);
		op.setWideRowMode("OFF");
		op.setDisableAccessChecks(true);
		op.setCaseSensitive(Utility.isMySQL());

		RequestEvent<ExecuteQueryEventOp> req = new RequestEvent<>(op);
		ResponseEvent<QueryExecResult> resp = querySvc.executeQuery(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	private ListDetail getListDetail(ListConfig cfg, Column orderBy, QueryExecResult result) {
		int startIdx = 0;
		if (cfg.isDistinct()) {
			startIdx = orderBy != null ? 1 : (cfg.getOrderBy() != null ? cfg.getOrderBy().size() : 0);
		}

		List<Row> rows = new ArrayList<>();
		for (String[] rowData : result.getRows()) {
			Map<String, Object> hidden = new HashMap<>();
			int colIdx = startIdx;
			for (Column hiddenColumn : cfg.getHiddenColumns()) {
				hidden.put(hiddenColumn.getCaption(), rowData[colIdx++]);
			}

			Row row = new Row();
			row.setHidden(hidden);
			row.setData(ArrayUtils.subarray(rowData, colIdx, rowData.length));
			rows.add(row);
		}

		ListDetail listDetail = new ListDetail();
		listDetail.setIcons(Utility.stream(cfg.getIcons()).collect(Collectors.joining("\n")));
		listDetail.setColumns(cfg.getColumns());
		listDetail.setFixedColumns(cfg.getFixedColumns());
		listDetail.setRows(rows);
		return listDetail;
	}

	private Column getFilterDetail(Column input, Map<String, Container> formsCache) {
		Column result = new Column();
		BeanUtils.copyProperties(input, result);

		result.setMetainfo(new HashMap<>());
		result.getMetainfo().putAll(input.getMetainfo());

		if (input.isTemporal()) {
			result.setDataType(DataType.INTEGER.name());
			return result;
		}

		DataType type;
		if (input.isConcatExpr()) {
			type = DataType.STRING;
			result.setSearchType("contains");
		} else {
			Control field = getField(input.getExpr(), formsCache);
			if (!result.getMetainfo().containsKey("phi") && field.isPhi()) {
				result.getMetainfo().put("phi", "true");
			}

			if (field instanceof LookupControl) {
				type = ((LookupControl) field).getValueType();
			} else {
				type = field.getDataType();
			}
		}

		result.setDataType(type.name());
		return result;
	}

	private Control getField(String expr, Map<String, Container> formsCache) {
		String[] exprParts = expr.split("\\.");

		String formName, fieldName;
		if (exprParts[1].equals("extensions") || exprParts[1].equals("customFields")) {
			if (exprParts.length < 4) {
				throw OpenSpecimenException.userError(ListError.INVALID_FIELD, expr);
			}

			formName = exprParts[2];
			fieldName = StringUtils.join(exprParts, ".", 3, exprParts.length);
		} else {
			formName = exprParts[0];
			fieldName = StringUtils.join(exprParts, ".", 1, exprParts.length);
		}

		Container form = formsCache.get(formName);
		if (form == null) {
			form = Container.getContainer(formName);
			if (form == null) {
				throw OpenSpecimenException.userError(ListError.INVALID_FIELD, expr);
			}

			formsCache.put(formName, form);
		}

		Control field = form.getControlByUdn(fieldName, "\\.");
		if (field == null) {
			throw OpenSpecimenException.userError(ListError.INVALID_FIELD, expr);
		}

		return field;
	}
}
