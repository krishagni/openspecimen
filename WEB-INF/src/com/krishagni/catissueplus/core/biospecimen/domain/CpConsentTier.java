package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.envers.Audited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.repository.CollectionProtocolDao;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;

@Configurable
@Audited
public class CpConsentTier extends ConsentTier {
	private CollectionProtocol collectionProtocol;

	@Autowired
	private DaoFactory daoFactory;

	public CollectionProtocol getCollectionProtocol() {
		return collectionProtocol;
	}

	public void setCollectionProtocol(CollectionProtocol collectionProtocol) {
		this.collectionProtocol = collectionProtocol;
	}

	public List<DependentEntityDetail> getDependentEntities() {
		CollectionProtocolDao cpDao = daoFactory.getCollectionProtocolDao();
		int responseCount = cpDao.getConsentRespsCount(this.getId());

		return DependentEntityDetail
			.listBuilder()
			.add(ConsentTierResponse.getEntityName(), responseCount)
			.build();
	}

	public CpConsentTier copy() {
		CpConsentTier result = new CpConsentTier();
		result.setStatement(getStatement());
		result.setActivityStatus(getActivityStatus());
		return result;
	}

	protected Set<String> getAuditStringInclusionProps() {
		return Arrays.stream(new String[] { "id", "collectionProtocol", "statement", "activityStatus" })
			.collect(Collectors.toSet());
	}
}
