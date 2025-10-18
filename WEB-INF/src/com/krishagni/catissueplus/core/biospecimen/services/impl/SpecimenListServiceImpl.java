package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.events.BoxDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.administrative.services.StorageContainerService;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.PickedSpecimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListItem;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListsFolder;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimensPickList;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenListErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenListFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenListsFolderFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.impl.SpecimenListsFolderErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.PickListSpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PickListSpecimensOp;
import com.krishagni.catissueplus.core.biospecimen.events.ShareSpecimenListOp;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListSummary;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListsFolderDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListsFolderSummary;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimensPickListDetail;
import com.krishagni.catissueplus.core.biospecimen.events.UpdateFolderCartsOp;
import com.krishagni.catissueplus.core.biospecimen.events.UpdateListSpecimensOp;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.PickListSpecimensCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.PickListsCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsFoldersCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.impl.BiospecimenDaoHelper;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenListService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.Tuple;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.StarredItemService;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.events.QueryExecResult;
import com.krishagni.catissueplus.core.de.services.QueryService;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;
import com.krishagni.catissueplus.core.query.Column;
import com.krishagni.catissueplus.core.query.ListConfig;
import com.krishagni.catissueplus.core.query.ListService;
import com.krishagni.catissueplus.core.query.ListUtil;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.query.WideRowMode;


public class SpecimenListServiceImpl implements SpecimenListService, InitializingBean, ObjectAccessor {

	private static final LogUtil logger = LogUtil.getLogger(SpecimenListServiceImpl.class);

	private static final Pattern DEF_LIST_NAME_PATTERN = Pattern.compile("\\$\\$\\$\\$user_\\d+");

	private SpecimenListFactory specimenListFactory;
	
	private DaoFactory daoFactory;

	private ListService listSvc;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;

	private QueryService querySvc;

	private StarredItemService starredItemSvc;

	private SpecimenListsFolderFactory folderFactory;

	private StorageContainerService storageContainerSvc;

	public SpecimenListFactory getSpecimenListFactory() {
		return specimenListFactory;
	}

