package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.events.ListPvCriteria;
import com.krishagni.catissueplus.core.administrative.repository.PermissibleValueDao;
import com.krishagni.catissueplus.core.common.OrderBySubstringMatch;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
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
	
	@SuppressWarnings("unchecked")
	@Override
	public List<PermissibleValue> getPvs(ListPvCriteria crit) {
		Criteria query = getPvQuery(crit);		
		if (StringUtils.isNotBlank(crit.query())) {
			query.add(
				Restrictions.or(
					Restrictions.ilike("value", crit.query(), MatchMode.ANYWHERE), 
					Restrictions.ilike("conceptCode", crit.query(), MatchMode.ANYWHERE)
				)
			).addOrder(
				OrderBySubstringMatch.asc("value", crit.query()).ignoreCase()
			);
		}

		return query.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.addOrder(Order.asc("sortOrder"))
			.addOrder(Order.asc("value"))
			.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public PermissibleValue getByValue(String attribute, String value) {
		List<PermissibleValue> pvs = getSessionFactory().getCurrentSession()
				.getNamedQuery(GET_BY_VALUE)
				.setString("attribute", attribute)
				.setString("value", value)
				.list();
		
		return CollectionUtils.isEmpty(pvs) ? null : pvs.iterator().next();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PermissibleValue> getByPropertyKeyValue(String attribute, String propName, String propValue) {
		Object[] bindValues = { propName, propValue, propValue + "^%", "%^" + propValue + "^%", "%^" + propValue};
		Type[] bindTypes = {StringType.INSTANCE, StringType.INSTANCE, StringType.INSTANCE, StringType.INSTANCE, StringType.INSTANCE};
		return (List<PermissibleValue>) getCurrentSession().createCriteria(PermissibleValue.class, "pv")
			.add(Restrictions.eq("pv.attribute", attribute))
			.add(Restrictions.sqlRestriction(PV_PROP_VALUE_MATCH_SQL, bindValues, bindTypes))
			.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<PermissibleValue> getSpecimenClasses() {
		return getCurrentSession().getNamedQuery(GET_SPECIMEN_CLASSES).list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getSpecimenTypes(Collection<String> specimenClasses) {
		return getCurrentSession().getNamedQuery(GET_SPECIMEN_TYPES)
			.setParameterList("specimenClasses", specimenClasses)
			.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getSpecimenClass(String type) {
		List<String> classes = getCurrentSession().getNamedQuery(GET_SPECIMEN_CLASS)
			.setString("type", type)
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
		Criteria query = getCurrentSession().createCriteria(PermissibleValue.class, "pv")
			.add(Restrictions.eq("pv.attribute", attribute))
			.add(Restrictions.in("pv.value", values));

		if (StringUtils.isNotBlank(parentValue)) {
			query.createAlias("pv.parent", "ppv")
				.add(Restrictions.eq("ppv.attribute", attribute))
				.add(Restrictions.eq("ppv.value", parentValue));
		}

		if (leafNode) {
			query.createAlias("children", "c", JoinType.LEFT_OUTER_JOIN)
				.add(Restrictions.isNull("c.id"));
		}

		return query.list();
	}

	@Override
	public boolean exists(String attribute, Collection<String> values) {
		return exists(attribute, values, false);
	}

	@Override
	public boolean exists(String attribute, String parentValue, Collection<String> values) {
		Number count = (Number)sessionFactory.getCurrentSession().createCriteria(PermissibleValue.class)
				.createAlias("parent", "ppv")
				.add(Restrictions.eq("ppv.attribute", attribute))
				.add(Restrictions.eq("ppv.value", parentValue))
				.add(Restrictions.in("value", values))
				.setProjection(Projections.count("id"))
				.uniqueResult();
		return count.intValue() == values.size();
	}

	public boolean exists(String attribute, Collection<String> values, boolean leafLevelCheck) {
		Criteria query = sessionFactory.getCurrentSession()
				.createCriteria(PermissibleValue.class)
				.add(Restrictions.eq("attribute", attribute))
				.add(Restrictions.in("value", values))
				.setProjection(Projections.count("id"));
		
		if (leafLevelCheck) {
			query.createAlias("children", "c", JoinType.LEFT_OUTER_JOIN)
				.add(Restrictions.isNull("c.id"));
		}
		
		Number count = (Number)query.uniqueResult();				
		return count.intValue() == values.size();	
	}
	
	@Override
	public boolean exists(String attribute, int depth, Collection<String> values) {
		return exists(attribute, depth, values, false);
	}
	
	@Override
	public boolean exists(String attribute, int depth, Collection<String> values, boolean anyLevel) {
		Criteria query = sessionFactory.getCurrentSession().createCriteria(PermissibleValue.class)
				.add(Restrictions.in("value", values))
				.setProjection(Projections.count("id"));
		
		for (int i = 1; i <= depth; ++i) {			
			if (i == 1) {
				query.createAlias("parent", "pv" + i, anyLevel ? JoinType.LEFT_OUTER_JOIN : JoinType.INNER_JOIN);
			} else {
				query.createAlias("pv" + (i - 1) + ".parent", "pv" + i, anyLevel ? JoinType.LEFT_OUTER_JOIN : JoinType.INNER_JOIN);
			}			
		}
		
		Disjunction attrCond = Restrictions.disjunction();
		attrCond.add(Restrictions.eq("pv" + depth + ".attribute", attribute));
		if (anyLevel) {
			for (int i = depth - 1; i >= 1; i--) {
				attrCond.add(Restrictions.eq("pv" + i + ".attribute", attribute));
			}
			
			attrCond.add(Restrictions.eq("attribute", attribute));
		}
		
		Number count = (Number)query.add(attrCond).uniqueResult();
		return count.intValue() == values.size();
	}
	
	private Criteria getPvQuery(ListPvCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(PermissibleValue.class);
		if (crit.values() != null) {
			List<String> values = crit.values().stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
			if (!values.isEmpty()) {
				query.add(Restrictions.in("value", values));
			}
		}

		if (StringUtils.isNotBlank(crit.parentAttribute()) || StringUtils.isNotBlank(crit.parentValue())) {
			query.createAlias("parent", "p");
		}
				
		if (StringUtils.isNotBlank(crit.attribute())) {
			query.add(Restrictions.eq("attribute", crit.attribute()));
		} else if (StringUtils.isNotBlank(crit.parentAttribute())) {
			query.add(Restrictions.eq("p.attribute", crit.parentAttribute()));
		}
		
		if (StringUtils.isNotBlank(crit.parentValue())) {
			query.add(Restrictions.eq("p.value", crit.parentValue()));
		}
		
		if (crit.includeOnlyLeafValue()) {
			query.createAlias("children", "c", JoinType.LEFT_OUTER_JOIN)
				.add(Restrictions.isNull("c.id"));			
		}

		if (crit.includeOnlyRootValue()) {
			query.createAlias("parent", "root", JoinType.LEFT_OUTER_JOIN)
				.add(Restrictions.isNull("root.id"));
		}

		if (StringUtils.isBlank(crit.activityStatus())) {
			query.add(Restrictions.eq("activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus()));
		} else if (!crit.activityStatus().equalsIgnoreCase("all")) {
			query.add(Restrictions.eq("activityStatus", crit.activityStatus()));
		}

		return query;
	}

	private static final String FQN = PermissibleValue.class.getName();

	private static final String GET_BY_VALUE = FQN + ".getByValue";

	private static final String GET_SPECIMEN_CLASSES = FQN + ".getSpecimenClasses";

	private static final String GET_SPECIMEN_TYPES = FQN + ".getSpecimenTypes";

	private static final String GET_SPECIMEN_CLASS = FQN + ".getSpecimenClass";

	private static final String PV_PROP_VALUE_MATCH_SQL =
		"exists (" +
		"  select " +
		"    pv_id " +
		"  from" +
		"    os_pv_props " +
		"  where " +
		"    pv_id = {alias}.identifier and " +
		"    name = ? and " +
		"    (value = ? or value like ? or value like ? or value like ?)" +
		")";
}
