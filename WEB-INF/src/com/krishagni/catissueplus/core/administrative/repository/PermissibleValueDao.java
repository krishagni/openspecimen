
package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.PvAttribute;
import com.krishagni.catissueplus.core.administrative.events.ListPvAttributesCriteria;
import com.krishagni.catissueplus.core.administrative.events.ListPvCriteria;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface PermissibleValueDao extends Dao<PermissibleValue>{
	PvAttribute getAttribute(String name);

	List<PvAttribute> getAttributes(ListPvAttributesCriteria criteria);

	Long getAttributesCount(ListPvAttributesCriteria criteria);

	List<PvAttribute> getFormAttributes(Long formId);

	void saveAttribute(PvAttribute attribute);

	void deleteAttribute(String name);

	PvAttribute promoteAttribute(String oldName, String newName, String caption);

	void archiveFormAttributes(Long formId);

	List<PermissibleValue> getPvs(String attribute, Long lastId, int maxResults);

	public List<PermissibleValue> getPvs(ListPvCriteria crit);

	Long getPvsCount(ListPvCriteria crit);

	Map<String, Long> getPvCounts(Collection<String> attributes);

	public List<PermissibleValue> getSpecimenClasses();
	
	public List<String> getSpecimenTypes(Collection<String> specimenClasses);

	public String getSpecimenClass(String type);
	
	public PermissibleValue getByValue(String attribute, String value);
	
	public List<PermissibleValue> getByPropertyKeyValue(String attribute,String propName, String propValue);

	public PermissibleValue getPv(String attribute, String value);

	public PermissibleValue getPv(String attribute, String value, boolean leafNode);

	public PermissibleValue getPv(String attribute, String parentValue, String value);

	public List<PermissibleValue> getPvs(String attribute, Collection<String> values);

	public List<PermissibleValue> getPvs(String attribute, String parentValue, Collection<String> values, boolean leafNode);

	public boolean exists(String attribute, Collection<String> values);
	
	public boolean exists(String attribute, String parentValue, Collection<String> values);
	
	public boolean exists(String attribute, Collection<String> values, boolean leafLevelCheck);
	
	public boolean exists(String attribute, int depth, Collection<String> values);
	
	public boolean exists(String attribute, int depth, Collection<String> values, boolean anyLevel);
}
