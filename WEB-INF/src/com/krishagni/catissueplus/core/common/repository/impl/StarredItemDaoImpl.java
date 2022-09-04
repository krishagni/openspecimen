package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.StarredItem;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.StarredItemDao;

public class StarredItemDaoImpl extends AbstractDao<StarredItem> implements StarredItemDao {
	@Override
	public Class<StarredItem> getType() {
		return StarredItem.class;
	}

	@Override
	public StarredItem getItem(String itemType, Long itemId, Long userId) {
		Criteria<StarredItem> query = createCriteria(StarredItem.class, "si");
		return query.join("si.user", "user")
			.add(query.eq("si.itemType", itemType))
			.add(query.eq("si.itemId", itemId))
			.add(query.eq("user.id", userId))
			.uniqueResult();
	}

	@Override
	public List<Long> getItemIds(String itemType, Long userId) {
		Criteria<Long> query = createCriteria(StarredItem.class, Long.class, "si");
		return query.join("si.user", "user")
			.add(query.eq("si.itemType", itemType))
			.add(query.eq("user.id", userId))
			.select("si.itemId")
			.list();
	}
}