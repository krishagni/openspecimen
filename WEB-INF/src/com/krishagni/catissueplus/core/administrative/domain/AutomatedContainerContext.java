package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.krishagni.catissueplus.core.administrative.repository.ContainerStoreListCriteria;
import com.krishagni.catissueplus.core.administrative.services.StorageContainerService;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.TransactionCache;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;

@Configurable
public class AutomatedContainerContext {
	private static final LogUtil logger = LogUtil.getLogger(AutomatedContainerContext.class);

	private static AutomatedContainerContext instance = new AutomatedContainerContext();

	@Autowired
	private DaoFactory daoFactory;

	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	@Autowired
	private StorageContainerService storageContainerSvc;

	public static AutomatedContainerContext getInstance() {
		return instance;
	}

	public void storeSpecimen(StorageContainer container, Specimen specimen) {
		addSpecimen(container, specimen, ContainerStoreList.Op.PUT);
	}

	public void retrieveSpecimen(StorageContainer container, Specimen specimen) {
		addSpecimen(container, specimen, ContainerStoreList.Op.PICK);
	}

	private void addSpecimen(StorageContainer container, Specimen specimen, ContainerStoreList.Op op) {
		if (!container.isAutomated() || !container.isCreateStoreList()) {
			return;
		}

		ContainerStoreList storeList = getStoreLists().get(listLookupKey(container, op));
		if (storeList == null) {
			storeList = createNewList(container, op);
		}

		ContainerStoreListItem item = new ContainerStoreListItem();
		item.setSpecimen(specimen);
		item.setStoreList(storeList);
		item.setComments(specimen.getTransferComments());

		Runnable saveItem = () -> daoFactory.getContainerStoreListDao().saveOrUpdateItem(item);
		if (specimen.getId() == null) {
			specimen.addOnSaveProc(saveItem);
		} else {
			saveItem.run();
		}
	}

	private String listLookupKey(StorageContainer container, ContainerStoreList.Op op) {
		return container.getId() + "-" + op.name();
	}

	private ContainerStoreList createNewList(StorageContainer container, ContainerStoreList.Op op) {
		ContainerStoreList list = new ContainerStoreList();
		list.setContainer(container);
		list.setCreationTime(Calendar.getInstance().getTime());
		list.setOp(op);
		list.setUser(AuthUtil.getCurrentUser());

		daoFactory.getContainerStoreListDao().saveOrUpdate(list);
		getStoreLists().put(listLookupKey(container, op), list);
		return list;
	}

	private Map<String, ContainerStoreList> getStoreLists() {
		Map<String, ContainerStoreList> storeLists = TransactionCache.getInstance().get("containerStoreLists");
		if (storeLists == null) {
			final Map<String, ContainerStoreList> storeListMap = storeLists = new HashMap<>();
			TransactionCache.getInstance().put("containerStoreLists", storeLists,
				(status) -> {
					if (status != 0) {
						logger.error("Transaction failed. Store lists will not be processed. Status = " + status);
						return;
					}

					if (storeListMap.isEmpty()) {
						return;
					}

					taskExecutor.execute(new Runnable() {
						@Override
						public void run() {
							try {
								logger.debug("Executing the queued container store lists");

								setupUserAuthContext();
								storageContainerSvc.processStoreLists(new Supplier<>() {
									private boolean supplied = false;

									@Override
									public List<ContainerStoreList> get() {
										if (supplied) {
											return null;
										}

										supplied = true;
										return getStoreLists(storeListMap.values());
									}
								});
							} catch (Throwable t) {
								logger.error("Error executing the container store lists", t);
							}
						}
					});
				}
			);
		}

		return storeLists;
	}

	@PlusTransactional
	private void setupUserAuthContext() {
		AuthUtil.setCurrentUser(daoFactory.getUserDao().getSystemUser());
	}

	@PlusTransactional
	private List<ContainerStoreList> getStoreLists(Collection<ContainerStoreList> storeLists) {
		ContainerStoreListCriteria crit = new ContainerStoreListCriteria()
			.ids(storeLists.stream().map(ContainerStoreList::getId).collect(Collectors.toList()));
		List<ContainerStoreList> dbStoreLists = daoFactory.getContainerStoreListDao().getStoreLists(crit);
		dbStoreLists.forEach(list -> list.getContainer().getAutoFreezerProvider().getImplClass());
		return dbStoreLists;
	}
}
