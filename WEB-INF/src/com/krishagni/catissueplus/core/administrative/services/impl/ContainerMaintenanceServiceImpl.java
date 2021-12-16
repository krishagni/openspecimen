package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;


import com.krishagni.catissueplus.core.administrative.domain.ContainerActivityLog;
import com.krishagni.catissueplus.core.administrative.domain.ContainerTask;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledContainerActivity;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerActivityErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerActivityLogFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerTaskErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerTaskFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.ScheduledContainerActivityFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.events.ContainerActivityLogDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerTaskDetail;
import com.krishagni.catissueplus.core.administrative.events.ScheduledContainerActivityDetail;
import com.krishagni.catissueplus.core.administrative.repository.ContainerActivityLogListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTaskListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.ScheduledContainerActivityListCriteria;
import com.krishagni.catissueplus.core.administrative.services.ContainerMaintenanceService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;

public class ContainerMaintenanceServiceImpl implements ContainerMaintenanceService, InitializingBean {
	private DaoFactory daoFactory;

	private ContainerTaskFactory containerTaskFactory;

	private ScheduledContainerActivityFactory schedContActivityFactory;

	private ContainerActivityLogFactory activityLogFactory;

	private ExportService exportSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setContainerTaskFactory(ContainerTaskFactory containerTaskFactory) {
		this.containerTaskFactory = containerTaskFactory;
	}

	public void setSchedContActivityFactory(ScheduledContainerActivityFactory schedContActivityFactory) {
		this.schedContActivityFactory = schedContActivityFactory;
	}