	public void setSpecimenListFactory(SpecimenListFactory specimenListFactory) {
		this.specimenListFactory = specimenListFactory;
	}

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setListSvc(ListService listSvc) {
		this.listSvc = listSvc;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	public void setQuerySvc(QueryService querySvc) {
		this.querySvc = querySvc;
	}

	public void setStarredItemSvc(StarredItemService starredItemSvc) {
		this.starredItemSvc = starredItemSvc;
	}

	public void setFolderFactory(SpecimenListsFolderFactory folderFactory) {
		this.folderFactory = folderFactory;
	}

	public void setStorageContainerSvc(StorageContainerService storageContainerSvc) {
		this.storageContainerSvc = storageContainerSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenListSummary>> getSpecimenLists(RequestEvent<SpecimenListsCriteria> req) {
		try {
			List<SpecimenListSummary> lists = new ArrayList<>();
			SpecimenListsCriteria crit = addSpecimenListsCriteria(req.getPayload());
			if (crit.orderByStarred()) {
				List<Long> listIds = daoFactory.getStarredItemDao().getItemIds(getObjectName(), AuthUtil.getCurrentUser().getId());
				if (!listIds.isEmpty()) {
					crit.ids(listIds);
					lists = daoFactory.getSpecimenListDao().getSpecimenLists(crit);
					crit.ids(Collections.emptyList()).notInIds(listIds);
					lists.forEach(l -> l.setStarred(true));
				}
			}

			if (lists.size() < crit.maxResults()) {
				crit.maxResults(crit.maxResults() - lists.size());
				lists.addAll(daoFactory.getSpecimenListDao().getSpecimenLists(crit));
			}

			return ResponseEvent.response(lists);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getSpecimenListsCount(RequestEvent<SpecimenListsCriteria> req) {
		try {
			SpecimenListsCriteria crit = addSpecimenListsCriteria(req.getPayload());
			return ResponseEvent.response(daoFactory.getSpecimenListDao().getSpecimenListsCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenListDetail> getSpecimenList(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			SpecimenList specimenList = getSpecimenList(crit.getId(), crit.getName());
			return ResponseEvent.response(SpecimenListDetail.from(specimenList));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);			
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenListDetail> createSpecimenList(RequestEvent<SpecimenListDetail> req) {
		try {
			SpecimenListDetail listDetails = req.getPayload();
			
			List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				return ResponseEvent.userError(SpecimenListErrorCode.ACCESS_NOT_ALLOWED);
			}
			
			UserSummary owner = new UserSummary();
			owner.setId(AuthUtil.getCurrentUser().getId());
			listDetails.setOwner(owner);
			
			SpecimenList specimenList = specimenListFactory.createSpecimenList(listDetails);
			ensureUniqueName(specimenList);
			ensureValidSpecimensAndUsers(listDetails, specimenList, siteCps);

			daoFactory.getSpecimenListDao().saveOrUpdate(specimenList);

			// init the shared users collection. otherwise, lazy exception is generated when large
			// number of cart items are saved.
			Collection<User> allSharedUsers = specimenList.getAllSharedUsers();
			saveListItems(specimenList, listDetails.getSpecimenIds(), true);
			notifyUsersOnCreate(specimenList, allSharedUsers);
			EventPublisher.getInstance().publish(new SpecimenListSavedEvent(specimenList, 0));
			return ResponseEvent.response(SpecimenListDetail.from(specimenList));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenListDetail> updateSpecimenList(RequestEvent<SpecimenListDetail> req) {
		return updateSpecimenList(req, false);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenListDetail> patchSpecimenList(RequestEvent<SpecimenListDetail> req) {
		return updateSpecimenList(req, true);
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenListDetail> deleteSpecimenList(RequestEvent<Long> req) {
		try {
			SpecimenList existing = getSpecimenList(req.getPayload(), null);

			//
			// copy of deleted list
			//
			SpecimenList deletedSpecimenList = new SpecimenList();
			BeanUtils.copyProperties(existing, deletedSpecimenList);

			existing.delete();
			daoFactory.getSpecimenListDao().saveOrUpdate(existing);
			notifyUsersOnDelete(deletedSpecimenList);
			EventPublisher.getInstance().publish(new SpecimenListSavedEvent(deletedSpecimenList, 2));
			return ResponseEvent.response(SpecimenListDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenInfo>> getListSpecimens(RequestEvent<SpecimenListCriteria> req) {
		try {
			SpecimenListCriteria crit = getListSpecimensCriteria(req.getPayload());
			List<Specimen> specimens = daoFactory.getSpecimenDao().getSpecimens(crit);
			return ResponseEvent.response(SpecimenInfo.from(specimens));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> getListSpecimensCount(RequestEvent<SpecimenListCriteria> req) {
		try {
			SpecimenListCriteria crit = getListSpecimensCriteria(req.getPayload());
			return ResponseEvent.response(daoFactory.getSpecimenDao().getSpecimensCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenInfo>> getListSpecimensSortedByRel(RequestEvent<EntityQueryCriteria> req) {
		try {
			int maxSpmns = ConfigUtil.getInstance().getIntSetting(ConfigParams.MODULE, ConfigParams.REL_SORTING_MAX_SPMNS, 250);

			SpecimenList list = getSpecimenList(req.getPayload().getId(), req.getPayload().getName());
			int listSize = daoFactory.getSpecimenListDao().getListSpecimensCount(list.getId());
			if (listSize > maxSpmns) {
				return ResponseEvent.userError(SpecimenListErrorCode.EXCEEDS_REL_SORT_SIZE, list.getName(), maxSpmns);
			}

			List<Specimen> specimens = getReadAccessSpecimens(list.getId(), listSize);
			return ResponseEvent.response(SpecimenInfo.from(SpecimenList.groupByAncestors(specimens)));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer>  updateListSpecimens(RequestEvent<UpdateListSpecimensOp> req) {
		try {
			UpdateListSpecimensOp opDetail = req.getPayload();
			if (CollectionUtils.isEmpty(opDetail.getSpecimens())) {
				return ResponseEvent.response(0);
			}

			SpecimenList specimenList = getSpecimenList(opDetail.getListId(), null);
			if (StringUtils.isNotBlank(specimenList.getSourceEntityType())) {
				return ResponseEvent.userError(SpecimenListErrorCode.ADD_RM_SPMNS_NA, specimenList.getDisplayName(), specimenList.getSourceEntityType(), specimenList.getSourceEntityId());
			}

			ensureValidSpecimens(opDetail.getSpecimens(), null);
			return ResponseEvent.response(updateListSpecimens(specimenList, opDetail));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public int updateListSpecimens(SpecimenList specimenList, UpdateListSpecimensOp opDetail) {
		switch (opDetail.getOp()) {
			case ADD:
				if (specimenList.getId() == null) {
					daoFactory.getSpecimenListDao().saveOrUpdate(specimenList);
				}

				return saveListItems(specimenList, opDetail.getSpecimens(), false);

			case REMOVE:
				if (specimenList.getId() != null) {
					return deleteListItems(specimenList, opDetail.getSpecimens());
				}
		}

		return 0;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> addChildSpecimens(RequestEvent<Long> req) {
		try {
			SpecimenList list = getSpecimenList(req.getPayload(), null);
			daoFactory.getSpecimenListDao().addChildSpecimens(list.getId(), ConfigUtil.getInstance().isOracle());
			return ResponseEvent.response(Boolean.TRUE);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<UserSummary>> shareSpecimenList(RequestEvent<ShareSpecimenListOp> req) {
		try {
			ShareSpecimenListOp opDetail = req.getPayload();
			SpecimenList specimenList = getSpecimenList(opDetail.getListId(), null);

			List<User> users = null;
			List<Long> userIds = opDetail.getUserIds();
			if (userIds == null || userIds.isEmpty()) {
				users = new ArrayList<User>();
			} else {
				ensureValidUsers(userIds);
				users = daoFactory.getUserDao().getUsersByIds(userIds);
			}
			
			switch (opDetail.getOp()) {
				case ADD:
					specimenList.addSharedUsers(users);
					break;
					
				case UPDATE:
					specimenList.updateSharedUsers(users);
					break;
					
				case REMOVE:
					specimenList.removeSharedUsers(users);
					break;					
			}

			daoFactory.getSpecimenListDao().saveOrUpdate(specimenList);			
			List<UserSummary> result = new ArrayList<UserSummary>();
			for (User user : specimenList.getSharedWith()) {
				result.add(UserSummary.from(user));
			}
			
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> exportSpecimenList(RequestEvent<SpecimenListCriteria> req) {
		try {
			return ResponseEvent.response(exportSpecimenList0(req.getPayload(), null));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	//
	// Specimen Lists Folder APIs
	//

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenListsFolderSummary>> getFolders(RequestEvent<SpecimenListsFoldersCriteria> req) {
		try {
			List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			SpecimenListsFoldersCriteria criteria = req.getPayload();
			if (!AuthUtil.isAdmin()) {
				criteria.userId(AuthUtil.getCurrentUser().getId());
			}

			List<SpecimenListsFolder> folders = daoFactory.getSpecimenListsFolderDao().getFolders(criteria);
			List<SpecimenListsFolderSummary> result = SpecimenListsFolderSummary.from(folders);
			if (criteria.includeStat() && !folders.isEmpty()) {
				List<Long> folderIds = folders.stream().map(SpecimenListsFolder::getId).collect(Collectors.toList());
				Map<Long, Integer> counts = daoFactory.getSpecimenListsFolderDao().getFolderCartsCount(folderIds);
				result.forEach(folder -> folder.setCartsCount(counts.getOrDefault(folder.getId(), 0)));
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getFoldersCount(RequestEvent<SpecimenListsFoldersCriteria> req) {
		try {
			List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			SpecimenListsFoldersCriteria criteria = req.getPayload();
			if (!AuthUtil.isAdmin()) {
				criteria.userId(AuthUtil.getCurrentUser().getId());
			}

			return ResponseEvent.response(daoFactory.getSpecimenListsFolderDao().getFoldersCount(criteria));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenListsFolderDetail> getFolder(RequestEvent<EntityQueryCriteria> req) {
		try {
			SpecimenListsFolder folder = getFolder(req.getPayload().getId(), true);
			return ResponseEvent.response(SpecimenListsFolderDetail.from(folder));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenListsFolderDetail> createFolder(RequestEvent<SpecimenListsFolderDetail> req) {
		try {
			List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			SpecimenListsFolder folder = folderFactory.createFolder(req.getPayload());
			daoFactory.getSpecimenListsFolderDao().saveOrUpdate(folder);
			updateFolderCarts(folder, UpdateFolderCartsOp.Operation.ADD, req.getPayload().getCartIds());
			return ResponseEvent.response(SpecimenListsFolderDetail.from(folder));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenListsFolderDetail> updateFolder(RequestEvent<SpecimenListsFolderDetail> req) {
		try {
			SpecimenListsFolderDetail input = req.getPayload();
			SpecimenListsFolder existing = getFolder(input.getId(), false);
			SpecimenListsFolder folder = folderFactory.createFolder(input);
			existing.update(folder);
			return ResponseEvent.response(SpecimenListsFolderDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimenListsFolderDetail> deleteFolder(RequestEvent<EntityQueryCriteria> req) {
		try {
			SpecimenListsFolder folder = getFolder(req.getPayload().getId(), false);
			folder.delete();
			return ResponseEvent.response(SpecimenListsFolderDetail.from(folder));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> updateFolderCarts(RequestEvent<UpdateFolderCartsOp> req) {
		try {
			UpdateFolderCartsOp op = req.getPayload();
			SpecimenListsFolder folder = getFolder(op.getFolderId(), true);
			return ResponseEvent.response(updateFolderCarts(folder, op.getOp(), op.getCartIds()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimensPickListDetail>> getPickLists(RequestEvent<PickListsCriteria> req) {
		try {
			PickListsCriteria crit = req.getPayload();
			SpecimenList cart = getSpecimenList(crit.cartId(), crit.cartName());
			crit.cartId(cart.getId());

			List<SpecimensPickList> pickLists = daoFactory.getSpecimenListDao().getPickLists(crit);
			if (!crit.includeStat() || pickLists.isEmpty()) {
				return ResponseEvent.response(SpecimensPickListDetail.from(pickLists));
			}

			Map<Long, SpecimensPickListDetail> pickListsMap = new LinkedHashMap<>();
			for (SpecimensPickList pickList : pickLists) {
				pickListsMap.put(pickList.getId(), SpecimensPickListDetail.from(pickList, false));
			}

			Map<Long, Map<String, Long>> counts = daoFactory.getSpecimenListDao().getPickListSpecimensCount(pickListsMap.keySet());
			for (SpecimensPickListDetail detail : pickListsMap.values()) {
				Map<String, Long> count = counts.get(detail.getId());
				detail.setTotalSpecimens(count.get("total"));
				detail.setPickedSpecimens(count.get("picked"));
			}

			return ResponseEvent.response(new ArrayList<>(pickListsMap.values()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimensPickListDetail> getPickList(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			if (crit.getId() == null) {
				return ResponseEvent.userError(SpecimenListErrorCode.PICK_LIST_ID_REQ);
			}

			SpecimenList cart = getSpecimenList(crit.paramLong("cartId"), crit.paramString("cartName"));
			SpecimensPickList pickList = getPickList0(cart, crit.getId());

			SpecimensPickListDetail result = SpecimensPickListDetail.from(pickList);
			Map<String, Long> counts = daoFactory.getSpecimenListDao().getPickListSpecimensCount(pickList.getId());
			result.setPickedSpecimens(counts.getOrDefault("picked", 0L));
			result.setTotalSpecimens(counts.getOrDefault("total", 0L));
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimensPickListDetail> createPickList(RequestEvent<SpecimensPickListDetail> req) {
		try {
			SpecimensPickListDetail input = req.getPayload();
			SpecimenListSummary inputCart = input.getCart();

			if (inputCart == null || (inputCart.getId() == null && StringUtils.isBlank(inputCart.getName()))) {
				return ResponseEvent.userError(SpecimenListErrorCode.NAME_REQUIRED);
			}

			if (StringUtils.isBlank(input.getName())) {
				return ResponseEvent.userError(SpecimenListErrorCode.PICK_LIST_NAME_REQ);
			}

			SpecimenList cart = getSpecimenList(inputCart.getId(), inputCart.getName());
			SpecimensPickList pickList = new SpecimensPickList();
			pickList.setName(input.getName());
			pickList.setCart(cart);
			pickList.setCreator(AuthUtil.getCurrentUser());
			pickList.setCreationTime(Calendar.getInstance().getTime());
			pickList.setTransferToBox(input.getTransferToBox());
			daoFactory.getSpecimenListDao().saveOrUpdate(pickList);
			return ResponseEvent.response(SpecimensPickListDetail.from(pickList));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimensPickListDetail> updatePickList(RequestEvent<SpecimensPickListDetail> req) {
		try {
			SpecimensPickListDetail input = req.getPayload();
			SpecimenListSummary inputCart = input.getCart();
			if (inputCart == null || (inputCart.getId() == null && StringUtils.isBlank(inputCart.getName()))) {
				return ResponseEvent.userError(SpecimenListErrorCode.NAME_REQUIRED);
			}

			if (input.getId() == null) {
				return ResponseEvent.userError(SpecimenListErrorCode.PICK_LIST_ID_REQ);
			}

			if (StringUtils.isBlank(input.getName())) {
				return ResponseEvent.userError(SpecimenListErrorCode.PICK_LIST_NAME_REQ);
			}

			SpecimenList cart = getSpecimenList(inputCart.getId(), inputCart.getName());
			SpecimensPickList pickList = daoFactory.getSpecimenListDao().getPickList(cart.getId(), input.getId());
			if (pickList == null) {
				return ResponseEvent.userError(SpecimenListErrorCode.PICK_LIST_NOT_FOUND, cart.getDisplayName(), input.getId());
			}

			pickList.setName(input.getName());
			pickList.setUpdater(AuthUtil.getCurrentUser());
			pickList.setUpdateTime(Calendar.getInstance().getTime());
			pickList.setTransferToBox(input.getTransferToBox());
			daoFactory.getSpecimenListDao().saveOrUpdate(pickList);
			return ResponseEvent.response(SpecimensPickListDetail.from(pickList));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<SpecimensPickListDetail> deletePickList(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			if (crit.getId() == null) {
				return ResponseEvent.userError(SpecimenListErrorCode.PICK_LIST_ID_REQ);
			}

			SpecimenList cart = getSpecimenList(crit.paramLong("cartId"), crit.paramString("cartName"));
			SpecimensPickList pickList = daoFactory.getSpecimenListDao().getPickList(cart.getId(), crit.getId());
			if (pickList == null) {
				return ResponseEvent.userError(SpecimenListErrorCode.PICK_LIST_NOT_FOUND, cart.getDisplayName(), crit.getId());
			}

			daoFactory.getSpecimenListDao().deletePickList(cart.getId(), pickList.getId());
			return ResponseEvent.response(SpecimensPickListDetail.from(pickList));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<PickListSpecimenDetail>> getPickListSpecimens(RequestEvent<PickListSpecimensCriteria> req) {
		try {
			PickListSpecimensCriteria crit = req.getPayload();
			if (crit.pickListId() == null) {
				return ResponseEvent.userError(SpecimenListErrorCode.PICK_LIST_ID_REQ);
			}

			SpecimenList cart = getSpecimenList(crit.cartId(), crit.cartName());
			SpecimensPickList list = daoFactory.getSpecimenListDao().getPickList(cart.getId(), crit.pickListId());
			if (list == null) {
				return ResponseEvent.userError(SpecimenListErrorCode.PICK_LIST_NOT_FOUND, cart.getDisplayName(), crit.pickListId());
			}

			String filters = StringUtils.EMPTY;
			if (StringUtils.isNotBlank(crit.cp())) {
				filters += " and CollectionProtocol.shortTitle = \"" + crit.cp() + "\"";
			}

			if (StringUtils.isNotBlank(crit.type())) {
				filters += " and Specimen.type = \"" + crit.type() + "\"";
			}

			if (StringUtils.isNotBlank(crit.container())) {
				filters += " and " + String.format(DESCENDENT_CONTAINER_IDS_SQL, crit.container());
			}

			ExecuteQueryEventOp queryOp = new ExecuteQueryEventOp();
			queryOp.setDrivingForm("Participant");
			queryOp.setOutputColumnExprs(true);
			queryOp.setOutputIsoDateTime(true);
			queryOp.setDisableAuditing(true);
			queryOp.setTimeout(-1);
			if (Boolean.TRUE.equals(crit.picked())) {
				queryOp.setAql(String.format(PICKED_SPECIMENS_AQL, list.getId(), filters, crit.startAt(), crit.maxResults()));
			} else {
				queryOp.setAql(String.format(UNPICKED_SPECIMENS_AQL, "\"" + cart.getName() + "\"", list.getId(), filters, crit.startAt(), crit.maxResults()));
			}

			QueryExecResult result = ResponseEvent.unwrap(querySvc.executeQuery(RequestEvent.wrap(queryOp)));
			if (result.getRows() == null || result.getRows().isEmpty()) {
				return ResponseEvent.response(Collections.emptyList());
			}

			ZoneId zoneId = ZoneId.systemDefault();
			if (AuthUtil.getUserTimeZone() != null) {
				zoneId = AuthUtil.getUserTimeZone().toZoneId();
			}

			List<PickListSpecimenDetail> pickListItems = new ArrayList<>();
			for (String[] row : result.getRows()) {
				PickListSpecimenDetail item = new PickListSpecimenDetail();
				SpecimenInfo specimen = new SpecimenInfo();
				item.setSpecimen(specimen);

				StorageLocationSummary location = new StorageLocationSummary();
				UserSummary pickedBy = new UserSummary();

				int idx = -1;
				for (String columnLabel : result.getColumnLabels()) {
					++idx;
					switch (columnLabel) {
						case "CollectionProtocol.id" -> specimen.setCpId(Long.parseLong(row[idx]));
						case "CollectionProtocol.shortTitle" -> specimen.setCpShortTitle(row[idx]);
						case "Specimen.id" -> specimen.setId(Long.parseLong(row[idx]));
						case "Specimen.label" -> specimen.setLabel(row[idx]);
						case "Specimen.barcode" -> specimen.setBarcode(row[idx]);
						case "Specimen.class" -> specimen.setSpecimenClass(row[idx]);
						case "Specimen.type" -> specimen.setType(row[idx]);
						case "Specimen.availableQty" -> specimen.setAvailableQty(StringUtils.isNotBlank(row[idx]) ? new BigDecimal(row[idx]) : null);
						case "Specimen.specimenPosition.containerHierarchy.hierarchy" -> location.setHierarchy(row[idx]);
						case "Specimen.specimenPosition.containerDisplayName" -> location.setDisplayName(row[idx]);
						case "Specimen.specimenPosition.containerName" -> location.setName(row[idx]);
						case "Specimen.specimenPosition.formattedPos" -> location.setFormattedPosition(row[idx]);
						case "Specimen.pickedSpecimens.pickedBy.userId" -> pickedBy.setId(Long.parseLong(row[idx]));
						case "Specimen.pickedSpecimens.pickedBy.firstName" -> pickedBy.setFirstName(row[idx]);
						case "Specimen.pickedSpecimens.pickedBy.lastName" -> pickedBy.setLastName(row[idx]);
						case "Specimen.pickedSpecimens.pickedBy.emailAddress" -> pickedBy.setEmailAddress(row[idx]);
						case "Specimen.pickedSpecimens.pickupTime" -> item.setPickupTime(Date.from(LocalDateTime.parse(row[idx]).atZone(zoneId).toInstant()));
					}
				}

				if (StringUtils.isNotBlank(location.getName())) {
					specimen.setStorageLocation(location);
				}

				if (pickedBy.getId() != null) {
					item.setPickedBy(pickedBy);
				}

				pickListItems.add(item);
			}

			return ResponseEvent.response(pickListItems);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> updatePickListSpecimens(RequestEvent<PickListSpecimensOp> req) {
		try {
			List<SiteCpPair> siteCps = getReadAccessSpecimenSiteCps();

			PickListSpecimensOp input = req.getPayload();
			if (input.getPickListId() == null) {
				throw OpenSpecimenException.userError(SpecimenListErrorCode.PICK_LIST_ID_REQ);
			}

			if (input.getOp() == null) {
				input.setOp(PickListSpecimensOp.Op.PICK);
			}


			SpecimenList cart = getSpecimenList(input.getCartId(), input.getCartName());
			SpecimensPickList pickList = getPickList0(cart, input.getPickListId());

			// pick + transfer to box + boxDetail provided => use pickBoxSpecimens
			if (isBoxTransferPick(pickList, input)) {
				return ResponseEvent.response(Collections.singletonMap("picked", pickBoxSpecimens(input, pickList)));
			}

			if (CollectionUtils.isEmpty(input.getSpecimens())) {
				return emptyPickListOpResponse(input);
			}

			StorageContainer box = resolveBoxIfNeeded(input, pickList);

			SpecimensIndexData idxData = buildSpecimensIndexData(input.getSpecimens());
			if (idxData.indexMap().isEmpty()) {
				return emptyPickListOpResponse(input);
			}

			List<Specimen> specimens = fetchSpecimens(cart.getId(), siteCps, idxData);

			// compute not found and error if any
			List<String> notFound = computeNotFoundSpecimens(idxData, specimens);
			if (!notFound.isEmpty()) {
				throw OpenSpecimenException.userError(SpecimenListErrorCode.INV_CART_SPECIMENS, cart.getDisplayName(), StringUtils.join(notFound, ", "));
			}

			if (specimens.isEmpty()) {
				return emptyPickListOpResponse(input);
			}

			// preserve requested order using indexMap
			specimens.sort(Comparator.comparing(s -> getSpecimenPositionIndex(idxData.indexMap(), s)));

			// do the operation
			List<Long> spmnIds = null;
			if (input.getOp() == PickListSpecimensOp.Op.PICK) {
				spmnIds = handlePick(pickList, specimens, box);
			} else {
				spmnIds = handleUnpick(pickList, specimens);
			}

			// update pickList audit
			pickList.setUpdater(AuthUtil.getCurrentUser());
			pickList.setUpdateTime(Calendar.getInstance().getTime());
			return ResponseEvent.response(Collections.singletonMap(input.getOp().name().toLowerCase() + "ed", spmnIds));
		} catch (OpenSpecimenException ose) {
			return handleNoSpaceError(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public void deleteInactivePickLists() {
		try {
			if (!AuthUtil.isAdmin()) {
				logger.error("Only admin users can cleanup pick lists");
				return;
			}

			int days = ConfigUtil.getInstance().getIntSetting(ConfigParams.MODULE, ConfigParams.INACTIVE_PICK_LIST_LIMIT, 90);
			if (days <= 0) {
				logger.info("Deletion of inactive pick lists is disabled");
				return;
			}

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DAY_OF_MONTH, -days);
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			Date inactiveLimit = cal.getTime();

			cal.add(Calendar.DAY_OF_MONTH, 5);
			Date warningLimit = cal.getTime();

			List<Tuple> inactivePickLists = getInactivePickLists(warningLimit);
			for (Tuple pickList : inactivePickLists) { // {cartId, pickListId, lastAccessTime
				Date lastAccessed = pickList.element(2);
				if (lastAccessed.before(inactiveLimit) || lastAccessed.equals(inactiveLimit)) {
					deletePickList(pickList.element(0), pickList.element(1));
				} else {
					warnPickListDelete(pickList.element(0), pickList.element(1));
				}
			}
		} catch (Exception e) {
			logger.error("Error deleting inactive pick lists", e);
		}
	}

	@Override
	public void afterPropertiesSet()
	throws Exception {
		listSvc.registerListConfigurator("cart-specimens-list-view", this::getListSpecimensConfig);
	}

	@Override
	public QueryDataExportResult exportSpecimenList(SpecimenListCriteria crit, QueryService.ExportProcessor proc) {
		return exportSpecimenList0(crit, proc);
	}

	@Override
	@PlusTransactional
	public boolean toggleStarredSpecimenList(Long listId, boolean starred) {
		try {
			SpecimenList list = getSpecimenList(listId, null);
			if (starred) {
				starredItemSvc.save(getObjectName(), list.getId());
			} else {
				starredItemSvc.delete(getObjectName(), list.getId());
			}

			return true;
		} catch (Exception e) {
			if (e instanceof OpenSpecimenException) {
				throw e;
			}

			throw OpenSpecimenException.serverError(e);
		}
	}

	@Override
	public String getObjectName() {
		return SpecimenList.getEntityName();
	}

	@Override
	public Map<String, Object> resolveUrl(String key, Object value) {
		return Collections.singletonMap("listId", Long.parseLong(value.toString()));
	}

	@Override
	public String getAuditTable() {
		return "CARTS_NOT_AUDITED";
	}

	@Override
	public void ensureReadAllowed(Long cartId) {
		getSpecimenList(cartId, null);
	}

	private SpecimenListsCriteria addSpecimenListsCriteria(SpecimenListsCriteria crit) {
		if (!AuthUtil.isAdmin()) {
			crit.userId(AuthUtil.getCurrentUser().getId());
		}

		return crit;
	}

	private int saveListItems(SpecimenList list, List<Long> specimenIds, boolean newList) {
		if (CollectionUtils.isEmpty(specimenIds)) {
			return 0;
		}

		if (!newList) {
			//
			// we could have obtained only those IDs not in specimen list
			// but then we will be loosing order in which the specimen labels were inputted
			//
			List<Long> idsInList = daoFactory.getSpecimenListDao().getSpecimenIdsInList(list.getId(), specimenIds);
			specimenIds.removeAll(idsInList);
			if (specimenIds.isEmpty()) {
				return 0;
			}
		}

		Date currentTime = Calendar.getInstance().getTime();
		List<SpecimenListItem> items = specimenIds.stream()
			.map(specimenId -> {
				Specimen spmn = new Specimen();
				spmn.setId(specimenId);

				SpecimenListItem item = new SpecimenListItem();
				item.setList(list);
				item.setSpecimen(spmn);
				item.setAddedBy(AuthUtil.getCurrentUser());
				item.setAddedOn(currentTime);
				return item;
			}).collect(Collectors.toList());

		daoFactory.getSpecimenListDao().saveListItems(items);
		return items.size();
	}

	private int deleteListItems(SpecimenList list, List<Long> specimenIds) {
		return daoFactory.getSpecimenListDao().deleteListItems(list.getId(), specimenIds);
	}

	private ResponseEvent<SpecimenListDetail> updateSpecimenList(RequestEvent<SpecimenListDetail> req, boolean partial) {
		try {
			SpecimenListDetail listDetails = req.getPayload();
			SpecimenList existing = getSpecimenList(listDetails.getId(), null);
			UserSummary owner = new UserSummary();
			owner.setId(existing.getOwner().getId());
			listDetails.setOwner(owner);
			
			SpecimenList specimenList = null;
			if (partial) {
				specimenList = specimenListFactory.createSpecimenList(existing, listDetails);
			} else {
				specimenList = specimenListFactory.createSpecimenList(listDetails);
			}
			
			ensureUniqueName(existing, specimenList);
			ensureValidSpecimensAndUsers(listDetails, specimenList, null);

			Collection<User> addedUsers   = CollectionUtils.subtract(specimenList.getAllSharedUsers(), existing.getAllSharedUsers());
			Collection<User> removedUsers = CollectionUtils.subtract(existing.getAllSharedUsers(), specimenList.getAllSharedUsers());

			existing.update(specimenList);
			daoFactory.getSpecimenListDao().saveOrUpdate(existing);
			if (StringUtils.isBlank(existing.getSourceEntityType())) {
				saveListItems(existing, listDetails.getSpecimenIds(), false);
			}

			notifyUsersOnUpdate(existing, addedUsers, removedUsers);
			EventPublisher.getInstance().publish(new SpecimenListSavedEvent(existing, 1));
			return ResponseEvent.response(SpecimenListDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private SpecimenList getSpecimenList(Long listId, String listName) {
		SpecimenList list = null;
		Object key = null;

		if (listId != null) {
			if (listId != 0) {
				list = daoFactory.getSpecimenListDao().getSpecimenList(listId);
			} else {
				list = getDefaultList(AuthUtil.getCurrentUser());
			}
			key = listId;
		} else if (StringUtils.isNotBlank(listName)) {
			list = daoFactory.getSpecimenListDao().getSpecimenListByName(listName);
			key = listName;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(SpecimenListErrorCode.NAME_REQUIRED);
		} else if (list == null) {
			throw OpenSpecimenException.userError(SpecimenListErrorCode.NOT_FOUND, key);
		}

		Long userId = AuthUtil.getCurrentUser().getId();
		if (!AuthUtil.isAdmin() && !list.canUserAccess(userId)) {
			throw OpenSpecimenException.userError(SpecimenListErrorCode.ACCESS_NOT_ALLOWED);
		}

		return list;
	}

	private List<Long> getReadAccessSpecimenIds(List<Long> specimenIds, List<SiteCpPair> siteCps) {
		if (siteCps == null) {
			siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		}

		if (siteCps != null && siteCps.isEmpty()) {
			return Collections.emptyList();
		}

		SpecimenListCriteria crit = new SpecimenListCriteria().ids(specimenIds).siteCps(siteCps);
		return daoFactory.getSpecimenDao().getSpecimenIds(crit);
	}

	private List<Specimen> getReadAccessSpecimens(Long listId, int size) {
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			return Collections.emptyList();
		}

		SpecimenListCriteria crit = new SpecimenListCriteria()
			.specimenListId(listId).siteCps(siteCps)
			.maxResults(size).limitItems(true);
		return daoFactory.getSpecimenDao().getSpecimens(crit);
	}

	private SpecimenListCriteria getListSpecimensCriteria(SpecimenListCriteria crit) {
		//
		// specimen list is retrieved to ensure user has access to the list
		//
		getSpecimenList(crit.specimenListId(), null);

		List<SiteCpPair> siteCpPairs = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCpPairs != null && siteCpPairs.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		return crit.siteCps(siteCpPairs);
	}

	private void ensureValidSpecimensAndUsers(SpecimenListDetail details, SpecimenList specimenList, List<SiteCpPair> siteCpPairs) {
		if (details.isAttrModified("specimenIds")) {
			ensureValidSpecimens(details, siteCpPairs);
		}
		
		if (details.isAttrModified("sharedWith")){
			ensureValidUsers(specimenList);
		}
	}
	
	private void ensureValidSpecimens(SpecimenListDetail details, List<SiteCpPair> siteCpPairs) {
		if (CollectionUtils.isEmpty(details.getSpecimenIds())) {
			return;
		}

		ensureValidSpecimens(details.getSpecimenIds(), siteCpPairs);
	}
	
	private void ensureValidSpecimens(List<Long> specimenIds,  List<SiteCpPair> siteCpPairs) {
		List<Long> dbSpmnIds = getReadAccessSpecimenIds(specimenIds, siteCpPairs);
		if (dbSpmnIds.size() != specimenIds.size()) {
			throw OpenSpecimenException.userError(SpecimenListErrorCode.INVALID_SPECIMENS);
		}
	}

	private void ensureValidUsers(SpecimenList specimenList) {
		if (CollectionUtils.isEmpty(specimenList.getSharedWith()) || AuthUtil.isAdmin()) {
			return;
		}

		List<Long> sharedUsers = specimenList.getSharedWith().stream()
			.filter(user -> !user.equals(specimenList.getOwner()))
			.map(User::getId)
			.collect(Collectors.toList());
		ensureValidUsers(sharedUsers);
	}

	private void ensureValidUsers(List<Long> userIds) {
		List<User> users = daoFactory.getUserDao().getUsersByIds(userIds);
		if (userIds.size() != users.size()) {
			throw OpenSpecimenException.userError(SpecimenListErrorCode.INVALID_USERS_LIST);
		}
	}
	
	private void ensureUniqueName(SpecimenList existingList, SpecimenList newList) {
		if (existingList != null && existingList.getName().equals(newList.getName())) {
			return;
		}
		
		ensureUniqueName(newList);
	}
	
	private void ensureUniqueName(SpecimenList newList) {
		String newListName = newList.getName();

		SpecimenList list = daoFactory.getSpecimenListDao().getSpecimenListByName(newListName);
		if  (list != null) {
			throw OpenSpecimenException.userError(SpecimenListErrorCode.DUP_NAME, newListName);
		}

		if (DEF_LIST_NAME_PATTERN.matcher(newListName).matches()) {
			throw OpenSpecimenException.userError(SpecimenListErrorCode.DUP_NAME, newListName);
		}
	}

	private SpecimenList createDefaultList(User user) {
		return specimenListFactory.createDefaultSpecimenList(user);
	}

	private SpecimenList getDefaultList(User user) {
		SpecimenList specimenList = daoFactory.getSpecimenListDao().getDefaultSpecimenList(user.getId());
		if (specimenList == null) {
			specimenList = createDefaultList(user);
		}

		return specimenList;
	}

	private QueryDataExportResult exportSpecimenList0(SpecimenListCriteria crit, QueryService.ExportProcessor proc) {
		final SpecimenList list = getSpecimenList(crit.specimenListId(), null);

		Integer queryId = ConfigUtil.getInstance().getIntSetting("common", "cart_specimens_rpt_query", -1);
		if (queryId == -1) {
			return null;
		}

		SavedQuery query = deDaoFactory.getSavedQueryDao().getQuery(queryId.longValue());
		if (query == null) {
			throw OpenSpecimenException.userError(SavedQueryErrorCode.NOT_FOUND, queryId);
		}

		String restriction = "Specimen.specimenCarts.name = \"" + list.getName() + "\"";
		if (CollectionUtils.isNotEmpty(crit.ids())) {
			restriction += " and Specimen.id in (" + StringUtils.join(crit.ids(), ",") + ")";
		} else if (CollectionUtils.isNotEmpty(crit.labels())) {
			restriction += " and Specimen.label in (" + StringUtils.join(crit.labels(), ",") + ")";
		}

		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		boolean useMrnSites = AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn();
		String siteCpRestriction = BiospecimenDaoHelper.getInstance().getSiteCpsCondAql(siteCps, useMrnSites);
		if (StringUtils.isNotBlank(siteCpRestriction)) {
			restriction += " and " + siteCpRestriction;
		}

		ExecuteQueryEventOp op = new ExecuteQueryEventOp();
		op.setDrivingForm("Participant");
		op.setAql(query.getAql(restriction, "Specimen.specimenCarts.itemId asc"));
		op.setWideRowMode(WideRowMode.DEEP.name());
		op.setRunType("Export");
		op.setSavedQueryId(query.getId());
		op.setReportName(MessageUtil.getInstance().getMessage("cart_report", new String[] { list.getDisplayName() }));
		return querySvc.exportQueryData(op, Objects.requireNonNullElseGet(proc, () -> new QueryService.ExportProcessor() {
			@Override
			public String filename() {
				return "cart_" + list.getId() + "_" + UUID.randomUUID().toString();
			}

			@Override
			public void headers(OutputStream out) {
				Map<String, String> headers = new LinkedHashMap<>() {{
					put(msg(LIST_NAME), list.isDefaultList() ? msg("specimen_list_default_list", list.getOwner().formattedName()) : list.getName());
					put(msg(LIST_DESC), StringUtils.isNotBlank(list.getDescription()) ? list.getDescription() : msg("common_not_specified"));
				}};

				Utility.writeKeyValuesToCsv(out, headers);
			}
		}));

	}

	private void notifyUsersOnCreate(SpecimenList specimenList, Collection<User> users) {
		notifyUsersOnListOp(specimenList, users, "ADD");
	}

	private void notifyUsersOnUpdate(SpecimenList existing, Collection<User> addedUsers, Collection<User> removedUsers) {
		notifyUsersOnListOp(existing, addedUsers, "ADD");
		notifyUsersOnListOp(existing, removedUsers, "REMOVE");
	}

	private void notifyUsersOnDelete(SpecimenList specimenList) {
		notifyUsersOnListOp(specimenList, specimenList.getAllSharedUsers(), "DELETE");
	}

	private void notifyUsersOnListOp(SpecimenList specimenList, Collection<User> notifyUsers, String op) {
		if (CollectionUtils.isEmpty(notifyUsers)) {
			return;
		}

		String notifMsg = getNotifMsg(specimenList, op);

		// Send email notification
		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("$subject", new String[] { notifMsg });
		emailProps.put("emailText", notifMsg);
		emailProps.put("specimenList", specimenList);
		emailProps.put("currentUser", AuthUtil.getCurrentUser());
		emailProps.put("ccAdmin", false);
		emailProps.put("op", op);

		Set<User> rcpts = new HashSet<>(notifyUsers);
		for (User rcpt : rcpts) {
			emailProps.put("rcpt", rcpt);
			EmailUtil.getInstance().sendEmail(SPECIMEN_LIST_SHARED_TMPL, new String[] { rcpt.getEmailAddress() }, null, emailProps);
		}

		// UI notification
		Notification notif = new Notification();
		notif.setEntityType(SpecimenList.getEntityName());
		notif.setEntityId(specimenList.getId());
		notif.setOperation("UPDATE");
		notif.setCreatedBy(AuthUtil.getCurrentUser());
		notif.setCreationTime(Calendar.getInstance().getTime());
		notif.setMessage(notifMsg);
		NotifUtil.getInstance().notify(notif, Collections.singletonMap("specimen-list", rcpts));
	}

	private String getNotifMsg(SpecimenList specimenList, String op) {
		String msgKey = "specimen_list_user_notif_" + op.toLowerCase();
		return MessageUtil.getInstance().getMessage(msgKey, new String[] { specimenList.getName() });
	}

	private ListConfig getListSpecimensConfig(Map<String, Object> listReq) {
		Number listId = (Number) listReq.get("listId");
		if (listId == null) {
			listId = (Number) listReq.get("objectId");
		}

		SpecimenList list = getSpecimenList(listId != null ? listId.longValue() : null, null);

		ListConfig cfg = ListUtil.getSpecimensListConfig("cart-specimens-list-view", true);
		ListUtil.addHiddenFieldsOfSpecimen(cfg);
		if (cfg == null) {
			return null;
		}

		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(SpecimenListErrorCode.ACCESS_NOT_ALLOWED);
		}

		String restriction = "Specimen.specimenCarts.name = \"" + list.getName() + "\"";

		boolean useMrnSites = AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn();
		String cpSitesCond = BiospecimenDaoHelper.getInstance().getSiteCpsCondAql(siteCps, useMrnSites);
		if (StringUtils.isNotBlank(cpSitesCond)) {
			restriction += " and " + cpSitesCond;
		}

		Column orderBy = new Column();
		orderBy.setExpr("Specimen.specimenCarts.itemId");
		orderBy.setDirection("asc");

		cfg.setDrivingForm("Specimen");
		cfg.setRestriction(restriction);
		cfg.setDistinct(true);
		cfg.setOrderBy(Collections.singletonList(orderBy));
		return ListUtil.setListLimit(cfg, listReq);
	}

	private SpecimenListsFolder getFolder(Long folderId, boolean shared) {
		if (folderId == null) {
			throw OpenSpecimenException.userError(SpecimenListsFolderErrorCode.ID_REQ);
		}

		SpecimenListsFolder folder = daoFactory.getSpecimenListsFolderDao().getById(folderId);
		if (folder == null) {
			throw OpenSpecimenException.userError(SpecimenListsFolderErrorCode.NOT_FOUND, folderId);
		}

		if (AuthUtil.isAdmin() || folder.getOwner().equals(AuthUtil.getCurrentUser())) {
			return folder;
		}

		if (shared && daoFactory.getSpecimenListsFolderDao().isAccessible(folder.getId(), AuthUtil.getCurrentUser().getId())) {
			return folder;
		}

		throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
	}

	private Integer updateFolderCarts(SpecimenListsFolder folder, UpdateFolderCartsOp.Operation operation, List<Long> cartIds) {
		if (CollectionUtils.isEmpty(cartIds)) {
			return 0;
		}

		int count = 0;
		List<Long> accessibleCartIds = cartIds;
		if (!AuthUtil.isAdmin()) {
			accessibleCartIds = daoFactory.getSpecimenListDao().getListsSharedWithUser(cartIds, AuthUtil.getCurrentUser().getId());
		}

		if (accessibleCartIds.size() != cartIds.size()) {
			throw OpenSpecimenException.userError(SpecimenListErrorCode.ACCESS_NOT_ALLOWED);
		}

		List<SpecimenList> carts = daoFactory.getSpecimenListDao().getByIds(accessibleCartIds);
		switch (operation) {
			case ADD -> folder.getLists().addAll(carts);
			case REMOVE -> carts.forEach(folder.getLists()::remove);
		}

		return carts.size();
	}

	private List<Long> pickBoxSpecimens(PickListSpecimensOp input, SpecimensPickList pickList) {
		BoxDetail boxDetail = input.getBoxDetail();
		StorageContainer box = getBox(boxDetail.getId(), boxDetail.getName(), boxDetail.getBarcode(), false);
		boxDetail.setBox(box);


		Map<Long, Specimen> specimens = new LinkedHashMap<>();
		boxDetail.setOnSpecimenStore(
			position -> {
				if (position.getOccupyingSpecimen() != null) {
					specimens.put(position.getOccupyingSpecimen().getId(), position.getOccupyingSpecimen());
				}
			}
		);

		if (box != null && box.getId() != null) {
			ResponseEvent.unwrap(storageContainerSvc.updateBoxSpecimens(RequestEvent.wrap(boxDetail)));
		} else {
			ResponseEvent.unwrap(storageContainerSvc.addBoxSpecimens(RequestEvent.wrap(boxDetail)));
		}

		List<Long> inputSpmnIds = new ArrayList<>(specimens.keySet());
		List<Long> idsInList = daoFactory.getSpecimenListDao().getSpecimenIdsInList(pickList.getCart().getId(), inputSpmnIds);
		Collection<Long> notInList = CollectionUtils.subtract(inputSpmnIds, idsInList);
		if (!notInList.isEmpty()) {
			List<String> notFound = new ArrayList<>();
			for (Long id : notInList) {
				Specimen specimen = specimens.get(id);
				notFound.add(specimen.getLabel() + " (" + specimen.getBarcode() + ")");
			}

			throw OpenSpecimenException.userError(SpecimenListErrorCode.INV_CART_SPECIMENS, pickList.getCart().getDisplayName(), StringUtils.join(notFound, ", "));
		}

		List<Long> alreadyAdded = daoFactory.getSpecimenListDao().getSpecimenIdsInPickList(pickList.getId(), inputSpmnIds);
		return savePickListItems(pickList, specimens.values(), alreadyAdded, AuthUtil.getCurrentUser(), Calendar.getInstance().getTime());
	}

	private List<Long> savePickListItems(SpecimensPickList pickList, Collection<Specimen> specimens, List<Long> alreadyAdded, User user, Date time) {
		List<Long> pickedSpmnIds = new ArrayList<>();
		for (Specimen specimen : specimens) {
			if (!alreadyAdded.contains(specimen.getId())) {
				PickedSpecimen pickedSpecimen = new PickedSpecimen();
				pickedSpecimen.setSpecimen(specimen);
				pickedSpecimen.setPickList(pickList);
				pickedSpecimen.setUpdater(user);
				pickedSpecimen.setUpdateTime(time);
				daoFactory.getSpecimenListDao().savePickListItem(pickedSpecimen);
				pickedSpmnIds.add(specimen.getId());
			}
		}

		return pickedSpmnIds;
	}

	@PlusTransactional
	private List<Tuple> getInactivePickLists(Date cutOffDate) {
		return daoFactory.getSpecimenListDao().getInactivePickLists(cutOffDate);
	}

	@PlusTransactional
	private void deletePickList(Long cartId, Long pickListId) {
		try {
			logger.error("Deleting the pick list: " + pickListId);
			daoFactory.getSpecimenListDao().deletePickList(cartId, pickListId);
		} catch (Exception e) {
			logger.error("Error deleting the pick list: " + pickListId, e);
		}

	}

	@PlusTransactional
	private void warnPickListDelete(Long cartId, Long pickListId) {
		SpecimensPickList pickList = daoFactory.getSpecimenListDao().getPickList(cartId, pickListId);
		Date lastAccessed = pickList.getUpdateTime();
		if (lastAccessed == null) {
			lastAccessed = pickList.getCreationTime();
		}

		int inactiveDaysLimit = ConfigUtil.getInstance().getIntSetting(ConfigParams.MODULE, ConfigParams.INACTIVE_PICK_LIST_LIMIT, 90);
		Calendar cal = Calendar.getInstance();
		cal.setTime(lastAccessed);
		cal.add(Calendar.DAY_OF_MONTH, inactiveDaysLimit);
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);

		Map<String, Object> emailProps = new LinkedHashMap<>();
		emailProps.put("lastAccessTime", Utility.getDateTimeString(lastAccessed));
		emailProps.put("deleteDate", Utility.getDateTimeString(cal.getTime()));
		emailProps.put("pickList", pickList);
		emailProps.put("cart", pickList.getCart());
		emailProps.put("cartName", pickList.getCart().getDisplayName());
		emailProps.put("$subject", new String[] { pickList.getCart().getDisplayName(), pickList.getName() });

		Set<User> rcpts = pickList.getCart().getAllSharedUsers();
		rcpts.add(pickList.getCart().getOwner());

		String[] to = rcpts.stream()
			.filter(rcpt -> StringUtils.isNotBlank(rcpt.getEmailAddress()) && !rcpt.isContact() && !rcpt.isDndEnabled())
			.map(User::getEmailAddress)
			.toList()
			.toArray(new String[0]);
		EmailUtil.getInstance().sendEmail("warn_inactive_pick_list", to, null, emailProps);
	}

	private SpecimensPickList getPickList0(SpecimenList cart, Long pickListId) {
		SpecimensPickList pickList = daoFactory.getSpecimenListDao().getPickList(cart.getId(), pickListId);
		if (pickList == null) {
			throw OpenSpecimenException.userError(SpecimenListErrorCode.PICK_LIST_NOT_FOUND, cart.getDisplayName(), pickListId);
		}

		return pickList;
	}

	private List<SiteCpPair> getReadAccessSpecimenSiteCps() {
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		return siteCps;
	}

	private boolean isBoxTransferPick(SpecimensPickList pickList, PickListSpecimensOp input) {
		return input.getOp() == PickListSpecimensOp.Op.PICK
			&& Boolean.TRUE.equals(pickList.getTransferToBox())
			&& input.getBoxDetail() != null;
	}

	private ResponseEvent<Map<String, Object>> emptyPickListOpResponse(PickListSpecimensOp input) {
		return ResponseEvent.response(Collections.singletonMap(input.getOp().name().toLowerCase() + "ed", Collections.emptyList()));
	}

	private StorageContainer resolveBoxIfNeeded(PickListSpecimensOp input, SpecimensPickList pickList) {
		if (input.getOp() != PickListSpecimensOp.Op.PICK || !Boolean.TRUE.equals(pickList.getTransferToBox())) {
			return null;
		}

		if (StringUtils.isBlank(input.getBoxName())) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NAME_REQUIRED);
		}

		return getBox(null, input.getBoxName(), null, true);
	}

	private record SpecimensIndexData(List<Long> ids, List<String> labels, List<String> barcodes, Map<String, Integer> indexMap) { }

	private SpecimensIndexData buildSpecimensIndexData(List<SpecimenInfo> specimens) {
		Map<String, Integer> indexMap = new HashMap<>();

		List<Long> ids = new ArrayList<>();
		List<String> labels = new ArrayList<>();
		List<String> barcodes = new ArrayList<>();

		int idx = -1;
		for (SpecimenInfo specimen : specimens) {
			++idx;
			if (specimen.getId() != null) {
				ids.add(specimen.getId());
				indexMap.put("id:" + specimen.getId(), idx);
			} else if (StringUtils.isNotBlank(specimen.getLabel())) {
				labels.add(specimen.getLabel());
				indexMap.put("label:" + specimen.getLabel().toLowerCase(), idx);
			} else if (StringUtils.isNotBlank(specimen.getBarcode())) {
				barcodes.add(specimen.getBarcode());
				indexMap.put("barcode:" + specimen.getBarcode().toLowerCase(), idx);
			}
		}

		return new SpecimensIndexData(
			Collections.unmodifiableList(ids),
			Collections.unmodifiableList(labels),
			Collections.unmodifiableList(barcodes),
			Collections.unmodifiableMap(indexMap)
		);
	}

	private List<Specimen> fetchSpecimens(Long cartId, List<SiteCpPair> siteCps, SpecimensIndexData idxData) {
		SpecimenListCriteria crit = new SpecimenListCriteria()
			.ids(idxData.ids())
			.labels(idxData.labels())
			.barcodes(idxData.barcodes())
			.specimenListId(cartId)
			.siteCps(siteCps)
			.exactMatch(true);

		return daoFactory.getSpecimenDao().getSpecimens(crit);
	}

	private List<String> computeNotFoundSpecimens(SpecimensIndexData idxData, List<Specimen> specimens) {
		Set<String> foundIds = specimens.stream().map(s -> String.valueOf(s.getId())).collect(Collectors.toSet());
		Set<String> foundLabels = specimens.stream().map(Specimen::getLabel).filter(Objects::nonNull).map(String::toLowerCase).collect(Collectors.toSet());
		Set<String> foundBarcodes = specimens.stream().map(Specimen::getBarcode).filter(Objects::nonNull).map(String::toLowerCase).collect(Collectors.toSet());

		List<String> notFound = new ArrayList<>();

		notFound.addAll(idxData.ids().stream()
			.map(String::valueOf)
			.filter(id -> !foundIds.contains(id)).toList());

		notFound.addAll(idxData.labels().stream()
			.filter(lbl -> !foundLabels.contains(lbl.toLowerCase())).toList());

		notFound.addAll(idxData.barcodes().stream()
			.filter(bc -> !foundBarcodes.contains(bc.toLowerCase())).toList());

		return notFound;
	}

	private Integer getSpecimenPositionIndex(Map<String, Integer> idxMap, Specimen s1) {
		Integer idx = idxMap.get("id:" + s1.getId());
		if (idx == null) {
			idx = idxMap.get("label:" + s1.getLabel().toLowerCase());
			if (idx == null) {
				idx = idxMap.get("barcode:" + s1.getBarcode().toLowerCase());
			}
		}
		return idx != null ? idx : -1;
	}

	private List<Long> handlePick(SpecimensPickList pickList, List<Specimen> specimens, StorageContainer box) {
		User user = AuthUtil.getCurrentUser();
		Date time = Calendar.getInstance().getTime();

		if (Boolean.TRUE.equals(pickList.getTransferToBox()) && box != null) {
			int idx = -1;
			for (Specimen specimen : specimens) {
				++idx;
				if (!box.canContain(specimen)) {
					throw OpenSpecimenException.userError(StorageContainerErrorCode.CANNOT_HOLD_SPECIMEN, box.getName(), specimen.getLabelOrDesc());
				}

				StorageContainerPosition position = box.nextAvailablePosition();
				if (position == null) {
					ErrorCode errorCode = (idx == 0) ? SpecimenListErrorCode.NO_SPACE_IN_BOX : SpecimenListErrorCode.LTD_SPACE_IN_BOX;
					String message = MessageUtil.getInstance().getMessage(errorCode.code().toLowerCase(), new Object[] { box.getName(), idx });
					throw OpenSpecimenException.userError(errorCode, message);
				}

				specimen.updatePosition(position, user, time, "Picked. " + pickList.getDescription());
			}
		}

		List<Long> spmnIds = specimens.stream().map(BaseEntity::getId).collect(Collectors.toList());
		List<Long> alreadyAdded = daoFactory.getSpecimenListDao().getSpecimenIdsInPickList(pickList.getId(), spmnIds);
		return savePickListItems(pickList, specimens, alreadyAdded, user, time);
	}

	private List<Long> handleUnpick(SpecimensPickList pickList, List<Specimen> specimens) {
		if (Boolean.TRUE.equals(pickList.getTransferToBox())) {
			specimens.forEach(sp -> sp.virtualise("Unpicked. " + pickList.getDescription()));
		}

		List<Long> ids = specimens.stream().map(BaseEntity::getId).collect(Collectors.toList());
		daoFactory.getSpecimenListDao().deleteSpecimensFromPickList(pickList.getId(), ids);
		return ids;
	}

	private StorageContainer getBox(Long id, String name, String barcode, boolean errorOnNotFound) {
		Object key = null;
		StorageContainer container = null;
		if (id != null) {
			container = daoFactory.getStorageContainerDao().getById(id);
			key = id;
		} else if (StringUtils.isNotBlank(name)) {
			container = daoFactory.getStorageContainerDao().getByName(name);
			key = name;
		} else if (StringUtils.isNotBlank(barcode)) {
			container = daoFactory.getStorageContainerDao().getByBarcode(barcode);
			key = barcode;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.ID_NAME_OR_BARCODE_REQ);
		} else if (container == null && errorOnNotFound) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NOT_FOUND, key, 1);
		}

		return container;
	}

	private ResponseEvent<Map<String, Object>> handleNoSpaceError(OpenSpecimenException ose) {
		if (ose.getErrors().size() > 1) {
			return ResponseEvent.error(ose);
		}

		if (ose.containsError(SpecimenListErrorCode.NO_SPACE_IN_BOX) || ose.containsError(SpecimenListErrorCode.LTD_SPACE_IN_BOX)) {
			Map<String, Object> error = new HashMap<>();
			error.put("code", ose.getErrors().get(0).error());
			error.put("message", ose.getErrors().get(0).params()[0]);

			ResponseEvent<Map<String, Object>> resp = ResponseEvent.response(Collections.singletonMap("error", error));
			resp.setRollback(true);
			return resp;
		}

		return ResponseEvent.error(ose);
	}

	private String msg(String code, Object ... params) {
		return MessageUtil.getInstance().getMessage(code, params);
	}

	private static final String LIST_NAME      = "specimen_list_name";

	private static final String LIST_DESC      = "specimen_list_description";

	private static final String SPECIMEN_LIST_SHARED_TMPL = "specimen_list_shared";

	private static final String UNPICKED_SPECIMENS_AQL =
		"select " +
		"  CollectionProtocol.id, CollectionProtocol.shortTitle, Specimen.id, Specimen.label, Specimen.barcode, Specimen.class, Specimen.type, " +
		"  Specimen.availableQty, Specimen.specimenPosition.ancestors.root_container_name, " +
		"  Specimen.specimenPosition.containerHierarchy.hierarchy, Specimen.specimenPosition.containerDisplayName, " +
		"  Specimen.specimenPosition.containerName, Specimen.specimenPosition.formattedPos, " +
		"  Specimen.specimenPosition.rowOrdinal, Specimen.specimenPosition.columnOrdinal " +
		"where " +
		"  Specimen.specimenCarts.name = %s and " +
		"  Specimen.id not in (select Specimen.id where Specimen.pickedSpecimens.pickListId = %d) " +
		"  %s " + // filters
		"order by " +
		"  Specimen.specimenPosition.containerName, Specimen.specimenPosition.rowOrdinal, Specimen.specimenPosition.columnOrdinal " +
		"limit %d, %d";

	private static final String PICKED_SPECIMENS_AQL =
		"select " +
		"  CollectionProtocol.id, CollectionProtocol.shortTitle, Specimen.id, Specimen.label, Specimen.barcode, Specimen.class, Specimen.type, " +
		"  Specimen.availableQty, Specimen.specimenPosition.ancestors.root_container_name, " +
		"  Specimen.specimenPosition.containerHierarchy.hierarchy, Specimen.specimenPosition.containerDisplayName, " +
		"  Specimen.specimenPosition.containerName, Specimen.specimenPosition.formattedPos, " +
		"  Specimen.specimenPosition.rowOrdinal, Specimen.specimenPosition.columnOrdinal, " +
		"  Specimen.pickedSpecimens.pickedBy.userId, Specimen.pickedSpecimens.pickedBy.firstName, " +
		"  Specimen.pickedSpecimens.pickedBy.lastName, Specimen.pickedSpecimens.pickedBy.emailAddress, " +
		"  Specimen.pickedSpecimens.pickupTime " +
		"where " +
		"  Specimen.pickedSpecimens.pickListId = %d " +
		"  %s " + // filters
		"order by " +
		"  Specimen.pickedSpecimens.pickupTime desc " +
		"limit %d, %d";

	private static final String DESCENDENT_CONTAINER_IDS_SQL =
		"Specimen.specimenPosition.containerId in sql(\"select h.descendent_id from os_containers_hierarchy h inner join os_storage_containers c on c.identifier = h.ancestor_id where c.name = '%s'\")";
}
