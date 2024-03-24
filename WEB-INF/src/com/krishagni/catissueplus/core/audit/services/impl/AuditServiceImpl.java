package com.krishagni.catissueplus.core.audit.services.impl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.krishagni.catissueplus.core.administrative.domain.Password;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.domain.factory.AuditErrorCode;
import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.events.AuditEntityQueryCriteria;
import com.krishagni.catissueplus.core.audit.events.FormDataRevisionDetail;
import com.krishagni.catissueplus.core.audit.events.RevisionDetail;
import com.krishagni.catissueplus.core.audit.events.RevisionEntityRecordDetail;
import com.krishagni.catissueplus.core.audit.repository.RevisionsListCriteria;
import com.krishagni.catissueplus.core.audit.services.AuditLogExporter;
import com.krishagni.catissueplus.core.audit.services.AuditService;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.TransactionalThreadLocals;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.ObjectAccessorFactory;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogsListCriteria;
import com.krishagni.catissueplus.core.de.services.impl.QueryAuditLogsExporter;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class AuditServiceImpl implements AuditService, InitializingBean {

	private static final LogUtil logger = LogUtil.getLogger(AuditServiceImpl.class);

	private static final int ONLINE_EXPORT_TIMEOUT_SECS = 30;

	private static final String REV_EMAIL_TMPL = "audit_entity_revisions";

	private SessionFactory sessionFactory;

	private DaoFactory daoFactory;

	private ObjectAccessorFactory objectAccessorFactory;

	private ThreadPoolTaskExecutor taskExecutor;

	private List<AuditLogExporter> exporters = new ArrayList<>();

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setObjectAccessorFactory(ObjectAccessorFactory objectAccessorFactory) {
		this.objectAccessorFactory = objectAccessorFactory;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<AuditDetail>> getEntityAuditDetail(RequestEvent<List<AuditEntityQueryCriteria>> req) {
		return ResponseEvent.response(getEntityAuditDetail(req.getPayload()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<RevisionDetail>> getEntityRevisions(RequestEvent<List<AuditEntityQueryCriteria>> req) {
		List<AuditEntityQueryCriteria> criteria = req.getPayload();
		ensureReadAccess(criteria);

		List<RevisionDetail> revisions = criteria.stream().map(this::getEntityRevisions)
			.flatMap(Collection::stream)
			.collect(Collectors.toList());

		if (criteria.size() > 1) {
			revisions.sort((r1, r2) -> r2.getChangedOn().compareTo(r1.getChangedOn()));
		}

		return ResponseEvent.response(revisions);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ExportedFileDetail> exportRevisions(RequestEvent<RevisionsListCriteria> req) {
		if (!AuthUtil.isAdmin()) {
			return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
		}

		RevisionsListCriteria criteria = req.getPayload();
		if (StringUtils.isNotBlank(criteria.recordType())) {
			criteria.reportTypes(Collections.singletonList("data"));
			if (CollectionUtils.isEmpty(criteria.records())) {
				return ResponseEvent.userError(AuditErrorCode.RECORDS_REQ);
			}
		}

		List<User> users = null;
		if (CollectionUtils.isNotEmpty(criteria.userIds())) {
			users = daoFactory.getUserDao().getByIds(criteria.userIds());

			if (users.size() != criteria.userIds().size()) {
				Set<Long> foundIds = users.stream().map(User::getId).collect(Collectors.toSet());
				String notFoundIds = criteria.userIds().stream()
						.filter(id -> !foundIds.contains(id))
						.map(String::valueOf)
						.collect(Collectors.joining(", "));

				return ResponseEvent.userError(UserErrorCode.ONE_OR_MORE_NOT_FOUND, notFoundIds);
			}
		}

		Date startDate = Utility.chopSeconds(criteria.startDate());
		Date endDate   = Utility.getEndOfDay(criteria.endDate());
		Date endOfDay  = Utility.getEndOfDay(Calendar.getInstance().getTime());
		if (startDate != null && startDate.after(endOfDay)) {
			return ResponseEvent.userError(AuditErrorCode.DATE_GT_TODAY, Utility.getDateTimeString(startDate));
		}

		if (endDate != null && endDate.after(endOfDay)) {
			return ResponseEvent.userError(AuditErrorCode.DATE_GT_TODAY, Utility.getDateTimeString(endDate));
		}

		String recordType = criteria.recordType();
		if (StringUtils.isBlank(recordType) || (!recordType.equals("form") && CollectionUtils.isEmpty(criteria.recordIds()))) {
			int maxLimit = ConfigUtil.getInstance().getIntSetting("common", "max_audit_report_period", 90);
			if (startDate != null && endDate != null) {
				int days = Utility.daysBetween(startDate, endDate);
				if (days > maxLimit) {
					return ResponseEvent.userError(AuditErrorCode.DATE_INTERVAL_GT_ALLOWED, maxLimit);
				}
			} else if (startDate != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(startDate);
				cal.add(Calendar.DAY_OF_MONTH, maxLimit);
				endDate = cal.getTime().after(endOfDay) ? endOfDay : Utility.getEndOfDay(cal.getTime());
			} else {
				endDate = endDate != null ? endDate : endOfDay;

				Calendar cal = Calendar.getInstance();
				cal.setTime(endDate);
				cal.add(Calendar.DAY_OF_MONTH, -maxLimit);
				startDate = Utility.chopTime(cal.getTime());
			}
		}

		if (startDate != null && endDate != null && startDate.after(endDate)) {
			return ResponseEvent.userError(
				AuditErrorCode.FROM_DT_GT_TO_DATE,
				Utility.getDateTimeString(startDate),
				Utility.getDateTimeString(endDate)
			);
		}

		if (criteria.reportTypes() == null || criteria.reportTypes().isEmpty()) {
			criteria.reportTypes(Collections.singletonList("data"));
		}

		criteria.startDate(startDate).endDate(endDate);

		User currentUser     = AuthUtil.getCurrentUser();
		String ipAddress     = AuthUtil.getRemoteAddr();
		List<User> revisionsByUsers = users;

		logger.info("Invoking task executor to initiate export of revisions: " + criteria);

		File revisionsFile = null;
		Future<File> result = taskExecutor.submit(() -> exportRevisions(criteria, currentUser, ipAddress, revisionsByUsers));
		try {
			logger.info("Waiting for the export revisions to finish ...");
			revisionsFile = result.get(ONLINE_EXPORT_TIMEOUT_SECS, TimeUnit.SECONDS);
			logger.info("Export revisions finished within " + ONLINE_EXPORT_TIMEOUT_SECS + " seconds.");
		} catch (TimeoutException te) {
			// timed out waiting for the response
			logger.info("Export revisions is taking more time to finish.");
		} catch (OpenSpecimenException ose) {
			logger.error("Encountered error exporting audit revisions", ose);
			throw ose;
		} catch (Exception ie) {
			logger.error("Encountered error exporting audit revisions", ie);
			throw OpenSpecimenException.serverError(ie);
		}

		return ResponseEvent.response(new ExportedFileDetail(getFileId(revisionsFile), revisionsFile));
	}

	@Override
	public ResponseEvent<File> getExportedRevisionsFile(RequestEvent<String> req) {
		String filename = req.getPayload() + "_" + AuthUtil.getCurrentUser().getId();
		return ResponseEvent.response(new File(getAuditDir(), filename));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<Map<String, String>>> getRevisionEntities() {
		try {
			if (!AuthUtil.isAdmin()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			List<String> entityNames = daoFactory.getAuditDao().getRevisionEntityNames();
			List<Map<String, String>> result = new ArrayList<>();
			for (String entityName : entityNames) {
				Map<String, String> entity = new HashMap<>();
				entity.put("name", entityName);
				entity.put("value", MessageUtil.getInstance().getMessage("audit_entity_" + entityName));
				result.add(entity);
			}

			return ResponseEvent.response(result.stream().sorted(Comparator.comparing(e -> e.get("value"))).collect(Collectors.toList()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public void insertApiCallLog(UserApiCallLog userAuditLog) {
		daoFactory.getAuditDao().saveOrUpdate(userAuditLog);
	}

	@Override
	@PlusTransactional
	public long getTimeSinceLastApiCall(Long userId, String token) {
		Date lastApiCallTime = daoFactory.getAuditDao().getLatestApiCallTime(userId, token);
		long timeSinceLastApiCallInMilli = Calendar.getInstance().getTime().getTime() - lastApiCallTime.getTime();
		return TimeUnit.MILLISECONDS.toMinutes(timeSinceLastApiCallInMilli);
	}

	@Override
	public void registerExporter(AuditLogExporter exporter) {
		exporters.add(exporter);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		EventListenerRegistry reg = ((SessionFactoryImpl) sessionFactory).getServiceRegistry().getService(EventListenerRegistry.class);

		PersistEventListener listener = new PersistEventListener();
		reg.getEventListenerGroup(EventType.PRE_INSERT).appendListener(listener);
		reg.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(listener);
		reg.getEventListenerGroup(EventType.PRE_DELETE).appendListener(listener);
		reg.getEventListenerGroup(EventType.PRE_COLLECTION_RECREATE).appendListener(listener);
		reg.getEventListenerGroup(EventType.PRE_COLLECTION_UPDATE).appendListener(listener);
		reg.getEventListenerGroup(EventType.PRE_COLLECTION_REMOVE).appendListener(listener);
	}

	private List<AuditDetail> getEntityAuditDetail(List<AuditEntityQueryCriteria> criteria) {
		ensureReadAccess(criteria);
		return criteria.stream().map(this::getEntityAuditDetail).collect(Collectors.toList());
	}

	private void ensureReadAccess(List<AuditEntityQueryCriteria> criteria) {
		for (AuditEntityQueryCriteria crit : criteria) {
			ObjectAccessor accessor = objectAccessorFactory.getAccessor(crit.getObjectName());
			if (accessor == null) {
				throw OpenSpecimenException.userError(AuditErrorCode.ENTITY_NOT_FOUND, crit.getObjectName());
			}

			accessor.ensureReadAllowed(crit.getObjectId());
		}
	}

	private AuditDetail getEntityAuditDetail(AuditEntityQueryCriteria crit) {
		ObjectAccessor accessor = objectAccessorFactory.getAccessor(crit.getObjectName());
		return daoFactory.getAuditDao().getAuditDetail(accessor.getAuditTable(), crit.getObjectId());
	}

	private List<RevisionDetail> getEntityRevisions(AuditEntityQueryCriteria crit) {
		ObjectAccessor accessor = objectAccessorFactory.getAccessor(crit.getObjectName());
		return daoFactory.getAuditDao().getRevisions(accessor.getAuditTable(), crit.getObjectId());
	}

	@PlusTransactional
	private File exportRevisions(RevisionsListCriteria criteria, User exportedBy, String ipAddress, List<User> revisionsBy) {
		try {
			Date startTime = Calendar.getInstance().getTime();
			List<File> inputFiles = new ArrayList<>();
			AuthUtil.setCurrentUser(exportedBy, ipAddress);

			String baseDir = UUID.randomUUID().toString();
			Date exportedOn = Calendar.getInstance().getTime();

			if (criteria.includeReport("data")) {
				if (StringUtils.isBlank(criteria.recordType()) || criteria.recordType().equals("core")) {
					logger.info("Exporting core objects' revisions... ");
					File coreObjectsRevs = new CoreObjectsRevisionExporter(criteria, exportedBy, exportedOn, revisionsBy).export(baseDir);
					inputFiles.add(coreObjectsRevs);
				}

				FormsRevisionExporter formsExporter = new FormsRevisionExporter(criteria, exportedBy, exportedOn, revisionsBy);
				if (StringUtils.isBlank(criteria.recordType()) || criteria.recordType().equals("form")) {
					logger.info("Exporting form revisions... ");
					File formsRevs = formsExporter.exportMetadataRevisions(baseDir);
					inputFiles.add(formsRevs);
				}

				if (StringUtils.isBlank(criteria.recordType()) || criteria.recordType().equals("form_data")) {
					logger.info("Exporting form data revisions... ");
					File formsDataRevs = formsExporter.exportDataRevisions(baseDir);
					inputFiles.add(formsDataRevs);
				}
			}

			if (criteria.includeReport("auth")) {
				logger.info("Export user password revisions... ");
				File passwordRevs    = exportPasswordsData(criteria, exportedBy, exportedOn, revisionsBy, baseDir);
				inputFiles.add(passwordRevs);
			}

			if (criteria.includeReport("query_exim")) {
				logger.info("Export query run logs...");
				File queryRuns = exportQueryAudit(criteria, exportedBy, exportedOn, revisionsBy, baseDir);
				inputFiles.add(queryRuns);
			}

			if (criteria.includeReport("api_calls")) {
				logger.info("Export user API call logs...");
				File apiLogs = exportApiCallsLog(criteria, exportedBy, exportedOn, revisionsBy, baseDir);
				inputFiles.add(apiLogs);
			}

			for (AuditLogExporter exporter : exporters) {
				File reportFile = exporter.export(baseDir, exportedBy, exportedOn, revisionsBy, criteria);
				if (reportFile != null) {
					inputFiles.add(reportFile);
				}
			}

			logger.info("Creating revisions ZIP file... ");
			File result = new File(getAuditDir(), baseDir + "_" + getTs(exportedOn) + "_" + exportedBy.getId());
			Utility.zipFiles(inputFiles, result);

			logger.info("Cleaning up revisions directory: " + baseDir);
			cleanupRevisionsDir(baseDir);

			logger.info("Sending audit revisions email to " + exportedBy.formattedName() + " (" + exportedBy.getEmailAddress() + ")");
			sendEmailNotif(criteria, exportedBy, revisionsBy, result);

			ExportService exportSvc = OpenSpecimenAppCtxProvider.getBean("exportSvc");
			exportSvc.saveJob("audit_report", startTime, criteria.toStrMap());
			return result;
		} finally {
			AuthUtil.clearCurrentUser();
		}
	}

	private File getAuditDir() {
		return new File(ConfigUtil.getInstance().getDataDir(), "audit");
	}

	private File getAuditDir(String dir) {
		File result = new File(getAuditDir(), dir);
		if (!result.exists()) {
			result.mkdirs();
		}

		return result;
	}

	private void cleanupRevisionsDir(String baseDir) {
		File dir = getAuditDir(baseDir);
		for (File file : dir.listFiles()) {
			file.delete();
		}

		dir.delete();
	}

	private class CoreObjectsRevisionExporter {
		private RevisionsListCriteria criteria;

		private User exportedBy;

		private Date exportedOn;

		private List<User> revisionsBy;

		public CoreObjectsRevisionExporter(RevisionsListCriteria criteria, User exportedBy, Date exportedOn, List<User> revisionsBy) {
			this.criteria = criteria;
			this.exportedBy = exportedBy;
			this.exportedOn = exportedOn;
			this.revisionsBy = revisionsBy;
		}

		public File export(String dir) {
			long startTime = System.currentTimeMillis();
			CsvFileWriter csvWriter = null;

			try {
				File outputFile = getOutputFile(dir);
				csvWriter = CsvFileWriter.createCsvFileWriter(outputFile);

				writeHeader(criteria, exportedBy, exportedOn, revisionsBy, csvWriter);

				long lastRecId = 0, totalRecords = 0, lastChunk = 0;
				Map<String, String> context = new HashMap<>();
				while (true) {
					long t1 = System.currentTimeMillis();
					List<RevisionDetail> revisions = daoFactory.getAuditDao().getRevisions(criteria);
					if (revisions.isEmpty()) {
						logger.debug("No revisions for the criteria: " + criteria);
						break;
					}

					if (logger.isDebugEnabled()) {
						logger.debug(revisions.size() + " revisions fetched for the criteria: " + criteria);
					}

					for (RevisionDetail revision : revisions) {
						totalRecords += writeRows(context, revision, csvWriter);
						lastRecId = revision.getLastRecordId();

						long currentChunk = totalRecords / 25;
						if (currentChunk != lastChunk) {
							csvWriter.flush();
							lastChunk = currentChunk;
							logger.debug("Flushed the revision rows to file");
						}
					}

					criteria.lastId(lastRecId);
				}

				csvWriter.flush();
				return outputFile;
			} catch (Exception e) {
				logger.error("Error exporting core objects' revisions", e);
				throw OpenSpecimenException.serverError(e);
			} finally {
				IOUtils.closeQuietly(csvWriter);
				logger.info("Core objects' revisions export finished in " +  (System.currentTimeMillis() - startTime) + " ms");
			}
		}

		private File getOutputFile(String dir) {
			return new File(getAuditDir(dir), "os_core_objects_revisions_" + getTs(exportedOn) + ".csv");
		}

		//
		// Row format
		// revision number, rev date, user, rev type, entity name, entity id
		//
		private int writeRows(Map<String, String> context, RevisionDetail revision, CsvWriter writer)
			throws IOException {
			String revId     = revision.getRevisionId().toString();
			String dateTime  = Utility.getDateTimeString(revision.getChangedOn());

			String user      = null;
			String userEmail = null;
			String domain    = null;
			String userLogin = null;
			String institute = null;
			if (revision.getChangedBy() != null) {
				user      = revision.getChangedBy().formattedName();
				userEmail = revision.getChangedBy().getEmailAddress();
				domain    = revision.getChangedBy().getDomain();
				userLogin = revision.getChangedBy().getLoginName();
				institute = revision.getChangedBy().getInstituteName();
			}

			Function<String, String> toMsg = AuditServiceImpl.this::toMsg;
			int recsCount = 0;

			for (RevisionEntityRecordDetail record : revision.getRecords()) {
				if (criteria.includeModifiedProps() && StringUtils.isBlank(record.getModifiedProps()) && record.getType() != 2) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring the revision record: " + record.toString());
					}

					continue;
				}

				String op = null;
				switch (record.getType()) {
					case 0:
						op = "audit_op_insert";
						break;

					case 1:
						op = "audit_op_update";
						break;

					case 2:
						op = "audit_op_delete";
						break;
				}

				String opDisplay  = context.computeIfAbsent(op, toMsg);
				String entityName = context.computeIfAbsent("audit_entity_" + record.getEntityName(), toMsg);
				String entityId   = record.getEntityId().toString();
				String ipAddress  = revision.getIpAddress();
				String[] line     = {revId, dateTime, user, userEmail, domain, userLogin, institute, ipAddress, opDisplay, entityName, entityId, record.getModifiedProps()};

				writer.writeNext(line);
				if (logger.isDebugEnabled()) {
					logger.debug("Row " + recsCount + ": " + String.join(",", line));
				}

				++recsCount;
				if (recsCount % 25 == 0) {
					writer.flush();
					logger.debug("Flushed the revision rows to file");
				}
			}

			return recsCount;
		}
	}

	private class FormsRevisionExporter {
		private RevisionsListCriteria criteria;

		private User exportedBy;

		private Date exportedOn;

		private List<User> revisionsBy;

		public FormsRevisionExporter(RevisionsListCriteria criteria, User exportedBy, Date exportedOn, List<User> revisionsBy) {
			this.criteria = criteria;
			this.exportedBy = exportedBy;
			this.exportedOn = exportedOn;
			this.revisionsBy = revisionsBy;
		}

		public File exportMetadataRevisions(String dir) {
			long startTime = System.currentTimeMillis();
			CsvFileWriter csvWriter = null;

			try {
				File outputFile = getOutputFile(dir, "os_forms_revisions", this.exportedOn);
				csvWriter = CsvFileWriter.createCsvFileWriter(outputFile);

				String[] headerColumns = {
					"audit_rev_id", "audit_rev_tstmp", "audit_rev_user", "audit_rev_user_email",
					"audit_rev_domain", "audit_rev_user_login", "audit_rev_institute", "audit_rev_ip_address",
					"audit_rev_entity_op", "audit_rev_form_name", "audit_rev_entity_id", "audit_rev_change_log"
				};
				writeHeader(csvWriter, headerColumns);

				long lastRevId = 0, totalRecords = 0;
				Map<String, String> context = new HashMap<>();

				criteria.lastId(null);
				while (true) {
					List<FormDataRevisionDetail> revisions = daoFactory.getAuditDao().getFormRevisions(criteria);
					if (revisions.isEmpty()) {
						break;
					}

					for (FormDataRevisionDetail revision : revisions) {
						writeRow(context, revision, csvWriter);
						lastRevId = revision.getId();
						++totalRecords;

						if (totalRecords % 25 == 0) {
							csvWriter.flush();
						}
					}

					criteria.lastId(lastRevId);
				}

				csvWriter.flush();
				return outputFile;
			} catch (Exception e) {
				logger.error("Error exporting forms revisions", e);
				throw OpenSpecimenException.serverError(e);
			} finally {
				IOUtils.closeQuietly(csvWriter);
				logger.info("Forms revisions export finished in " +  (System.currentTimeMillis() - startTime) + " ms");
			}
		}

		public File exportDataRevisions(String dir) {
			long startTime = System.currentTimeMillis();
			CsvFileWriter csvWriter = null;

			try {
				File outputFile = getOutputFile(dir, "os_forms_data_revisions", this.exportedOn);
				csvWriter = CsvFileWriter.createCsvFileWriter(outputFile);

				String[] headerColumns = {
					"audit_rev_id", "audit_rev_tstmp", "audit_rev_user", "audit_rev_user_email",
					"audit_rev_domain", "audit_rev_user_login", "audit_rev_institute", "audit_rev_ip_address",
					"audit_rev_entity_op", "audit_rev_entity_name", "audit_rev_form_name",
					"audit_rev_parent_entity_id", "audit_rev_entity_id", "audit_rev_change_log"
				};
				writeHeader(csvWriter, headerColumns);

				long lastRevId = 0, totalRecords = 0;
				Map<String, String> context = new HashMap<>();

				criteria.lastId(null);
				while (true) {
					long t1 = System.currentTimeMillis();

					List<FormDataRevisionDetail> revisions = daoFactory.getAuditDao().getFormDataRevisions(criteria);
					System.err.println(criteria.lastId() + ", " + (System.currentTimeMillis() - t1) + " ms");

					if (revisions.isEmpty()) {
						break;
					}

					for (FormDataRevisionDetail revision : revisions) {
						writeRow(context, revision, csvWriter);
						lastRevId = revision.getId();
						++totalRecords;

						if (totalRecords % 25 == 0) {
							csvWriter.flush();
						}
					}

					criteria.lastId(lastRevId);
				}

				csvWriter.flush();
				return outputFile;
			} catch (Exception e) {
				logger.error("Error exporting forms data revisions", e);
				throw OpenSpecimenException.serverError(e);
			} finally {
				IOUtils.closeQuietly(csvWriter);
				logger.info("Forms data revisions export finished in " +  (System.currentTimeMillis() - startTime) + " ms");
			}
		}

		private void writeHeader(CsvWriter writer, String[] headerColumns) {
			writeExportHeader(writer, criteria, exportedBy, exportedOn, revisionsBy);
			writer.writeNext(Stream.of(headerColumns).map(MessageUtil.getInstance()::getMessage).toArray(String[]::new));
		}

		private void writeRow(Map<String, String> context, FormDataRevisionDetail revision, CsvWriter writer) {
			String revId     = revision.getId().toString();
			String dateTime  = Utility.getDateTimeString(revision.getTime());

			String user      = null;
			String userEmail = null;
			String domain    = null;
			String userLogin = null;
			String institute = null;
			if (revision.getUser() != null) {
				user      = revision.getUser().formattedName();
				userEmail = revision.getUser().getEmailAddress();
				domain    = revision.getUser().getDomain();
				userLogin = revision.getUser().getLoginName();
				institute = revision.getUser().getInstituteName();
			}

			Function<String, String> toMsg = AuditServiceImpl.this::toMsg;

			String op = "audit_op_" + revision.getOp().toLowerCase();
			String opDisplay = context.computeIfAbsent(op, toMsg);

			String entityType = null;
			String entityId = revision.getEntityId() != null ? revision.getEntityId().toString() : null;
			if (StringUtils.isNotBlank(revision.getEntityType())) {
				entityType = context.computeIfAbsent("form_entity_" + revision.getEntityType(), toMsg);
			}

			String formName = revision.getFormName();
			String recordId = revision.getRecordId().toString();
			String ipAddress = revision.getIpAddress();
			String data = revision.getData();

			if (entityType != null) {
				writer.writeNext(new String[] {revId, dateTime, user, userEmail, domain, userLogin, institute, ipAddress, opDisplay, entityType, formName, entityId, recordId, data});
			} else {
				writer.writeNext(new String[] {revId, dateTime, user, userEmail, domain, userLogin, institute, ipAddress, opDisplay, formName, recordId, data});
			}
		}
	}

	private File exportPasswordsData(RevisionsListCriteria criteria, User exportedBy, Date exportedOn, List<User> revisionsBy, String baseDir) {
		long startTime = System.currentTimeMillis();
		CsvFileWriter csvWriter = null;

		try {
			File outputFile = getOutputFile(baseDir, "os_password_revisions", exportedOn);
			csvWriter = CsvFileWriter.createCsvFileWriter(outputFile);
			writeHeader(criteria, exportedBy, exportedOn, revisionsBy, csvWriter);

			String editOp = MessageUtil.getInstance().getMessage("audit_op_update");
			String entity = MessageUtil.getInstance().getMessage("audit_entity_" + Password.class.getName());
			long lastRevId = Long.MAX_VALUE, totalRecords = 0;
			while (true) {
				long t1 = System.currentTimeMillis();

				List<Password> passwords = daoFactory.getUserDao()
					.getPasswords(criteria.startDate(), criteria.endDate(), lastRevId, revisionsBy);
				if (passwords.isEmpty()) {
					break;
				}

				for (Password password : passwords) {
					String user      = null;
					String userEmail = null;
					String userLogin = null;
					String institute = null;
					String domain    = null;
					if (password.getUpdatedBy() != null) {
						user      = password.getUpdatedBy().formattedName();
						userEmail = password.getUpdatedBy().getEmailAddress();
						userLogin = password.getUpdatedBy().getLoginName();
						institute = password.getUpdatedBy().getInstitute().getName();
						domain    = password.getUpdatedBy().getAuthDomain().getName();
					}

					csvWriter.writeNext(new String[] {
						password.getId().toString(),
						Utility.getDateTimeString(password.getUpdationDate()),
						user, userEmail, domain, userLogin, institute, password.getIpAddress(),
						editOp, entity, password.getUser().getId().toString(), "###"
					});

					lastRevId = password.getId();
					++totalRecords;
					if (totalRecords % 25 == 0) {
						csvWriter.flush();
					}
				}
			}

			csvWriter.flush();
			return outputFile;
		} catch (Exception e) {
			logger.error("Error exporting password revisions", e);
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(csvWriter);
			logger.info("Password revisions export finished in " +  (System.currentTimeMillis() - startTime) + " ms");
		}
	}

	private File exportQueryAudit(RevisionsListCriteria criteria, User exportedBy, Date exportedOn, List<User> revisionsBy, String baseDir) {
		File outputFile = getOutputFile(baseDir, "os_query_audit_logs", exportedOn);

		QueryAuditLogsListCriteria crit = new QueryAuditLogsListCriteria()
			.startDate(criteria.startDate())
			.endDate(criteria.endDate())
			.asc(false)
			.userIds(Utility.nullSafeStream(revisionsBy).map(User::getId).collect(Collectors.toList()));
		return new QueryAuditLogsExporter(crit, exportedBy, revisionsBy).exportAuditLogs(outputFile, exportedOn);
	}

	private File exportApiCallsLog(RevisionsListCriteria criteria, User exportedBy, Date exportedOn, List<User> callsBy, String baseDir) {
		long startTime = System.currentTimeMillis();
		CsvFileWriter csvWriter = null;

		try {
			File outputFile = getOutputFile(baseDir, "os_user_api_call_logs", exportedOn);
			csvWriter = CsvFileWriter.createCsvFileWriter(outputFile);
			writeExportHeader(csvWriter, criteria, exportedBy, exportedOn, callsBy);

			String[] headerColumns = {
				"audit_rev_id", "audit_start_time", "audit_end_time", "audit_rev_user", "audit_rev_user_email",
				"audit_rev_domain", "audit_rev_user_login", "audit_rev_institute", "audit_rev_ip_address",
				"audit_call_method", "audit_call_url", "audit_call_resp_code"
			};
			csvWriter.writeNext(Stream.of(headerColumns).map(MessageUtil.getInstance()::getMessage).toArray(String[]::new));

			long lastRevId = Long.MAX_VALUE, totalRecords = 0;
			while (true) {
				long t1 = System.currentTimeMillis();

				List<UserApiCallLog> callLogs = daoFactory.getAuditDao().getApiCallLogs(criteria.lastId(lastRevId));
				if (callLogs.isEmpty()) {
					break;
				}

				for (UserApiCallLog callLog : callLogs) {
					String user      = null;
					String userEmail = null;
					String userLogin = null;
					String institute = null;
					String domain    = null;
					if (callLog.getUser() != null) {
						user      = callLog.getUser().formattedName();
						userEmail = callLog.getUser().getEmailAddress();
						userLogin = callLog.getUser().getLoginName();
						institute = callLog.getUser().getInstitute().getName();
						domain    = callLog.getUser().getAuthDomain().getName();
					}

					String ipAddres = null;
					if (callLog.getLoginAuditLog() != null) {
						ipAddres = callLog.getLoginAuditLog().getIpAddress();
					}

					csvWriter.writeNext(new String[] {
						callLog.getId().toString(),
						Utility.getDateTimeString(callLog.getCallStartTime()),
						Utility.getDateTimeString(callLog.getCallEndTime()),
						user, userEmail, domain, userLogin, institute, ipAddres,
						callLog.getMethod(), callLog.getUrl(), callLog.getResponseCode()
					});

					lastRevId = callLog.getId();
					++totalRecords;
					if (totalRecords % 25 == 0) {
						csvWriter.flush();
					}
				}
			}

			csvWriter.flush();
			return outputFile;
		} catch (Exception e) {
			logger.error("Error exporting user API call logs", e);
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(csvWriter);
			logger.info("User API call logs export finished in " +  (System.currentTimeMillis() - startTime) + " ms");
		}
	}

	private File getOutputFile(String dir, String filename, Date exportedOn) {
		return new File(getAuditDir(dir), filename + "_" + getTs(exportedOn) + ".csv");
	}

	private void writeHeader(RevisionsListCriteria criteria, User exportedBy, Date exportedOn, List<User> revisionsBy, CsvWriter writer) {
		writeExportHeader(writer, criteria, exportedBy, exportedOn, revisionsBy);

		String[] keys = {
			"audit_rev_id", "audit_rev_tstmp", "audit_rev_user", "audit_rev_user_email",
			"audit_rev_domain", "audit_rev_user_login", "audit_rev_institute", "audit_rev_ip_address",
			"audit_rev_entity_op", "audit_rev_entity_name", "audit_rev_entity_id",
			"audit_rev_change_log"
		};
		writer.writeNext(Stream.of(keys).map(MessageUtil.getInstance()::getMessage).toArray(String[]::new));
	}

	private void sendEmailNotif(RevisionsListCriteria criteria, User exportedBy, List<User> revsBy, File revisionsFile) {
		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("startDate", getDateTimeString(criteria.startDate()));
		emailProps.put("endDate",   getDateTimeString(criteria.endDate()));
		String userNames = Utility.nullSafeStream(revsBy).map(User::formattedName).collect(Collectors.joining(", "));
		emailProps.put("users",     !userNames.isEmpty() ? userNames : null);
		emailProps.put("fileId",    getFileId(revisionsFile));
		emailProps.put("rcpt",      exportedBy.formattedName());
		emailProps.put("reportTypes", criteria.reportTypes().stream().map(t -> msg("audit_report_type_" + t)).collect(Collectors.joining(", ")));
		emailProps.put("recordType", StringUtils.isNotBlank(criteria.recordType()) ? msg("audit_record_type_" + criteria.recordType()) : null);
		if ("core".equals(criteria.recordType())) {
			emailProps.put("records", Utility.nullSafeStream(criteria.records()).map(t -> msg("audit_entity_" + t)).collect(Collectors.joining(", ")));
		} else {
			emailProps.put("records", StringUtils.join(criteria.records(), ", "));
		}

		if (CollectionUtils.isNotEmpty(criteria.recordIds())) {
			emailProps.put("recordIds", StringUtils.join(criteria.recordIds(), ", "));
		}

		EmailUtil.getInstance().sendEmail(
			REV_EMAIL_TMPL,
			new String[] { exportedBy.getEmailAddress() },
			null,
			emailProps
		);
	}

	private String msg(String key) {
		return MessageUtil.getInstance().getMessage(key);
	}

	private String getFileId(File revisionsFile) {
		if (revisionsFile == null) {
			return null;
		}

		return revisionsFile.getName().substring(0, revisionsFile.getName().lastIndexOf("_"));
	}

	private String getDateTimeString(Date dt) {
		return dt != null ? Utility.getDateTimeString(dt) : null;
	}

	private void writeExportHeader(CsvWriter writer, RevisionsListCriteria criteria, User exportedBy, Date exportedOn, List<User> revisionUsers) {
		writeRow(writer, toMsg("common_server_url"), ConfigUtil.getInstance().getAppUrl());
		writeRow(writer, toMsg("common_server_env"), ConfigUtil.getInstance().getDeployEnv());
		writeRow(writer, toMsg("audit_rev_exported_by"), exportedBy.formattedName(true));
		writeRow(writer, toMsg("audit_rev_exported_on"), getDateTimeString(exportedOn));

		if (CollectionUtils.isNotEmpty(revisionUsers)) {
			List<String> userNames = revisionUsers.stream().map(u -> u.formattedName(true)).collect(Collectors.toList());
			userNames.add(0, toMsg("audit_rev_audited_users"));
			writeRow(writer, userNames.toArray(new String[0]));
		}

		if (criteria.startDate() != null) {
			writeRow(writer, toMsg("audit_rev_start_date"), getDateTimeString(criteria.startDate()));
		}

		if (criteria.endDate() != null) {
			writeRow(writer, toMsg("audit_rev_end_date"), getDateTimeString(criteria.endDate()));
		}

		writeRow(writer, "");
	}

	private void writeRow(CsvWriter writer, String ... columns) {
		writer.writeNext(columns);
	}

	private String toMsg(String key) {
		try {
			int idx = key.indexOf("-");
			if (idx > 0) {
				key = key.substring(0, idx);
			}

			return MessageUtil.getInstance().getMessage(key);
		} catch (Exception e) {
			return key;
		}
	}

	private String getTs(Date date) {
		return new SimpleDateFormat("yyyyMMdd_HHmm").format(date);
	}

	private class PersistEventListener
		implements PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener,
		PreCollectionRecreateEventListener, PreCollectionUpdateEventListener, PreCollectionRemoveEventListener {

		private ThreadLocal<IdentityHashMap<BaseEntity, Boolean>> entities =
			new ThreadLocal<IdentityHashMap<BaseEntity, Boolean>>() {
				@Override
				protected IdentityHashMap<BaseEntity, Boolean> initialValue() {
					TransactionalThreadLocals.getInstance().register(this);
					return new IdentityHashMap<>();
				}
			};

		@Override
		public boolean onPreInsert(PreInsertEvent event) {
			if (!(event.getEntity() instanceof BaseEntity)) {
				return false;
			}

			BaseEntity entity = (BaseEntity) event.getEntity();
			boolean update = false;
			if (entity.getRoot() != null) {
				entity = entity.getRoot();
				update = true;
			}

			if (entities.get().containsKey(entity)) {
				return false;
			}

			Map<String, Object> props = new HashMap<>();
			if (update) {
				entity.setUpdater(AuthUtil.getCurrentUser());
				entity.setUpdateTime(Calendar.getInstance().getTime());

				props.put("updater", entity.getUpdater());
				props.put("updateTime", entity.getUpdateTime());
			} else {
				entity.setCreator(AuthUtil.getCurrentUser());
				entity.setCreationTime(Calendar.getInstance().getTime());

				props.put("creator", entity.getCreator());
				props.put("creationTime", entity.getCreationTime());
			}

			updateState(event.getPersister(), event.getState(), props);
			entities.get().put(entity, true);
			return false;
		}

		@Override
		public boolean onPreUpdate(PreUpdateEvent event) {
			if (!(event.getEntity() instanceof BaseEntity)) {
				return false;
			}

			BaseEntity entity = (BaseEntity) event.getEntity();
			if (entity.getRoot() != null) {
				entity = entity.getRoot();
			}

			if (entities.get().containsKey(entity)) {
				return false;
			}

			entity.setUpdater(AuthUtil.getCurrentUser());
			entity.setUpdateTime(Calendar.getInstance().getTime());

			Map<String, Object> props = new HashMap<>();
			props.put("updater", entity.getUpdater());
			props.put("updateTime", entity.getUpdateTime());
			updateState(event.getPersister(), event.getState(), props);

			entities.get().put(entity, true);
			return false;
		}

		@Override
		public boolean onPreDelete(PreDeleteEvent event) {
			if (!(event.getEntity() instanceof BaseEntity)) {
				return false;
			}

			BaseEntity entity = (BaseEntity) event.getEntity();
			if (entity.getRoot() != null) {
				entity = entity.getRoot();
			}

			if (entities.get().containsKey(entity)) {
				return false;
			}

			entity.setUpdater(AuthUtil.getCurrentUser());
			entity.setUpdateTime(Calendar.getInstance().getTime());

			Map<String, Object> props = new HashMap<>();
			props.put("updater", entity.getUpdater());
			props.put("updateTime", entity.getUpdateTime());
			updateState(event.getPersister(), event.getDeletedState(), props);

			entities.get().put(entity, true);
			return false;
		}

		@Override
		public void onPreRecreateCollection(PreCollectionRecreateEvent event) {
			if (!(event.getAffectedOwnerOrNull() instanceof BaseEntity)) {
				return;
			}

			BaseEntity entity = (BaseEntity) event.getAffectedOwnerOrNull();
			if (entity.getRoot() != null) {
				entity = entity.getRoot();
			}

			if (entities.get().containsKey(entity)) {
				return;
			}

			if (entity.getId() != null) {
				entity.setUpdater(AuthUtil.getCurrentUser());
				entity.setUpdateTime(Calendar.getInstance().getTime());
			} else {
				entity.setCreator(AuthUtil.getCurrentUser());
				entity.setCreationTime(Calendar.getInstance().getTime());
			}

			entities.get().put(entity, true);
		}

		@Override
		public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
			if (!(event.getAffectedOwnerOrNull() instanceof BaseEntity)) {
				return;
			}

			BaseEntity entity = (BaseEntity) event.getAffectedOwnerOrNull();
			if (entity.getRoot() != null) {
				entity = entity.getRoot();
			}

			if (entities.get().containsKey(entity)) {
				return;
			}

			entity.setUpdater(AuthUtil.getCurrentUser());
			entity.setUpdateTime(Calendar.getInstance().getTime());
			entities.get().put(entity, true);
		}

		@Override
		public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
			if (!(event.getAffectedOwnerOrNull() instanceof BaseEntity)) {
				return;
			}

			BaseEntity entity = (BaseEntity) event.getAffectedOwnerOrNull();
			if (entity.getRoot() != null) {
				entity = entity.getRoot();
			}

			if (entities.get().containsKey(entity)) {
				return;
			}

			entity.setUpdater(AuthUtil.getCurrentUser());
			entity.setUpdateTime(Calendar.getInstance().getTime());
			entities.get().put(entity, true);
		}

		private void updateState(EntityPersister persister, Object[] state, Map<String, Object> values) {
			if (values == null || values.isEmpty()) {
				return;
			}

			int idx = -1, count = 0;
			for (String prop : persister.getPropertyNames()) {
				++idx;

				Object value = values.get(prop);
				if (value == null) {
					continue;
				}

				state[idx] = value;
				++count;

				if (count == values.size()) {
					break;
				}
			}
		}
	}
}