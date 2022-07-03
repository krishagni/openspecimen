
package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.util.Utility;

@Audited
public class PermissibleValue extends BaseEntity {
	private String value;

	private String attribute;

	private String conceptCode;

	private PermissibleValue parent;
	
	private Long sortOrder;
	
	private Set<PermissibleValue> children = new HashSet<>();
	
	private Map<String, String> props = new HashMap<>();

	private String activityStatus;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getConceptCode() {
		return conceptCode;
	}

	public void setConceptCode(String conceptCode) {
		this.conceptCode = conceptCode;
	}

	public PermissibleValue getParent() {
		return parent;
	}

	public void setParent(PermissibleValue parent) {
		if (this.equals(parent)) {
			return;
		}

		this.parent = parent;
	}
	
	public Long getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Long sortOrder) {
		this.sortOrder = sortOrder;
	}

	@NotAudited
	public Set<PermissibleValue> getChildren() {
		return children;
	}

	public void setChildren(Set<PermissibleValue> children) {
		this.children = children;
	}

	public Map<String, String> getProps() {
		return props;
	}

	public void setProps(Map<String, String> props) {
		this.props = props;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public void update(PermissibleValue other) {
		setConceptCode(other.getConceptCode());
		setAttribute(other.getAttribute());
		setParent(other.getParent());
		setValue(other.getValue());
		setSortOrder(other.getSortOrder());
		setActivityStatus(other.getActivityStatus());
		updateProps(other);
	}

	public static String getValue(PermissibleValue pv) {
		return pv != null ? pv.getValue() : null;
	}

	public static Set<String> toValueSet(Collection<PermissibleValue> pvs) {
		return Utility.nullSafeStream(pvs).map(pv -> getValue(pv)).collect(Collectors.toSet());
	}

	public static List<String> toValueList(Collection<PermissibleValue> pvs) {
		return Utility.nullSafeStream(pvs).map(pv -> getValue(pv)).collect(Collectors.toList());
	}
	
	private void updateProps(PermissibleValue other) {
		getProps().keySet().retainAll(other.getProps().keySet());
		getProps().putAll(other.getProps());
	}
}
