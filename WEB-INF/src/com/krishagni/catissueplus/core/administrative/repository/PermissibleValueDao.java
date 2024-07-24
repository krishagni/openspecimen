
package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Collection;
import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.events.ListPvCriteria;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface PermissibleValueDao extends Dao<PermissibleValue>{
	public List<PermissibleValue> getPvs(ListPvCriteria crit);

	Long getPvsCount(ListPvCriteria crit);
	
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
