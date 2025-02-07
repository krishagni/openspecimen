package com.krishagni.catissueplus.core.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ListConfig {
	private Long cpId;

	private String drivingForm;

	private boolean distinct;

	private List<Column> columns = new ArrayList<>();

	private String criteria;

	private String restriction;

	private boolean hideEmptyColumns;

	private List<String> icons = new ArrayList<>();

	private List<Column> orderBy = new ArrayList<>();

	private List<Column> filters = new ArrayList<>();

	private List<Column> hiddenColumns = new ArrayList<>();

	private List<Column> fixedColumns;

	private int startAt = 0;

	private int maxResults = 100;

	private boolean includeCount = true;

	private Column primaryColumn;

	@JsonIgnore
	private BiFunction<Map<String, Object>, List<Row>, Map<String, Object>> fixedColumnsGenerator;

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public String getDrivingForm() {
		return drivingForm;
	}

	public void setDrivingForm(String drivingForm) {
		this.drivingForm = drivingForm;
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	public List<Column> getColumns() {
		return columns;
	}

	public void setColumns(List<Column> columns) {
		this.columns = columns;
	}

	public String getCriteria() {
		return criteria;
	}

	public void setCriteria(String criteria) {
		this.criteria = criteria;
	}

	public String getRestriction() {
		return restriction;
	}

	public void setRestriction(String restriction) {
		this.restriction = restriction;
	}

	public boolean isHideEmptyColumns() {
		return hideEmptyColumns;
	}

	public void setHideEmptyColumns(boolean hideEmptyColumns) {
		this.hideEmptyColumns = hideEmptyColumns;
	}

	public List<Column> getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(List<Column> orderBy) {
		this.orderBy = orderBy;
	}

	public List<Column> getFilters() {
		return filters;
	}

	public void setFilters(List<Column> filters) {
		this.filters = filters;
	}

	public List<Column> getHiddenColumns() {
		return hiddenColumns;
	}

	public void setHiddenColumns(List<Column> hiddenColumns) {
		this.hiddenColumns = hiddenColumns;
	}

	public List<String> getIcons() {
		return icons;
	}

	public void setIcons(List<String> icons) {
		this.icons = icons;
	}

	public List<Column> getFixedColumns() {
		return fixedColumns;
	}

	public void setFixedColumns(List<Column> fixedColumns) {
		this.fixedColumns = fixedColumns;
	}

	public int getStartAt() {
		return startAt;
	}

	public void setStartAt(int startAt) {
		this.startAt = startAt;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	public boolean isIncludeCount() {
		return includeCount;
	}

	public void setIncludeCount(boolean includeCount) {
		this.includeCount = includeCount;
	}

	public Column getPrimaryColumn() {
		return primaryColumn;
	}

	public void setPrimaryColumn(Column primaryColumn) {
		this.primaryColumn = primaryColumn;
	}

	public BiFunction<Map<String, Object>, List<Row>, Map<String, Object>>  getFixedColumnsGenerator() {
		return fixedColumnsGenerator;
	}

	public void setFixedColumnsGenerator(BiFunction<Map<String, Object>, List<Row>, Map<String, Object>>  fixedColumnsGenerator) {
		this.fixedColumnsGenerator = fixedColumnsGenerator;
	}
}
