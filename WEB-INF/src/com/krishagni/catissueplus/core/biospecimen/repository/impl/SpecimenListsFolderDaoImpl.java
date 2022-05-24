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

	@Override
	public List<Long> getCartIds(Long folderId) {
		return getCurrentSession().createCriteria(SpecimenListsFolder.class, "f")
			.createAlias("f.lists", "list")
			.setProjection(Projections.property("list.id"))
			.add(Restrictions.eq("f.id", folderId))
			.list();
	}

	@Override
	public int addCarts(Long folderId, List<Long> cartIds) {
		List<Long> existingCartIds = getCartIds(folderId);
		List<Long> toAdd = Utility.nullSafeStream(cartIds)
			.filter(cartId -> !existingCartIds.contains(cartId))
			.collect(Collectors.toList());

		getCurrentSession().doWork(connection -> {
			try (PreparedStatement pstmt = connection.prepareStatement(INSERT_FOLDER_CART_SQL)) {
				int count = 0;
				for (Long cartId : toAdd) {
					pstmt.setLong(1, folderId);
					pstmt.setLong(2, cartId);
					pstmt.addBatch();

					++count;
					if (count % 25 == 0) {
						pstmt.executeBatch();
					}
				}

				pstmt.executeBatch();
			}
		});

		return toAdd.size();
	}

	@Override
	public int removeCarts(Long folderId, List<Long> cartIds) {
		return getCurrentSession().createSQLQuery(REMOVE_FOLDER_CARTS_SQL)
			.setParameter("folderId", folderId)
			.setParameterList("cartIds", cartIds)
			.executeUpdate();
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

	private static final String INSERT_FOLDER_CART_SQL =
		"insert into os_cart_folder_carts (folder_id, cart_id) values (?, ?)";

	private static final String REMOVE_FOLDER_CARTS_SQL =
		"delete from os_cart_folder_carts where folder_id = :folderId and cart_id in (:cartIds)";
}
