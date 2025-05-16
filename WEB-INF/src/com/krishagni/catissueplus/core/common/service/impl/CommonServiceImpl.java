package com.krishagni.catissueplus.core.common.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTaskManager;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.UnhandledException;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UnhandledExceptionSummary;
import com.krishagni.catissueplus.core.common.repository.UnhandledExceptionListCriteria;
import com.krishagni.catissueplus.core.common.service.CommonService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class CommonServiceImpl implements CommonService, InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(CommonServiceImpl.class);

	private DaoFactory daoFactory;

	private ScheduledTaskManager taskMgr;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setTaskMgr(ScheduledTaskManager taskMgr) {
		this.taskMgr = taskMgr;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<UnhandledExceptionSummary>> getUnhandledExceptions(RequestEvent<UnhandledExceptionListCriteria> req) {
		try {
			User user = AuthUtil.getCurrentUser();
			if (!user.isInstituteAdmin() && !user.isAdmin()) {
				return ResponseEvent.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
			}

			UnhandledExceptionListCriteria listCrit = req.getPayload();
			if (listCrit.fromDate() != null) {
				listCrit.fromDate(Utility.chopTime(listCrit.fromDate()));
			}
			
			if (listCrit.toDate() != null) {
				listCrit.toDate(Utility.getEndOfDay(listCrit.toDate()));
			}

			if (!user.isAdmin()) {
				//
				// must be institute admin; therefore filter exception list based on
				// exceptions received by institute users
				//
				listCrit.instituteId(user.getInstitute().getId());
			}
			
			List<UnhandledException> exceptions = daoFactory.getUnhandledExceptionDao().getUnhandledExceptions(listCrit);
			return ResponseEvent.response(UnhandledExceptionSummary.from(exceptions));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getUnhandledExceptionsCount(RequestEvent<UnhandledExceptionListCriteria> req) {
		try {
			User user = AuthUtil.getCurrentUser();
			if (!user.isInstituteAdmin() && !user.isAdmin()) {
				return ResponseEvent.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
			}

			UnhandledExceptionListCriteria listCrit = req.getPayload();
			if (listCrit.fromDate() != null) {
				listCrit.fromDate(Utility.chopTime(listCrit.fromDate()));
			}

			if (listCrit.toDate() != null) {
				listCrit.toDate(Utility.getEndOfDay(listCrit.toDate()));
			}

			if (!user.isAdmin()) {
				//
				// must be institute admin; therefore filter exception list based on
				// exceptions received by institute users
				//
				listCrit.instituteId(user.getInstitute().getId());
			}

			return ResponseEvent.response(daoFactory.getUnhandledExceptionDao().getUnhandledExceptionsCount(listCrit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<String> getUnhandledExceptionLog(RequestEvent<Long> req) {
		try {
			User user = AuthUtil.getCurrentUser();
			if (!user.isInstituteAdmin() && !user.isAdmin()) {
				throw OpenSpecimenException.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
			}

			UnhandledException exception = daoFactory.getUnhandledExceptionDao().getById(req.getPayload());
			if (exception == null) {
				return ResponseEvent.userError(CommonErrorCode.EXCEPTION_NOT_FOUND, req.getPayload());
			}

			if (!user.isAdmin() && !user.getInstitute().equals(exception.getUser().getInstitute())) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			String log = new StringBuilder()
				.append("Input Arguments: \n").append(exception.getInputArgs()).append("\n\n")
				.append("Stack Trace: \n").append(exception.getStackTrace()).append("\n")
				.toString();
			return ResponseEvent.response(log);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public String getLatestReleaseNotes() {
		try {
			Resource[] resources = new PathMatchingResourcePatternResolver().getResources("/release-notes/*");
			List<File> files = new ArrayList<>();
			for (Resource resource : resources) {
				File file = resource.getFile();
				if (file.isDirectory() || !file.getName().endsWith(".html")) {
					continue;
				}

				files.add(file);
			}

			File file = files.stream()
				.sorted(
					(f1, f2) -> {
						String file1 = f1.getName().substring(1).split(".html")[0];
						String file2 = f2.getName().substring(1).split(".html")[0];
						return -1 * Double.valueOf(file1).compareTo(Double.valueOf(file2));
					}
				)
				.findFirst().orElse(null);
			return file != null ? Utility.getFileText(file) : "No release notes!";
		} catch (Exception e) {
			logger.error("Error reading release notes", e);
			throw new RuntimeException("Error reading release notes", e);
		}
	}

	@Override
	@PlusTransactional
	public void afterPropertiesSet() throws Exception {
		taskMgr.scheduleWithFixedDelay(
			() -> {
				try {
					File tmpDir = new File(ConfigUtil.getInstance().getDataDir(), "tmp");
					if (!tmpDir.exists()) {
						return;
					}

					long currentTime = Calendar.getInstance().getTimeInMillis();
					long olderThanOneDay = currentTime - 24 * 60 * 60 * 1000;
					for (File file : Objects.requireNonNull(tmpDir.listFiles())) {
						if (file.lastModified() < olderThanOneDay) {
							logger.info("Deleting the temporary file: " + file.getAbsolutePath());
							if (!file.delete()) {
								logger.warn("Unable to delete the file: " + file.getAbsolutePath());
							}
						}
					}
				} catch (Throwable t) {
					logger.error("Encountered error when cleaning files from the tmp directory", t);
				}
			},
			24 * 60
		);

		daoFactory.getLockDao().unlockAll();
	}
}
