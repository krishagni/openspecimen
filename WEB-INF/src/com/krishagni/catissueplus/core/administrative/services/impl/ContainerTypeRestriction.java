package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.services.ContainerSelectionRule;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.Restriction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;

@Configurable
public class ContainerTypeRestriction implements ContainerSelectionRule {

	@Autowired
	private SessionFactory sessionFactory;

	@Override
	public String getName() {
		return "ContainerType";
	}

	@Override
	public String getSql(String containerTabAlias, Map<String, Object> params) {
		return String.format(SQL_TMPL, containerTabAlias, getTypeName(params));
	}

	@Override
	public Restriction getRestriction(AbstractCriteria<?, ?> query, String containerObjAlias, Map<String, Object> params) {
		String typeName = getTypeName(params);

		SubQuery<Long> subQuery = query.createSubQuery(StorageContainer.class, Long.class, "container");
		subQuery.createAlias("container.type", "type")
			.add(subQuery.eq("type.name", typeName))
			.add(subQuery.eq("container.storeSpecimenEnabled", true))
			.distinct().select("container.id");
		return query.in(containerObjAlias + ".id", subQuery);
	}

	@Override
	public boolean eval(StorageContainer container, Map<String, Object> params) {
		return container.getType() != null && container.getType().getName().equals(getTypeName(params));
	}

	private String getTypeName(Map<String, Object> params) {
		Object value = params.get("name");
		if (!(value instanceof String)) {
			throw new IllegalArgumentException("Invalid container type name");
		}

		return (String) value;
	}

	private static final String SQL_TMPL = "%s.identifier in ( " +
		"select " +
		"  c.identifier " +
		"from " +
		"  os_storage_containers c " +
		"  inner join os_container_types t on t.identifier = c.type_id " +
		"where" +
		"  t.name = '%s' and " +
		"  c.store_specimens = 1 and " +
		"  c.activity_status != 'Disabled')";
}