	public void setActivityLogFactory(ContainerActivityLogFactory activityLogFactory) {
		this.activityLogFactory = activityLogFactory;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ContainerTaskDetail>> getTasks(RequestEvent<ContainerTaskListCriteria> req) {
		try {
			ContainerTaskListCriteria crit = req.getPayload();
			return ResponseEvent.response(ContainerTaskDetail.from(daoFactory.getContainerTaskDao().getTasks(crit)));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> getTasksCount(RequestEvent<ContainerTaskListCriteria> req) {
		try {
			ContainerTaskListCriteria crit = req.getPayload();
			return ResponseEvent.response(daoFactory.getContainerTaskDao().getTasksCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ContainerTaskDetail> getTask(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			ContainerTask task = getTask(crit.getId(), crit.getName());
			return ResponseEvent.response(ContainerTaskDetail.from(task));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ContainerTaskDetail> createTask(RequestEvent<ContainerTaskDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdminOrInstituteAdmin();

			ContainerTaskDetail input = req.getPayload();
			ContainerTask task = containerTaskFactory.createTask(input);
			ensureUniqueTaskName(null, task.getName());
			daoFactory.getContainerTaskDao().saveOrUpdate(task);
			return ResponseEvent.response(ContainerTaskDetail.from(task));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ContainerTaskDetail> updateTask(RequestEvent<ContainerTaskDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdminOrInstituteAdmin();

			ContainerTaskDetail input = req.getPayload();
			ContainerTask existing = getTask(input.getId(), input.getName());
			ContainerTask task = containerTaskFactory.createTask(input);
			ensureUniqueTaskName(existing.getName(), task.getName());
			existing.update(task);
			return ResponseEvent.response(ContainerTaskDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	//
	// container scheduled activities
	//
	@Override
	@PlusTransactional
	public ResponseEvent<List<ScheduledContainerActivityDetail>> getScheduledActivities(RequestEvent<ScheduledContainerActivityListCriteria> req) {
		try {
			ScheduledContainerActivityListCriteria crit = req.getPayload();
			ensureAccessRights(crit.containerId());
			List<ScheduledContainerActivity> activites = daoFactory.getScheduledContainerActivityDao().getActivities(crit);
			return ResponseEvent.response(ScheduledContainerActivityDetail.from(activites));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ScheduledContainerActivityDetail> getScheduledActivity(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			ScheduledContainerActivity activity = getActivity(crit.getId());
			ensureAccessRights(activity.getContainer(), READ);
			return ResponseEvent.response(ScheduledContainerActivityDetail.from(activity));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ScheduledContainerActivityDetail> createScheduledActivity(RequestEvent<ScheduledContainerActivityDetail> req) {
		try {
			ScheduledContainerActivity activity = schedContActivityFactory.createActivity(req.getPayload());
			ensureAccessRights(activity.getContainer(), WRITE);
			ensureUniqueActivityName(activity.getContainer(), null, activity.getName());

			daoFactory.getScheduledContainerActivityDao().saveOrUpdate(activity);
			return ResponseEvent.response(ScheduledContainerActivityDetail.from(activity));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ScheduledContainerActivityDetail> updateScheduledActivity(RequestEvent<ScheduledContainerActivityDetail> req) {
		try {
			ScheduledContainerActivity existing = getActivity(req.getPayload().getId());
			ensureAccessRights(existing.getContainer(), WRITE);

			ScheduledContainerActivity activity = schedContActivityFactory.createActivity(req.getPayload());
			ensureUniqueActivityName(activity.getContainer(), existing.getName(), activity.getName());
			existing.update(activity);
			return ResponseEvent.response(ScheduledContainerActivityDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	//
	// container activity logs
	//
	@Override
	@PlusTransactional
	public ResponseEvent<List<ContainerActivityLogDetail>> getActivityLogs(RequestEvent<ContainerActivityLogListCriteria> req) {
		try {
			ContainerActivityLogListCriteria crit = req.getPayload();
			ensureAccessRights(crit.containerId());
			List<ContainerActivityLog> logs = daoFactory.getContainerActivityLogDao().getActivityLogs(crit);
			return ResponseEvent.response(ContainerActivityLogDetail.from(logs));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ContainerActivityLogDetail> getActivityLog(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			ContainerActivityLog log = daoFactory.getContainerActivityLogDao().getById(crit.getId());
			if (log == null) {
				return ResponseEvent.userError(ContainerActivityErrorCode.NOT_FOUND, crit.getId());
			}

			ensureAccessRights(log.getContainer(), READ);
			return ResponseEvent.response(ContainerActivityLogDetail.from(log));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ContainerActivityLogDetail> createActivityLog(RequestEvent<ContainerActivityLogDetail> req) {
		try {
			ContainerActivityLog log = activityLogFactory.createLog(req.getPayload());
			ensureAccessRights(log.getContainer(), WRITE);
			daoFactory.getContainerActivityLogDao().saveOrUpdate(log);
			return ResponseEvent.response(ContainerActivityLogDetail.from(log));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ContainerActivityLogDetail> updateActivityLog(RequestEvent<ContainerActivityLogDetail> req) {
		try {
			ContainerActivityLogDetail input = req.getPayload();
			ContainerActivityLog existing = daoFactory.getContainerActivityLogDao().getById(input.getId());
			if (existing == null) {
				return ResponseEvent.userError(ContainerActivityErrorCode.NOT_FOUND, input.getId());
			}

			ensureAccessRights(existing.getContainer(), WRITE);
			ContainerActivityLog log = activityLogFactory.createLog(req.getPayload());
			existing.update(log);
			return ResponseEvent.response(ContainerActivityLogDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		exportSvc.registerObjectsGenerator("containerActivityLog", this::getActivityLogsGenerator);
	}

	private void ensureUniqueTaskName(String oldName, String newName) {
		if (oldName != null && oldName.equals(newName)) {
			return;
		}

		ContainerTask existing = daoFactory.getContainerTaskDao().getByName(newName);
		if (existing != null) {
			throw OpenSpecimenException.userError(ContainerTaskErrorCode.DUP_NAME, newName);
		}
	}

	private ContainerTask getTask(Long id, String name) {
		ContainerTask task = null;
		Object key = null;

		if (id != null) {
			task = daoFactory.getContainerTaskDao().getById(id);
			key = id;
		} else if (StringUtils.isNotBlank(name)) {
			task = daoFactory.getContainerTaskDao().getByName(name);
			key = name;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(ContainerTaskErrorCode.NAME_REQ);
		} else if (task == null) {
			throw OpenSpecimenException.userError(ContainerTaskErrorCode.NOT_FOUND, key);
		}

		return task;
	}

	private ScheduledContainerActivity getActivity(Long id) {
		if (id == null) {
			throw OpenSpecimenException.userError(ContainerActivityErrorCode.ID_REQ);
		}

		ScheduledContainerActivity activity = daoFactory.getScheduledContainerActivityDao().getById(id);
		if (activity == null) {
			throw OpenSpecimenException.userError(ContainerActivityErrorCode.NOT_FOUND, id);
		}

		return activity;
	}

	private void ensureUniqueActivityName(StorageContainer container, String oldName, String newName) {
		if (oldName != null && oldName.equals(newName)) {
			return;
		}

		ScheduledContainerActivity existing = daoFactory.getScheduledContainerActivityDao().getActivity(container.getId(), newName);
		if (existing != null) {
			throw OpenSpecimenException.userError(ContainerActivityErrorCode.DUP_NAME, newName, container.getName());
		}
	}

	private void ensureAccessRights(Long containerId) {
		StorageContainer container = daoFactory.getStorageContainerDao().getById(containerId);
		if (container == null) {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.NOT_FOUND, containerId, 1);
		}

		ensureAccessRights(container, READ);
	}

	private void ensureAccessRights(StorageContainer container, int rights) {
		if (rights == READ) {
			AccessCtrlMgr.getInstance().ensureReadContainerRights(container);
		} else {
			AccessCtrlMgr.getInstance().ensureUpdateContainerRights(container);
		}
	}

	private Function<ExportJob, List<? extends Object>> getActivityLogsGenerator() {
		return new Function<ExportJob, List<? extends Object>>() {
			private boolean endOfLogs;

			private ContainerActivityLogListCriteria crit;

			@Override
			public List<? extends Object> apply(ExportJob job) {
				if (crit == null) {
					initParams(job);
				}

				if (endOfLogs) {
					return Collections.emptyList();
				}

				List<ContainerActivityLog> logs = daoFactory.getContainerActivityLogDao().getActivityLogs(crit);
				crit.startAt(crit.startAt() + logs.size());
				endOfLogs = (logs.size() < 100);
				return ContainerActivityLogDetail.from(logs);
			}

			private void initParams(ExportJob job) {
				try {
					String containerIdStr = job.param("containerId");
					if (StringUtils.isBlank(containerIdStr)) {
						throw OpenSpecimenException.userError(StorageContainerErrorCode.ID_REQ);
					}

					Long containerId = Long.parseLong(containerIdStr);
					ensureAccessRights(containerId);

					crit = new ContainerActivityLogListCriteria().containerId(containerId).startAt(0);
				} catch (Exception e) {
					endOfLogs = true;
				}
			}
		};
	}

	private static final int READ = 0;

	private static final int WRITE = 1;
}
