package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.events.ListPvCriteria;
import com.krishagni.catissueplus.core.administrative.repository.PermissibleValueDao;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;
import com.krishagni.catissueplus.core.common.util.Status;

public class PermissibleValueDaoImpl extends AbstractDao<PermissibleValue> implements PermissibleValueDao {

	@Override
	public Class<PermissibleValue> getType() {
		return PermissibleValue.class;
	}

	@Override
	public PermissibleValue getById(Long id) {
		return getCurrentSession().get(PermissibleValue.class, id);
	}

	@Override
	public List<PermissibleValue> getPvs(ListPvCriteria crit) {
		return getPvQuery(crit).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Long getPvsCount(ListPvCriteria crit) {
		return getPvQuery(crit).getCount("pv.id");
	}

	@Override
	public PermissibleValue getByValue(String attribute, String value) {
		List<PermissibleValue> pvs = createNamedQuery(GET_BY_VALUE, PermissibleValue.class)
			.setParameter("attribute", attribute)
			.setParameter("value", value)
			.list();
		return CollectionUtils.isEmpty(pvs) ? null : pvs.iterator().next();
	}

	@Override
	public List<PermissibleValue> getByPropertyKeyValue(String attribute, String propName, String propValue) {
		Criteria<PermissibleValue> query = createCriteria(PermissibleValue.class, "pv");

		SubQuery<Long> subQuery = query.createSubQuery(PermissibleValue.class, "pv");
		subQuery.joinMap("pv.props", "props")
			.add(subQuery.eq("pv.attribute", attribute))
			.add(subQuery.eq(subQuery.key("props"), propName))
			.add(subQuery.or(
				subQuery.eq(subQuery.value("props"), propValue),
				subQuery.ilike(subQuery.value("props"), propValue + "^%", false),
				subQuery.ilike(subQuery.value("props"), "%^" + propValue + "^%", false),
				subQuery.ilike(subQuery.value("props"), "%^" + propValue, false)
			))
			.select("pv.id");

		return query.add(query.eq("pv.attribute", attribute))
			.add(query.in("pv.id", subQuery))
			.list();
	}

	@Override
	public List<PermissibleValue> getSpecimenClasses() {
		return createNamedQuery(GET_SPECIMEN_CLASSES, PermissibleValue.class).list();
	}

	@Override
	public List<String> getSpecimenTypes(Collection<String> specimenClasses) {
		return createNamedQuery(GET_SPECIMEN_TYPES, String.class)
			.setParameterList("specimenClasses", specimenClasses)
			.list();
	}

	@Override
	public String getSpecimenClass(String type) {
		List<String> classes = createNamedQuery(GET_SPECIMEN_CLASS, String.class)
			.setParameter("type", type)
			.list();
		return classes.size() == 1 ? classes.get(0) : null;
	}

	@Override
	public PermissibleValue getPv(String attribute, String value) {
		return getPv(attribute, value, false);
	}

	@Override
	public PermissibleValue getPv(String attribute, String value, boolean leafNode) {
		List<PermissibleValue> pvs = getPvs(attribute, null, Collections.singleton(value), leafNode);
		return pvs != null && pvs.size() > 0 ? pvs.iterator().next() : null;
	}

	@Override
	public PermissibleValue getPv(String attribute, String parentValue, String value) {
		List<PermissibleValue> pvs = getPvs(attribute, parentValue, Collections.singleton(value), false);
		return pvs != null && pvs.size() > 0 ? pvs.iterator().next() : null;
	}

	@Override
	public List<PermissibleValue> getPvs(String attribute, Collection<String> values) {
		return getPvs(attribute, null, values, false);
	}

	@Override
	public List<PermissibleValue> getPvs(String attribute, String parentValue, Collection<String> values, boolean leafNode) {
		Criteria<PermissibleValue> query = createCriteria(PermissibleValue.class, "pv");
		query.add(query.eq("pv.attribute", attribute))
			.add(query.in("pv.value", values));

		if (StringUtils.isNotBlank(parentValue)) {
			query.join("pv.parent", "ppv")
				.add(query.eq("ppv.attribute", attribute))
				.add(query.eq("ppv.value", parentValue));
		}

		if (leafNode) {
			query.leftJoin("pv.children", "c")
				.add(query.isNull("c.id"));
		}

		return query.list();
	}

	@Override
	public boolean exists(String attribute, Collection<String> values) {
		return exists(attribute, values, false);
	}

	@Override
	public boolean exists(String attribute, String parentValue, Collection<String> values) {
		Criteria<PermissibleValue> query = createCriteria(PermissibleValue.class, "pv");
		Long count = query.join("pv.parent", "ppv")
			.add(query.eq("ppv.attribute", attribute))
			.add(query.eq("ppv.value", parentValue))
			.add(query.in("pv.value", values))
			.getCount("pv.id");
		return count.intValue() == values.size();
	}

	public boolean exists(String attribute, Collection<String> values, boolean leafLevelCheck) {
		Criteria<PermissibleValue> query = createCriteria(PermissibleValue.class, "pv");
		query.add(query.eq("pv.attribute", attribute))
			.add(query.in("pv.value", values));
		
		if (leafLevelCheck) {
			query.leftJoin("pv.children", "c")
				.add(query.isNull("c.id"));
		}
		
		Long count = query.getCount("pv.id");
		return count.intValue() == values.size();	
	}
	
	@Override
	public boolean exists(String attribute, int depth, Collection<String> values) {
		return exists(attribute, depth, values, false);
	}
	
	@Override
	public boolean exists(String attribute, int depth, Collection<String> values, boolean anyLevel) {
		Criteria<PermissibleValue> query = createCriteria(PermissibleValue.class, "pv");
		query.add(query.in("pv.value", values));

		for (int i = 1; i <= depth; ++i) {			
			if (i == 1) {
				query.createAlias("pv.parent", "pv" + i, anyLevel ? AbstractCriteria.JoinType.LEFT_JOIN : AbstractCriteria.JoinType.INNER_JOIN);
			} else {
				query.createAlias("pv" + (i - 1) + ".parent", "pv" + i, anyLevel ? AbstractCriteria.JoinType.LEFT_JOIN : AbstractCriteria.JoinType.INNER_JOIN);
			}			
		}
		
		Disjunction attrCond = query.disjunction();
		attrCond.add(query.eq("pv" + depth + ".attribute", attribute));
		if (anyLevel) {
			for (int i = depth - 1; i >= 1; i--) {
				attrCond.add(query.eq("pv" + i + ".attribute", attribute));
			}
			
			attrCond.add(query.eq("attribute", attribute));
		}

		Long count = query.add(attrCond).getCount("pv.id");
		return count.intValue() == values.size();
	}
	
	private Criteria<PermissibleValue> getPvQuery(ListPvCriteria crit) {
		Criteria<PermissibleValue> query = createCriteria(PermissibleValue.class, "pv");
		if (crit.values() != null) {
			List<String> values = crit.values().stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
			if (!values.isEmpty()) {
				query.add(query.in("pv.value", values));
			}
		}

		if (StringUtils.isNotBlank(crit.parentAttribute()) || StringUtils.isNotBlank(crit.parentValue())) {
			query.join("pv.parent", "p");
		}

		if (StringUtils.isNotBlank(crit.attribute())) {
			query.add(query.eq("pv.attribute", crit.attribute()));
		} else if (StringUtils.isNotBlank(crit.parentAttribute())) {
			query.add(query.eq("p.attribute", crit.parentAttribute()));
		}

		if (StringUtils.isNotBlank(crit.parentValue())) {
			query.add(query.eq("p.value", crit.parentValue()));
		}

		if (crit.includeOnlyLeafValue()) {
			query.leftJoin("pv.children", "children")
				.add(query.isNull("children.id"));
		}

		if (crit.includeOnlyRootValue()) {
			query.leftJoin("pv.parent", "parent")
				.add(query.isNull("parent.id"));
		}

		if (StringUtils.isBlank(crit.activityStatus())) {
			query.add(query.eq("pv.activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus()));
		} else if (!crit.activityStatus().equalsIgnoreCase("all")) {
			query.add(query.eq("pv.activityStatus", crit.activityStatus()));
		}

		if (StringUtils.isNotBlank(crit.query())) {
			query.add(
				query.or(
					query.ilike("pv.value", crit.query()),
					query.ilike("pv.conceptCode", crit.query())
				)
			);

			query.addOrder(query.asc(query.ilocate("pv.value", crit.query())));
		}

		return query.addOrder(query.asc("pv.sortOrder")).addOrder(query.asc("pv.value"));
	}

	private static final String FQN = PermissibleValue.class.getName();

	private static final String GET_BY_VALUE = FQN + ".getByValue";

	private static final String GET_SPECIMEN_CLASSES = FQN + ".getSpecimenClasses";

	private static final String GET_SPECIMEN_TYPES = FQN + ".getSpecimenTypes";

	private static final String GET_SPECIMEN_CLASS = FQN + ".getSpecimenClass";
}
