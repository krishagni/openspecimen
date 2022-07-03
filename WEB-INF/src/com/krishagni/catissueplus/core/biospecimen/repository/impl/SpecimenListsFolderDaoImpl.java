package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListsFolder;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsFolderDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsFoldersCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenListsFolderDaoImpl extends AbstractDao<SpecimenListsFolder> implements SpecimenListsFolderDao {
	@Override
	public Class<SpecimenListsFolder> getType() {
		return SpecimenListsFolder.class;
	}

	@Override
	public List<SpecimenListsFolder> getFolders(SpecimenListsFoldersCriteria criteria) {
		return getCurrentSession().createCriteria(SpecimenListsFolder.class, "f")
			.add(Subqueries.propertyIn("f.id", getListQuery(criteria)))
			.setFirstResult(criteria.startAt())
			.setMaxResults(criteria.maxResults())
			.addOrder(Order.asc("f.name"))
			.list();
	}

	@Override
	public Long getFoldersCount(SpecimenListsFoldersCriteria criteria) {
		Number count = (Number) getCurrentSession().createCriteria(SpecimenListsFolder.class, "f")
			.add(Subqueries.propertyIn("f.id", getListQuery(criteria)))
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	public Map<Long, Integer> getFolderCartsCount(Collection<Long> folderIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_FOLDER_CARTS_COUNT)
			.setParameterList("folderIds", folderIds)
			.list();

		Map<Long, Integer> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Integer) row[1]);
		}

		return result;
	}

	@Override
	public boolean isAccessible(Long folderId, Long userId) {
		return !getCurrentSession().createCriteria(SpecimenListsFolder.class, "f")
			.createAlias("f.owner", "owner", JoinType.LEFT_OUTER_JOIN)
			.createAlias("f.userGroups", "userGroup", JoinType.LEFT_OUTER_JOIN)
			.createAlias("userGroup.users", "user", JoinType.LEFT_OUTER_JOIN)
			.add(Restrictions.eq("f.id", folderId))
			.add(Restrictions.or(Restrictions.eq("owner.id", userId), Restrictions.eq("user.id", userId)))
			.list().isEmpty();
	}

	private DetachedCriteria getListQuery(SpecimenListsFoldersCriteria criteria) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(SpecimenListsFolder.class, "f")
			.setProjection(Projections.distinct(Projections.property("f.id")));
		Criteria query = detachedCriteria.getExecutableCriteria(getCurrentSession());

		if (StringUtils.isNotBlank(criteria.query())) {
			if (isMySQL()) {
				query.add(Restrictions.like("f.name", criteria.query(), MatchMode.ANYWHERE));
			} else {
				query.add(Restrictions.ilike("f.name", criteria.query(), MatchMode.ANYWHERE));
			}
		}

		if (criteria.userId() != null) {
			query.createAlias("f.owner", "owner", JoinType.LEFT_OUTER_JOIN)
				.createAlias("f.userGroups", "userGroup", JoinType.LEFT_OUTER_JOIN)
				.createAlias("userGroup.users", "user", JoinType.LEFT_OUTER_JOIN)
				.add(
					Restrictions.or(
						Restrictions.eq("owner.id", criteria.userId()),
						Restrictions.eq("user.id", criteria.userId())
					)
				);
		}

		return detachedCriteria;
	}

	private static final String FQN = SpecimenListsFolder.class.getName();

	private static final String GET_FOLDER_CARTS_COUNT = FQN + ".getFolderCartsCount";
}
