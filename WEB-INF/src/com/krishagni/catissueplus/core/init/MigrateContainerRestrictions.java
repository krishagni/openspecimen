package com.krishagni.catissueplus.core.init;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;


import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class MigrateContainerRestrictions implements InitializingBean {

	private static final LogUtil LOGGER = LogUtil.getLogger(MigrateContainerRestrictions.class);

	private PlatformTransactionManager txnMgr;

	private DaoFactory daoFactory;

	private JdbcTemplate jdbcTemplate;

	private List<PermissibleValue> specimenClasses;

	public void setTxnMgr(PlatformTransactionManager txnMgr) {
		this.txnMgr = txnMgr;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		migrate();
	}

	public void migrate() 
	throws Exception {		
		List<Long> primaryContIds  = getPrimaryContainerIds();		
		if (CollectionUtils.isEmpty(primaryContIds)) {
			LOGGER.info("No storage containers to migrate. Stopping!");
			return;
		}
		
		loadDefaultSpecimenTypes();
		
		for (final Long contId : primaryContIds) {
			Long startTime = System.currentTimeMillis();
			LOGGER.info("Migration started for primary container with ID: " + contId + " at : " + new Date());

			TransactionTemplate txnTmpl = new TransactionTemplate(txnMgr);
			txnTmpl.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
			txnTmpl.execute(new TransactionCallback<Void>() {
				
				@Override
				public Void doInTransaction(TransactionStatus status) {
					try {
						StorageContainer primaryContainer = daoFactory.getStorageContainerDao().getById(contId);
						migrateRestrictions(primaryContainer);
						normalizeRestrictions(primaryContainer);
						disableOldPrimaryContainer(primaryContainer);
					} catch (Exception e) {
						LOGGER.error(
								"Error while migrating primary container with Id : " + contId, e);
					}
					
					return null;
				}
			});
			
			LOGGER.info(
					"Migration completed for primary container with ID: " + contId + " at : " + new Date() + "." +
					"Time taken: " + (System.currentTimeMillis() - startTime) + " ms");
		}
	}
	
	private List<Long> getPrimaryContainerIds() {
		TransactionTemplate txnTmpl = new TransactionTemplate(txnMgr);
		txnTmpl.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
		return txnTmpl.execute(new TransactionCallback<List<Long>>() {
			
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {				
				try {
					return jdbcTemplate.queryForList(GET_PRIMARY_CONTAINER_IDS, Long.class);					
				} catch (Throwable t) {
					status.setRollbackOnly();
					LOGGER.error("Error fetching primary container IDs", t);
					throw OpenSpecimenException.serverError(t);
				}
			}			
		});
	}
	
	private void loadDefaultSpecimenTypes() {
		TransactionTemplate txnTmpl = new TransactionTemplate(txnMgr);
		txnTmpl.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
		txnTmpl.execute(new TransactionCallback<Void>() {
			
			@Override
			public Void doInTransaction(TransactionStatus status) {				
				try {
					specimenClasses = daoFactory.getPermissibleValueDao().getSpecimenClasses();
					for (PermissibleValue classPv : specimenClasses) {
						for (PermissibleValue typePv : classPv.getChildren()) {
							typePv.getValue(); // eager loading
						}
					}

					return null;
				} catch (Throwable t) {
					status.setRollbackOnly();
					LOGGER.error("Error loading default specimen types", t);
					throw OpenSpecimenException.serverError(t);
				}
			}			
		});
	}

	private void migrateRestrictions(StorageContainer container) 
	throws IllegalAccessException, InvocationTargetException {
		for (StorageContainer childContainer : container.getChildContainers()) {
			migrateRestrictions(childContainer);
		}

		LOGGER.info(
				"Migrating restrictions for container with ID: " + container.getId() + 
				" with name : " +container.getName());
		Set<PermissibleValue> specimenTypeRestrictions = getTypeRestrictions(container.getId());
		container.getAllowedSpecimenTypes().addAll(specimenTypeRestrictions);
		
		Set<String> allowedCps = getCpRestrictions(container.getId()); 		
		List<CollectionProtocol> cps = new ArrayList<CollectionProtocol>();
		if (CollectionUtils.isNotEmpty(allowedCps)) {
			cps = daoFactory.getCollectionProtocolDao().getCpsByShortTitle(allowedCps);
		}		
		container.getAllowedCps().addAll(cps);
		
		setComputedRestrictions(container);

		List<Specimen> problematicSpmns = new ArrayList<Specimen>();
		List<StorageContainer> problematicCntnrs = new ArrayList<StorageContainer>();
		for (StorageContainerPosition pos : container.getOccupiedPositions()) {
			if (pos.getOccupyingSpecimen() != null && !container.canContain(pos.getOccupyingSpecimen())) {
				problematicSpmns.add(pos.getOccupyingSpecimen());
			}
			
			if (pos.getOccupyingContainer() != null && !container.canContain(pos.getOccupyingContainer())) {
				problematicCntnrs.add(pos.getOccupyingContainer());
			}
		}
		
		for (Specimen spmn : problematicSpmns) {
			LOGGER.info(
					"Updating the restrictions for Specimen with label : " + spmn.getLabel() + 
					" for container with name : " + container.getName());
			container.getAllowedSpecimenTypes().add(spmn.getSpecimenType());
			container.getAllowedCps().add(spmn.getCollectionProtocol());			
		}
		
		for (StorageContainer cntnr : problematicCntnrs) {
			LOGGER.info(
					"Updating the restrictions for Container with label : " + cntnr.getName() + 
					" for parent container with name : " + container.getName());
			container.getAllowedSpecimenClasses().addAll(cntnr.getAllowedSpecimenClasses());
			container.getAllowedSpecimenTypes().addAll(cntnr.getAllowedSpecimenTypes());
			container.getAllowedCps().addAll(cntnr.getAllowedCps());			
		}
		
		Set<PermissibleValue> specimenClassRestrictions = getClassRestrictions(container.getAllowedSpecimenTypes());
		container.getAllowedSpecimenClasses().addAll(specimenClassRestrictions);
		setComputedRestrictions(container);
		LOGGER.info("Migration completed for container with name : " + container.getName());
	}

	private void normalizeRestrictions(StorageContainer container) {
		for (StorageContainer childContainer : container.getChildContainers()) {
			normalizeRestrictions(childContainer);
			
			if(!container.getCompAllowedCps().containsAll(childContainer.getAllowedCps())){
				container.getAllowedCps().addAll(childContainer.getAllowedCps());
			}
			
			if(!container.getCompAllowedSpecimenClasses().containsAll(childContainer.getAllowedSpecimenClasses())){
				container.getAllowedSpecimenClasses().addAll(childContainer.getAllowedSpecimenClasses());
			}
			
			if(!container.getCompAllowedSpecimenTypes().containsAll(childContainer.getAllowedSpecimenTypes())){
				container.getAllowedSpecimenTypes().addAll(childContainer.getAllowedSpecimenTypes());
			}
			
			clearDuplicateTypes(container);
		}
		
		for (StorageContainer childContainer : container.getChildContainers()) {
			if (equals(container.getAllowedCps(), childContainer.getAllowedCps())) {
				childContainer.getAllowedCps().clear();
			}
		
			if (equals(container.getAllowedSpecimenClasses(), childContainer.getAllowedSpecimenClasses())) {
				childContainer.getAllowedSpecimenClasses().clear();
			}
			
			if (equals(container.getAllowedSpecimenTypes(), childContainer.getAllowedSpecimenTypes())) {
				childContainer.getAllowedSpecimenTypes().clear();
			}
			
			clearDuplicateTypes(container);
			setComputedRestrictions(childContainer);			
		}
		
		setComputedRestrictions(container);
	}

	private void clearDuplicateTypes(StorageContainer container) {
		if (CollectionUtils.isEmpty(container.getAllowedSpecimenClasses())) {
			return;
		}

		for (PermissibleValue classPv : container.getAllowedSpecimenClasses()) {
			container.getAllowedSpecimenTypes().removeAll(classPv.getChildren());
		}
	}
	
	private void disableOldPrimaryContainer(StorageContainer container) {
		jdbcTemplate.update(DISABLE_OLD_MIGRATED_CONTAINER_SQL, container.getId());
		LOGGER.info("Disabled container with id : " + container.getId() + " in catissue_container table");		
	}
	
	private Set<PermissibleValue> getTypeRestrictions(Long contId) {
		List<String> result = jdbcTemplate.queryForList(GET_TYPE_RESTRICTION_SQL, String.class, contId);
		List<PermissibleValue> typePvs = daoFactory.getPermissibleValueDao()
			.getPvs("specimen_type", null, result, true);
		return new HashSet<>(typePvs);
	}

	private Set<String> getCpRestrictions(Long contId) {
		List<String> result = jdbcTemplate.queryForList(GET_CP_RESTRICTION_SQL, String.class, contId);
		return new HashSet<>(result);
	}
	
	private Set<PermissibleValue> getClassRestrictions(Set<PermissibleValue> specimenTypeRestrictions) {
		Set<PermissibleValue> classRestrictions = new HashSet<>();
		for (PermissibleValue specimenClass : specimenClasses) {
			if (specimenTypeRestrictions.containsAll(specimenClass.getChildren())) {
				classRestrictions.add(specimenClass);
				specimenTypeRestrictions.removeAll(specimenClass.getChildren());
			}
		}
		
		return classRestrictions;
	}
	
	private void setComputedRestrictions(StorageContainer container) {
		container.getCompAllowedSpecimenClasses().addAll(container.computeAllowedSpecimenClasses());
		container.getCompAllowedSpecimenTypes().addAll(container.computeAllowedSpecimenTypes());
		
		for (PermissibleValue allowedClass : container.getAllowedSpecimenClasses()) {
			container.getCompAllowedSpecimenTypes().removeAll(allowedClass.getChildren());
		}
		
		container.getCompAllowedCps().addAll(container.computeAllowedCps());
	}
	
	private static <T> boolean equals(Collection<T> c1, Collection<T> c2) {
		return CollectionUtils.isEqualCollection(c1, c2);
	}
	
	private static final String GET_PRIMARY_CONTAINER_IDS =
			"select " +
					"cc.identifier " +
			"from " +
					"catissue_container cc "+
			    "left join catissue_container_position ccp on ccp.container_id = cc.identifier " +
			"where " +
					"cc.activity_status <> 'Disabled' and ccp.parent_container_id is null ";

	private static final String GET_TYPE_RESTRICTION_SQL = 
			"select " +
					"specimen_type as specimen_type " +
			"from " + 
					"catissue_stor_cont_spec_type " +
			"where " + 
					"storage_container_id = ?";

	private static final String GET_CP_RESTRICTION_SQL =
			"select " + 
					"cp.short_title as short_title " + 
			"from " +
					"catissue_st_cont_coll_prot_rel cpRel " +
					"join catissue_collection_protocol cp on cpRel.collection_protocol_id = cp.identifier " +
			"where " +
					"storage_container_id = ?";
	
	private static final String DISABLE_OLD_MIGRATED_CONTAINER_SQL = 
			"update " +
					"catissue_container " +
			"set " +
					"activity_status = 'Disabled' " +
			"where " +
					"identifier = ? ";
}