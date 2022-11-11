package com.krishagni.catissueplus.core.init;

import java.util.function.Function;

import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.services.impl.CprSearchKeywordProvider;
import com.krishagni.catissueplus.core.biospecimen.services.impl.MrnSearchKeywordProvider;
import com.krishagni.catissueplus.core.biospecimen.services.impl.ParticipantSearchKeywordProvider;
import com.krishagni.catissueplus.core.common.service.SearchEntityKeywordProvider;
import com.krishagni.catissueplus.core.common.service.SearchService;
import com.krishagni.catissueplus.core.common.service.impl.DefaultSearchEntityKeywordProvider;

public class PostInitializer implements InitializingBean {
	private SearchService searchSvc;

	public void setSearchSvc(SearchService searchSvc) {
		this.searchSvc = searchSvc;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		addKeywordProvider(CollectionProtocol.class, CollectionProtocol.getEntityName(), "title,shortTitle,code,irbIdentifier");
		addKeywordProvider(new CprSearchKeywordProvider());
		addKeywordProvider(new ParticipantSearchKeywordProvider());
		addKeywordProvider(new MrnSearchKeywordProvider());
		addKeywordProvider(Visit.class, Visit.getEntityName(), "name,surgicalPathologyNumber");
		addKeywordProvider(Specimen.class, Specimen.getEntityName(), "label,additionalLabel,barcode");
		addKeywordProvider(StorageContainer.class, StorageContainer.getEntityName(), "name,barcode,displayName");
		addKeywordProvider(Site.class, Site.getEntityName(), "name,code");
		addKeywordProvider(User.class, User.getEntityName(), "firstName,lastName,loginName,emailAddress");
		addKeywordProvider(DistributionProtocol.class, DistributionProtocol.getEntityName(), "title,shortTitle,irbId");
		addKeywordProvider(DistributionOrder.class, DistributionOrder.getEntityName(), "name");
		addKeywordProvider(Shipment.class, Shipment.getEntityName(), "name");
		addKeywordProvider(SpecimenList.class, SpecimenList.getEntityName(), "name", (cart) -> ((SpecimenList) cart).getDeletedOn() != null);
		addKeywordProvider(Institute.class, Institute.getEntityName(), "name");
	}

	private void addKeywordProvider(Class<?> entityClass, String entityName, String keywordProps) {
		addKeywordProvider(entityClass, entityName, keywordProps, null);
	}

	private void addKeywordProvider(Class<?> entityClass, String entityName, String keywordProps, Function<Object, Boolean> isEntityDeletedFn) {
		searchSvc.registerKeywordProvider(new DefaultSearchEntityKeywordProvider(entityClass, entityName, keywordProps, isEntityDeletedFn));
	}

	private void addKeywordProvider(SearchEntityKeywordProvider provider) {
		searchSvc.registerKeywordProvider(provider);
	}
}
