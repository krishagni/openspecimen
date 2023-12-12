package com.krishagni.catissueplus.core.importer.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.events.MergedObject;
import com.krishagni.catissueplus.core.audit.repository.RevisionsListCriteria;
import com.krishagni.catissueplus.core.audit.services.AuditService;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvException;
import com.krishagni.catissueplus.core.common.util.CsvFileReader;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.SessionUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;
import com.krishagni.catissueplus.core.importer.domain.ImportJob.CsvType;
import com.krishagni.catissueplus.core.importer.domain.ImportJob.Status;
import com.krishagni.catissueplus.core.importer.domain.ImportJob.Type;
import com.krishagni.catissueplus.core.importer.domain.ImportJobErrorCode;
import com.krishagni.catissueplus.core.importer.domain.ObjectSchema;
import com.krishagni.catissueplus.core.importer.events.FileRecordsDetail;
import com.krishagni.catissueplus.core.importer.events.ImportDetail;
import com.krishagni.catissueplus.core.importer.events.ImportJobDetail;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.events.ObjectSchemaCriteria;
import com.krishagni.catissueplus.core.importer.repository.ImportJobDao;
import com.krishagni.catissueplus.core.importer.repository.ListImportJobsCriteria;
import com.krishagni.catissueplus.core.importer.services.ImportService;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;
import com.krishagni.catissueplus.core.importer.services.ObjectImporterFactory;
import com.krishagni.catissueplus.core.importer.services.ObjectImporterLifecycle;
import com.krishagni.catissueplus.core.importer.services.ObjectReader;
import com.krishagni.catissueplus.core.importer.services.ObjectSchemaFactory;
import com.krishagni.catissueplus.core.init.AppProperties;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.query.cachestore.LinkedEhCacheMap;

public class ImportServiceImpl implements ImportService, ApplicationListener<ContextRefreshedEvent>, InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(ImportServiceImpl.class);

	private static final int MAX_RECS_PER_TXN = 5000;

	private static final String CFG_MAX_TXN_SIZE = "import_max_records_per_txn";

	private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyyMMddHHmmssSSS");

	private ConfigurationService cfgSvc;

	private ImportJobDao importJobDao;

	private ObjectSchemaFactory schemaFactory;
	
	private ObjectImporterFactory importerFactory;

	private AuditService auditService;
	
	private TransactionTemplate txTmpl;

	private TransactionTemplate newTxTmpl;

	private BlockingQueue<ImportJob> queuedJobs = new LinkedBlockingQueue<>();

	private volatile long lastRefreshTime = 0;

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setImportJobDao(ImportJobDao importJobDao) {
		this.importJobDao = importJobDao;
	}
	
	public void setSchemaFactory(ObjectSchemaFactory schemaFactory) {
		this.schemaFactory = schemaFactory;
	}

	public void setImporterFactory(ObjectImporterFactory importerFactory) {
		this.importerFactory = importerFactory;
	}

	public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.txTmpl = new TransactionTemplate(transactionManager);
		this.txTmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

		this.newTxTmpl = new TransactionTemplate(transactionManager);
		this.newTxTmpl.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ImportJobDetail>> getImportJobs(RequestEvent<ListImportJobsCriteria> req) {
		try {
			ListImportJobsCriteria crit = req.getPayload();
			if (AuthUtil.isInstituteAdmin()) {
				crit.instituteId(AuthUtil.getCurrentUserInstitute().getId());
			} else if (!AuthUtil.isAdmin()) {
				crit.userId(AuthUtil.getCurrentUser().getId());
			}
			
			List<ImportJob> jobs = importJobDao.getImportJobs(crit);			
			return ResponseEvent.response(ImportJobDetail.from(jobs));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ImportJobDetail> getImportJob(RequestEvent<Long> req) {
		try {
			ImportJob job = getImportJob(req.getPayload());
			return ResponseEvent.response(ImportJobDetail.from(job));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<String> getImportJobFile(RequestEvent<Long> req) {
		try {
			ImportJob job = getImportJob(req.getPayload());
			File file = new File(getJobOutputFilePath(job.getId()));
			if (!file.exists()) {
				return ResponseEvent.userError(ImportJobErrorCode.OUTPUT_FILE_NOT_CREATED);
			}
			
			return ResponseEvent.response(file.getAbsolutePath());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<String> uploadImportJobFile(RequestEvent<InputStream> req) {
		OutputStream out = null;
		
		try {
			if (!AccessCtrlMgr.getInstance().isBulkImportAllowed()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			//
			// 1. Ensure import directory is present
			//
			String importDir = getImportDir();			
			new File(importDir).mkdirs();
			
			//
			// 2. Generate unique file ID
			//
			String fileId = UUID.randomUUID().toString();
			
			//
			// 3. Copy uploaded file to import directory
			//
			InputStream in = req.getPayload();			
			out = new FileOutputStream(importDir + File.separator + fileId);
			IOUtils.copy(in, out);
			
			return ResponseEvent.response(fileId);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}	
	
	@Override
	public ResponseEvent<ImportJobDetail> importObjects(RequestEvent<ImportDetail> req) {
		ImportJob job = null;
		try {
			ImportDetail detail = req.getPayload();
			String inputFile = getFilePath(detail.getInputFileId());

			if (isZipFile(inputFile)) {
				inputFile = inflateAndGetInputCsvFile(inputFile, detail.getInputFileId());
			}

			//
			// Ensure transaction size is well within configured limits
			//
			int inputRecordsCnt = CsvFileReader.getRowsCount(inputFile, true);
			if (detail.isAtomic() && inputRecordsCnt > getMaxRecsPerTxn()) {
				return ResponseEvent.response(ImportJobDetail.txnSizeExceeded(inputRecordsCnt));
			}

			job = createImportJob(detail);

			//
			// Set up file in job's directory
			//
			createJobDir(job.getId());
			moveToJobDir(inputFile, job.getId());

			queuedJobs.offer(job);
			return ResponseEvent.response(ImportJobDetail.from(job));
		} catch (CsvException csve) {
			return ResponseEvent.userError(ImportJobErrorCode.RECORD_PARSE_ERROR, csve.getLocalizedMessage());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ImportJobDetail> stopJob(RequestEvent<Long> req) {
		try {
			Long jobId = req.getPayload();
			ImportJob job = importJobDao.getJobForUpdate(jobId);
			ensureAccess(job);

			if (!job.isInProgress() && !job.isQueued()) {
				return ResponseEvent.userError(ImportJobErrorCode.NOT_IN_PROGRESS, req.getPayload());
			}

			if (job.isQueued()) {
				queuedJobs.removeIf(qj -> qj.equals(job));
				job.setStatus(Status.STOPPED);
			} else {
				job.stopRunning();
			}

			importJobDao.saveOrUpdate(job);
			return ResponseEvent.response(ImportJobDetail.from(job));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<String> getInputFileTemplate(RequestEvent<ObjectSchemaCriteria> req) {
		try {
			ObjectSchemaCriteria detail = req.getPayload();
			ObjectSchema schema = schemaFactory.getSchema(detail.getObjectType(), detail.getParams());
			if (schema == null) {
				return ResponseEvent.userError(ImportJobErrorCode.OBJ_SCHEMA_NOT_FOUND, detail.getObjectType());
			}

			return ResponseEvent.response(ObjectReader.getSchemaFields(schema, detail.getFieldSeparator()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<List<Map<String, Object>>> processFileRecords(RequestEvent<FileRecordsDetail> req) {
		ObjectReader reader = null;
		File file = null;

		try {
			FileRecordsDetail detail = req.getPayload();

			ObjectSchema.Record schemaRec = new ObjectSchema.Record();
			schemaRec.setFields(detail.getFields());

			ObjectSchema schema = new ObjectSchema();
			schema.setRecord(schemaRec);

			file = new File(getFilePath(detail.getFileId()));
			reader = new ObjectReader(
				file.getAbsolutePath(),
				schema,
				ConfigUtil.getInstance().getDeDateFmt(),
				ConfigUtil.getInstance().getTimeFmt());

			List<Map<String, Object>> records = new ArrayList<>();
			Map<String, Object> record = null;
			while ((record = (Map<String, Object>)reader.next()) != null) {
				records.add(record);
			}

			return ResponseEvent.response(records);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			IOUtils.closeQuietly(reader);
			if (file != null) {
				file.delete();
			}
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> scheduleImportJobs(RequestEvent<FileDetail> req) {
		File workDir = null;

		try {
			if (!AuthUtil.isAdmin()) {
				return ResponseEvent.userError(RbacErrorCode.ADMIN_RIGHTS_REQUIRED);
			}

			workDir = new File(getImportDir(), UUID.randomUUID().toString());
			workDir.mkdirs();

			FileDetail input = req.getPayload();
			File importFile = new File(workDir, input.getFilename());
			if (!importFile.getCanonicalPath().startsWith(workDir.getCanonicalPath())) {
				return ResponseEvent.userError(CommonErrorCode.INVALID_INPUT, "Path traversal attempt");
			}

			try (OutputStream out = new FileOutputStream(importFile)) {
				IOUtils.copy(input.getFileIn(), out);
			}

			List<File> files = Collections.emptyList();
			if (isZipFile(importFile)) {
				File inflateDir = new File(workDir, "inflated");
				try (InputStream zipIn = new FileInputStream(importFile)) {
					Utility.inflateZip(zipIn, inflateDir.getAbsolutePath());
				}

				TreeMap<Date, File> sortedFiles = new TreeMap<>();
				for (File csv : inflateDir.listFiles()) {
					Date tstmp = getTimestamp(csv);
					if (tstmp == null) {
						return ResponseEvent.userError(ImportJobErrorCode.INV_FILENAME, csv.getName());
					}

					sortedFiles.put(tstmp, csv);
				}

				files = new ArrayList<>(sortedFiles.values());
			} else {
				Date tstmp = getTimestamp(importFile);
				if (tstmp == null) {
					return ResponseEvent.userError(ImportJobErrorCode.INV_FILENAME, importFile.getName());
				}

				files = Collections.singletonList(importFile);
			}

			File scheduledDir = new File(getDataDir(), "scheduled-bulk-import");
			for (File file : files) {
				FileUtils.moveFileToDirectory(file, scheduledDir, true);
			}

			return ResponseEvent.response(files.size());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			if (workDir != null) {
				try {
					FileUtils.deleteDirectory(workDir);
				} catch (Exception e) {
					logger.error("Error deleting the directory: " + workDir.getAbsolutePath());
				}
			}
		}
	}

	@Override
	@PlusTransactional
	public ImportJob saveJob(String entity, String op, Date startTime, Map<String, String> params) {
		ImportJob job = new ImportJob();
		job.setName(entity);
		job.setType(Type.valueOf(op));
		job.setCsvtype(ImportJob.CsvType.SINGLE_ROW_PER_OBJ);
		job.setStatus(ImportJob.Status.COMPLETED);
		job.setTotalRecords(1L);
		job.setFailedRecords(0L);
		job.setCreatedBy(AuthUtil.getCurrentUser());
		job.setCreationTime(startTime);
		job.setEndTime(Calendar.getInstance().getTime());
		job.setAtomic(true);
		job.setIpAddress(AuthUtil.getRemoteAddr());
		job.param("noReport", Boolean.TRUE.toString());
		if (params != null) {
			job.getParams().putAll(params);
		}

		importJobDao.saveOrUpdate(job, true);
		return job;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		boolean startScheduler = (lastRefreshTime == 0L);
		lastRefreshTime = System.currentTimeMillis();
		if (!startScheduler) {
			return;
		}

		Executors.newSingleThreadExecutor().submit(() -> {
			while (true) {
				try {
					while ((System.currentTimeMillis() - lastRefreshTime) < 60 * 1000) {
						Thread.sleep(10 * 1000);
					}

					logger.info("Starting bulk import jobs scheduler");
					runImportJobScheduler();
				} catch (Throwable t) {
					logger.error("Bulk import jobs thread stopped. Restarting the thread after 60 seconds.", t);
				} finally {
					lastRefreshTime = System.currentTimeMillis();
				}
			}
		});
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		auditService.registerExporter(this::exportJobsReport);
	}

	private boolean isZipFile(String zipFilename) {
		return isZipFile(new File(zipFilename));
	}

	private boolean isZipFile(File file) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			IOUtils.closeQuietly(zipFile);
		}
	}

	private String inflateAndGetInputCsvFile(String zipFile, String fileId) {
		FileInputStream in = null;
		String inputFile = null;
		try {
			in = new FileInputStream(zipFile);

			File zipDirFile = new File(getImportDir() + File.separator + "inflated-" + fileId);
			Utility.inflateZip(in, zipDirFile.getAbsolutePath());
			inputFile = Arrays.stream(Objects.requireNonNull(zipDirFile.listFiles()))
				.filter(f -> !f.isDirectory() && f.getName().endsWith(".csv"))
				.map(File::getAbsolutePath)
				.findFirst()
				.orElse(null);
		
			if (inputFile == null) {
				throw OpenSpecimenException.userError(ImportJobErrorCode.CSV_NOT_FOUND_IN_ZIP, zipFile);
			}
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(in);
		}

		return inputFile;
	}

	private int getMaxRecsPerTxn() {
		return ConfigUtil.getInstance().getIntSetting("common", CFG_MAX_TXN_SIZE, MAX_RECS_PER_TXN);
	}

	private ImportJob getImportJob(Long jobId) {
		ImportJob job = importJobDao.getById(jobId);
		if (job == null) {
			throw OpenSpecimenException.userError(ImportJobErrorCode.NOT_FOUND, jobId);
		}

		return ensureAccess(job);
	}

	private ImportJob ensureAccess(ImportJob job) {
		User currentUser = AuthUtil.getCurrentUser();
		if (!currentUser.isAdmin() && !currentUser.equals(job.getCreatedBy())) {
			throw OpenSpecimenException.userError(ImportJobErrorCode.ACCESS_DENIED);
		}

		return job;
	}
	
	private String getDataDir() {
		return cfgSvc.getDataDir();
	}
	
	private String getImportDir() {
		return getDataDir() + File.separator + "bulk-import";
	}
	
	private String getFilePath(String fileId) { 
		 return getImportDir() + File.separator + fileId;
	}
	
	private String getJobsDir() {
		return getDataDir() + File.separator + "bulk-import" + File.separator + "jobs";
	}
	
	private String getJobDir(Long jobId) {
		return getJobsDir() + File.separator + jobId;
	}
	
	private String getJobOutputFilePath(Long jobId) {
		return getJobDir(jobId) + File.separator + "output.csv";
	}
	
	private String getFilesDirPath(Long jobId) {
		return getJobDir(jobId) + File.separator + "files";
	}
	
	private boolean createJobDir(Long jobId) {
		return new File(getJobDir(jobId)).mkdirs();
	}
	
	private void moveToJobDir(String file, Long jobId) {
		String jobDir = getJobDir(jobId);

		//
		// Input file and its parent directory
		//
		File inputFile = new File(file);
		File srcDir = inputFile.getParentFile();

		//
		// Move input CSV file to job dir
		//
		inputFile.renameTo(new File(jobDir + File.separator + "input.csv"));

		//
		// Move other uploaded files to job dir as well
		//
		File filesDir = new File(srcDir + File.separator + "files");
		if (filesDir.exists()) {
			filesDir.renameTo(new File(getFilesDirPath(jobId)));
		}
	}

	@PlusTransactional
	private ImportJob createImportJob(ImportDetail detail) { // TODO: ensure checks are done
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		ImportJob job = new ImportJob();
		job.setCreatedBy(AuthUtil.getCurrentUser());
		job.setCreationTime(Calendar.getInstance().getTime());
		job.setName(detail.getObjectType());
		job.setStatus(Status.QUEUED);
		job.setAtomic(detail.isAtomic());
		job.setParams(detail.getObjectParams());
		job.setFieldSeparator(detail.getFieldSeparator());
		job.setRunByNode("none");
		job.setIpAddress(AuthUtil.getRemoteAddr());

		setImportType(detail, job, ose);
		setCsvType(detail, job, ose);
		setDateAndTimeFormat(detail, job, ose);
		setTimeZone(detail, job, ose);
		ose.checkAndThrow();

		importJobDao.saveOrUpdate(job, true);
		return job;		
	}

	private void setImportType(ImportDetail detail, ImportJob job, OpenSpecimenException ose) {
		String importType = detail.getImportType();
		job.setType(StringUtils.isBlank(importType) ? Type.CREATE : Type.valueOf(importType));
	}

	private void setCsvType(ImportDetail detail, ImportJob job, OpenSpecimenException ose) {
		String csvType = detail.getCsvType();
		job.setCsvtype(StringUtils.isBlank(csvType) ? CsvType.SINGLE_ROW_PER_OBJ : CsvType.valueOf(csvType));
	}

	private void setDateAndTimeFormat(ImportDetail detail, ImportJob job, OpenSpecimenException ose) {
		String dateFormat = detail.getDateFormat();
		if (StringUtils.isBlank(dateFormat)) {
			dateFormat = ConfigUtil.getInstance().getDeDateFmt();
		} else if (!Utility.isValidDateFormat(dateFormat)) {
			ose.addError(ImportJobErrorCode.INVALID_DATE_FORMAT, dateFormat);
			return;
		}

		job.setDateFormat(dateFormat);

		String timeFormat = detail.getTimeFormat();
		if (StringUtils.isBlank(timeFormat)) {
			timeFormat = ConfigUtil.getInstance().getTimeFmt();
		} else if (!Utility.isValidDateFormat(dateFormat + " " + timeFormat)) {
			ose.addError(ImportJobErrorCode.INVALID_TIME_FORMAT, timeFormat);
			return;
		}

		job.setTimeFormat(timeFormat);
	}

	private void setTimeZone(ImportDetail detail, ImportJob job, OpenSpecimenException ose) {
		String timeZone = detail.getTimeZone();
		if (StringUtils.isNotBlank(timeZone)) {
			try {
				TimeZone tz = TimeZone.getTimeZone(timeZone);
			} catch (Exception e) {
				ose.addError(CommonErrorCode.INVALID_TZ, timeZone);
				return;
			}
		} else {
			TimeZone tz = AuthUtil.getUserTimeZone();
			if (tz != null) {
				timeZone = tz.getID();
			}
		}

		job.setTimeZone(timeZone);
	}

	private void runImportJobScheduler() {
		try {
			int failedJobs = markInProgressJobsAsFailed();
			logger.info("Marked " + failedJobs + " in-progress jobs from previous incarnation as failed");

			loadJobQueue();

			while (true) {
				logger.info("Probing for queued bulk import jobs");
				ImportJob job = queuedJobs.poll(15, TimeUnit.MINUTES);
				if (job == null) {
					//
					// to load the jobs, if any, that were queued on the dead nodes
					//
					loadJobQueue();
					continue;
				} else if (job.isStopped()) {
					continue;
				}

				try {
					if (!acquireLock(job)) {
						continue;
					}

					logger.info("Picked import job " + job.getId() + " for processing");
					new ImporterTask(job).run();
				} catch (Throwable t) {
					logger.error("Error running the job " + job.getId() + " to completion", t);
				} finally {
					releaseRunLock();
				}
			}
		} catch (Throwable t) {
			logger.error("Import jobs scheduler encountered a fatal exception. Stopping to run import jobs.", t);
		}
	}

	@PlusTransactional
	private int markInProgressJobsAsFailed() {
		return importJobDao.markInProgressJobsAsFailed(AppProperties.getInstance().getNodeName());
	}

	@PlusTransactional
	private void loadJobQueue() {
		logger.info("Loading bulk import jobs queue");

		ListImportJobsCriteria crit = new ListImportJobsCriteria()
			.status(Status.QUEUED.name())
			.maxResults(100);

		int startAt = 0;
		boolean endOfJobs = false;
		while (!endOfJobs) {
			List<ImportJob> jobs = importJobDao.getImportJobs(crit.startAt(startAt));
			jobs.forEach(job -> {
				if (!Hibernate.isInitialized(job.getCreatedBy())) {
					Hibernate.initialize(job.getCreatedBy());
				}

				job.getParams().size();

				queuedJobs.offer(job);
			});

			endOfJobs = (jobs.size() < 100);
			startAt += jobs.size();
		}

		logger.info("Loaded " + startAt + " bulk import jobs in queue from previous incarnation");
	}

	private boolean acquireLock(ImportJob job) {
		logger.info("Attempting to acquire lock on the job: " + job.getId());
		if (!acquireJobLock(job)) {
			logger.info("Failed to acquire lock on the job: " + job.getId());
			return false;
		}

		logger.info("Acquired lock on the job: " + job.getId());

		while (true) {
			try {
				logger.info("Attempting to acquire import job run lock... ");
				if (acquireRunLock()) {
					break;
				}

				logger.info("Failed to acquire import job run lock. Will try again in 60 seconds!");
				TimeUnit.SECONDS.sleep(60);
			} catch (Exception e) {
				releaseJobLock(job);
				logger.info("Error when acquiring run lock.", e);
				throw OpenSpecimenException.serverError(e);
			}
		}

		logger.info("Acquired import job run lock");
		return true;
	}

	private boolean acquireJobLock(ImportJob job) {
		return newTxTmpl.execute(
			status -> {
				String thisNode = AppProperties.getInstance().getNodeName();
				ImportJob dbJob = importJobDao.getJobForUpdate(job.getId());
				String activeNode = dbJob.getRunByNode();
				logger.info("Lock on the job " + job.getId() + " is held by " + activeNode);

				if ("none".equals(activeNode) && !"none".equals(thisNode)) {
					dbJob.setRunByNode(thisNode);
					importJobDao.saveOrUpdate(dbJob);
					setJobRunBy(job, thisNode);
					return true;
				} else {
					return thisNode.equals(activeNode);
				}
			}
		);
	}

	private boolean releaseJobLock(ImportJob job) {
		return newTxTmpl.execute(
			status -> {
				String thisNode = AppProperties.getInstance().getNodeName();
				ImportJob dbJob = importJobDao.getJobForUpdate(job.getId());
				String activeNode = dbJob.getRunByNode();
				if (!thisNode.equals(activeNode)) {
					return false;
				}

				dbJob.setRunByNode("none");
				importJobDao.saveOrUpdate(dbJob);
				setJobRunBy(job, "none");
				return true;
			}
		);
	}

	private void setJobRunBy(ImportJob job, String node) {
		TransactionSynchronizationManager.registerSynchronization(
			new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					job.setRunByNode(node);
				}
			}
		);
	}

	private boolean acquireRunLock() {
		return Boolean.TRUE.equals(newTxTmpl.execute(
			status -> {
				String thisNode = AppProperties.getInstance().getNodeName();
				String activeNode = importJobDao.getActiveImportRunnerNode();
				logger.info("Run lock is held by " + activeNode);

				if ("none".equals(activeNode) && !"none".equals(thisNode)) {
					return importJobDao.setActiveImportRunnerNode(thisNode);
				} else {
					return thisNode.equals(activeNode);
				}
			}
		));
	}

	private boolean releaseRunLock() {
		return Boolean.TRUE.equals(newTxTmpl.execute(
			status -> {
				String thisNode = AppProperties.getInstance().getNodeName();
				String activeNode = importJobDao.getActiveImportRunnerNode();
				if (!thisNode.equals(activeNode)) {
					return false;
				}

				return importJobDao.setActiveImportRunnerNode("none");
			}
		));
	}

	private Date getTimestamp(File file) {
		String filename = file.getName();
		String[] tokens = filename.split("_");

		if (tokens.length < 3) {
			logger.info(String.format("Filename '%s' is not in correct format", filename));
			return null;
		}

		String timestampStr = tokens[2];
		if ("extensions".equals(tokens[0]) && tokens.length == 5) {
			timestampStr = tokens[4];
		}

		try {
			return DATE_FMT.parse(timestampStr);
		} catch (ParseException e) {
			logger.error("Appended timestamp in filename is not in correct format: " + timestampStr, e);
		}

		return null;
	}

	//
	// activity report
	//
	private File exportJobsReport(String baseDir, User exportedBy, Date exportedOn, List<User> users, RevisionsListCriteria crit) {
		if (!crit.includeReport("query_exim")) {
			return null;
		}

		File auditDir = new File(ConfigUtil.getInstance().getDataDir(), "audit");
		File result = new File(auditDir, baseDir);
		if (!result.exists()) {
			result.mkdirs();
		}

		String ts = new SimpleDateFormat("yyyyMMdd_HHmm").format(exportedOn);
		File outputFile = new File(result, "os_import_jobs_" + ts + ".csv");
		return exportJobsReport(exportedBy, exportedOn, users, crit, outputFile);
	}

	private File exportJobsReport(User exportedBy, Date exportedOn, List<User> users, RevisionsListCriteria crit, File outputFile) {
		ListImportJobsCriteria jobsListCriteria = new ListImportJobsCriteria()
			.fromDate(crit.startDate())
			.toDate(crit.endDate())
			.userIds(Utility.nullSafeStream(users).map(User::getId).collect(Collectors.toList()));

		OutputStream fout = null;
		CsvWriter csvWriter = null;

		try {
			fout      = Files.newOutputStream(outputFile.toPath());
			csvWriter = CsvFileWriter.createCsvFileWriter(fout);

			Map<String, String> headers = new LinkedHashMap<String, String>() {{
				put(msg("common_server_url"), ConfigUtil.getInstance().getAppUrl());
				put(msg("common_server_env"), ConfigUtil.getInstance().getDeployEnv());
				put(msg("audit_rev_exported_by"), exportedBy.formattedName(true));
				put(msg("audit_rev_exported_on"), Utility.getDateTimeString(exportedOn));

				if (CollectionUtils.isNotEmpty(users)) {
					String userNames = users.stream().map(u -> u.formattedName(true)).collect(Collectors.joining(", "));
					put(msg("audit_rev_audited_users"), userNames);
				}

				if (jobsListCriteria.fromDate() != null) {
					put(msg("audit_rev_start_date"), Utility.getDateTimeString(jobsListCriteria.fromDate()));
				}

				if (jobsListCriteria.toDate() != null) {
					put(msg("audit_rev_end_date"),   Utility.getDateTimeString(jobsListCriteria.toDate()));
				}

				put("", "");
			}};

			Utility.writeKeyValuesToCsv(fout, headers);

			jobsListCriteria.startAt(0).maxResults(100);
			writeJobDetailToCsv(jobsListCriteria, csvWriter);

			return outputFile;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(csvWriter);
			IOUtils.closeQuietly(fout);
		}
	}

	private void writeJobDetailToCsv(ListImportJobsCriteria crit, CsvWriter csvWriter) {
		csvWriter.writeNext(new String[1]);
		csvWriter.writeNext(getAuditLogReportHeaders());

		int count = 0;
		boolean endOfRecords = false;
		while (!endOfRecords) {
			List<ImportJob> jobs = importJobDao.getImportJobs(crit);
			crit.startAt(crit.startAt() + jobs.size());
			endOfRecords = jobs.size() < 100;

			for (ImportJob job : jobs) {
				csvWriter.writeNext(getAuditLogReportData(job));
				++count;
				if (count % 25 == 0) {
					csvWriter.flush();
				}
			}
		}

		csvWriter.flush();
	}

	private String[] getAuditLogReportHeaders() {
		return new String[] {
			msg("audit_rev_id"),
			msg("audit_rev_tstmp"),
			msg("audit_rev_user"),
			msg("audit_rev_user_email"),
			msg("audit_rev_domain"),
			msg("audit_rev_user_login"),
			msg("audit_rev_institute"),
			msg("audit_rev_ip_address"),
			msg("audit_rev_entity_op"),
			msg("audit_rev_entity_name"),
			msg("audit_rev_exec_time"),
			msg("audit_rev_total_records"),
			msg("audit_rev_successful_records"),
			msg("audit_rev_failed_records"),
			msg("audit_rev_status"),
			msg("audit_rev_other_params")
		};
	}

	private String[] getAuditLogReportData(ImportJob job) {
		String timeToFinish = "";
		if (job.getEndTime() != null) {
			timeToFinish = String.valueOf((job.getEndTime().getTime()  - job.getCreationTime().getTime()) / 1000);
		}

		String type = "audit_op_insert";
		if (job.getType() == Type.UPDATE) {
			type = "audit_op_update";
		} else if (job.getType() == Type.UPSERT) {
			type = "audit_op_upsert";
		}

		User user = job.getCreatedBy();
		Map<String, Object> params = new HashMap<>(job.getParams());
		params.put("atomic", job.getAtomic());
		params.put("dateFormat", job.getDateFormat());
		params.put("timeFormat", job.getTimeFormat());
		params.put("timeZone", job.getTimeZone());
		params.put("fieldSeparator", job.getFieldSeparator());
		return new String[] {
			job.getId().toString(),
			Utility.getDateTimeString(job.getCreationTime()),
			user.formattedName(),
			user.getEmailAddress(),
			user.getAuthDomain().getName(),
			user.getLoginName(),
			user.getInstitute() != null ? user.getInstitute().getName() : "",
			job.getIpAddress() != null ? job.getIpAddress() : "",
			msg(type),
			job.getEntityName(),
			timeToFinish,
			job.getTotalRecords() != null ? job.getTotalRecords().toString() : "",
			job.getSuccessfulRecords() != null ? job.getSuccessfulRecords().toString() : "",
			job.getFailedRecords() != null ? job.getFailedRecords().toString() : "",
			msg("bulk_import_statuses_" + job.getStatus()),
			Utility.mapToJson(params, true)
		};
	}

	private String msg(String key) {
		return MessageUtil.getInstance().getMessage(key);
	}


	private class ImporterTask implements Runnable {
		private ImportJob job;

		private boolean atomic;

		private long totalRecords = 0;

		private long failedRecords = 0;

		ImporterTask(ImportJob job) {
			this.job = job;
			this.atomic = Boolean.TRUE.equals(job.getAtomic());
		}

		@Override
		public void run() {
			ObjectReader objReader = null;
			CsvWriter csvWriter = null;

			try {
				AuthUtil.setCurrentUser(job.getCreatedBy(), job.getIpAddress());
				ImporterContextHolder.getInstance().newContext();
				saveJob(totalRecords, failedRecords, Status.IN_PROGRESS);
				if (job.isStopped()) {
					return;
				}

				ObjectSchema schema = schemaFactory.getSchema(job.getName(), job.getParams());
				String filePath = getJobDir(job.getId()) + File.separator + "input.csv";
				csvWriter = getOutputCsvWriter(schema, job);
				objReader = new ObjectReader(filePath, schema, job.getDateFormat(), job.getTimeFormat(), job.getFieldSeparator());
				objReader.setTimeZone(job.getTimeZone());
				objReader.setIgnoreId(job.getType() == Type.CREATE);

				List<String> columnNames = objReader.getCsvColumnNames();
				columnNames.add("OS_IMPORT_STATUS");
				columnNames.add("OS_ERROR_MESSAGE");

				csvWriter.writeNext(columnNames.toArray(new String[0]));

				Status status = processRows(objReader, csvWriter);
				saveJob(totalRecords, failedRecords, status);
			} catch (Throwable t) {
				logger.error("Error running import records job", t);
				saveJob(totalRecords, failedRecords, Status.FAILED);

				String[] errorLine = null;
				if (t instanceof CsvException) {
					errorLine = ((CsvException) t).getErroneousLine();
				}

				if (errorLine == null) {
					errorLine = new String[] { t.getMessage() };
				}

				if (csvWriter != null) {
					csvWriter.writeNext(errorLine);
					csvWriter.writeNext(new String[] { ExceptionUtils.getStackTrace(t) });
				}
			} finally {
				ImporterContextHolder.getInstance().clearContext();
				AuthUtil.clearCurrentUser();

				IOUtils.closeQuietly(objReader);
				closeQuietly(csvWriter);

				//
				// Delete uploaded files
				//
				FileUtils.deleteQuietly(new File(getFilesDirPath(job.getId())));
				sendJobStatusNotification();
			}
		}

		private Status processRows(ObjectReader objReader, CsvWriter csvWriter) {
			if (atomic) {
				return txTmpl.execute(new TransactionCallback<Status>() {
					@Override
					public Status doInTransaction(TransactionStatus txnStatus) {
						Status jobStatus = processRows0(objReader, csvWriter);
						if (jobStatus == Status.FAILED || jobStatus == Status.STOPPED) {
							txnStatus.setRollbackOnly();
						}

						return jobStatus;
					}
				});
			} else {
				return processRows0(objReader, csvWriter);
			}
		}

		private Status processRows0(ObjectReader objReader, CsvWriter csvWriter) {
			boolean stopped;
			ObjectImporter<Object, Object> importer = importerFactory.getImporter(job.getName());
			startImporter(job.getId(), importer);

			if (job.getCsvtype() == CsvType.MULTIPLE_ROWS_PER_OBJ) {
				stopped = processMultipleRowsPerObj(objReader, csvWriter, importer);
			} else {
				stopped = processSingleRowPerObj(objReader, csvWriter, importer);
			}

			shutdownImporter(job.getId(), importer, stopped);

			if (stopped) {
				return Status.STOPPED;
			} else if (failedRecords > 0) {
				return Status.FAILED;
			} else {
				return Status.COMPLETED;
			}
		}

		private boolean processSingleRowPerObj(ObjectReader objReader, CsvWriter csvWriter, ObjectImporter<Object, Object> importer) {
			while (!job.isStopped()) {
				String errMsg = null;
				try {
					Object object = objReader.next();
					if (object == null) {
						break;
					}

					errMsg = importObject(importer, object, job.getParams());
				} catch (OpenSpecimenException ose) {
					errMsg = ose.getMessage();
				}

				++totalRecords;

				List<String> row = objReader.getCsvRow();
				if (StringUtils.isNotBlank(errMsg)) {
					row.add("FAIL");
					row.add(errMsg);
					++failedRecords;
				} else {
					row.add("SUCCESS");
					row.add("");
				}

				csvWriter.writeNext(row.toArray(new String[0]));
				if (totalRecords % 25 == 0) {
					saveJob(totalRecords, failedRecords, Status.IN_PROGRESS);
				}
			}

			return job.isStopped();
		}

		private boolean processMultipleRowsPerObj(ObjectReader objReader, CsvWriter csvWriter, ObjectImporter<Object, Object> importer) {
			LinkedEhCacheMap<String, MergedObject> objectsMap =  new LinkedEhCacheMap<>();
			
			while (true) {
				String errMsg = null;
				Object parsedObj = null;
				
				try {
					parsedObj = objReader.next();
					if (parsedObj == null) {
						break;
					}
				} catch (Exception e) {
					errMsg = e.getMessage();
					if (errMsg == null) {
						errMsg = e.getClass().getName();
					}
				}
				
				String key = objReader.getRowKey();
				MergedObject mergedObj = objectsMap.get(key);
				if (mergedObj == null) {
					mergedObj = new MergedObject();
					mergedObj.setKey(key);
					mergedObj.setObject(parsedObj);
				}

				if (errMsg != null) {
					//
					// mark the object as processed whenever an error is encountered.
					//
					mergedObj.addErrMsg(errMsg);
					mergedObj.setProcessed(true);
				}

				mergedObj.addRow(objReader.getCsvRow());
				mergedObj.merge(parsedObj);
				objectsMap.put(key, mergedObj);
			}

			Iterator<MergedObject> mergedObjIter = objectsMap.iterator();
			while (mergedObjIter.hasNext() && !job.isStopped()) {
				MergedObject mergedObj = mergedObjIter.next();
				if (!mergedObj.isErrorneous()) {
					String errMsg = null;
					try {
						errMsg = importObject(importer, mergedObj.getObject(), job.getParams());
					} catch (OpenSpecimenException ose) {
						errMsg = ose.getMessage();
					}

					if (StringUtils.isNotBlank(errMsg)) {
						mergedObj.addErrMsg(errMsg);
					}

					mergedObj.setProcessed(true);
					objectsMap.put(mergedObj.getKey(), mergedObj);
				}
				
				++totalRecords;
				if (mergedObj.isErrorneous()) {
					++failedRecords;
				}
				
				if (totalRecords % 25 == 0) {
					saveJob(totalRecords, failedRecords, Status.IN_PROGRESS);
				}
			}
			
			mergedObjIter = objectsMap.iterator();
			while (mergedObjIter.hasNext()) {
				MergedObject mergedObj = mergedObjIter.next();
				csvWriter.writeAll(mergedObj.getRowsWithStatus());
			}

			objectsMap.clear();
			return job.isStopped();
		}
		
		private String importObject(final ObjectImporter<Object, Object> importer, Object object, Map<String, String> params) {
			try {
				ImportObjectDetail<Object> detail = new ImportObjectDetail<>();
				detail.setId(job.getId().toString());
				detail.setObjectName(job.getName());
				detail.setCreate(job.getType() == Type.CREATE);
				detail.setType(job.getType().name());
				detail.setObject(object);
				detail.setParams(params);
				detail.setUploadedFilesDir(getFilesDirPath(job.getId()));
				
				final RequestEvent<ImportObjectDetail<Object>> req = new RequestEvent<>(detail);
				ResponseEvent<Object> resp = txTmpl.execute(
						new TransactionCallback<ResponseEvent<Object>>() {
							@Override
							public ResponseEvent<Object> doInTransaction(TransactionStatus status) {
								ResponseEvent<Object> resp = importer.importObject(req);
								if (!resp.isSuccessful()) {
									status.setRollbackOnly();
								}
								
								return resp;
							}
						});

				if (atomic) {
					//
					// Let's give a clean session for every object to be imported
					//
					clearSession();
				}

				if (resp.isSuccessful()) {
					return null;
				} else {
					return resp.getError().getMessage();
				}
			} catch (Exception e) {
				String msg   = ExceptionUtils.getMessage(e);
				String rcMsg = ExceptionUtils.getRootCauseMessage(e);
				if (StringUtils.isBlank(msg)) {
					msg = rcMsg;
				} else if (StringUtils.isNotBlank(rcMsg)) {
					msg += "; Exception: " + rcMsg;
				}

				if (StringUtils.isBlank(msg)) {
					msg = "Internal Server Error: Despite our best efforts, we could not find the root cause of the error. " +
						  "Please check the log files";
				}

				return msg;
			}
		}

		private void startImporter(Long jobId, ObjectImporter<Object, Object> importer) {
			if (!(importer instanceof ObjectImporterLifecycle)) {
				return;
			}

			txTmpl.execute((txnStatus) -> {
				((ObjectImporterLifecycle) importer).start(jobId.toString());
				return null;
			});
		}

		private void shutdownImporter(Long jobId, ObjectImporter<Object, Object> importer, boolean stopped) {
			if (!(importer instanceof ObjectImporterLifecycle)) {
				return;
			}

			try {
				Map<String, Object> runCtxt = new HashMap<>();
				runCtxt.put("stopped", stopped);
				runCtxt.put("failedRecords", failedRecords);

				txTmpl.execute((txnStatus) -> {
					((ObjectImporterLifecycle) importer).stop(jobId.toString(), runCtxt);
					return null;
				});
			} catch (Exception e) {
				logger.error("Error stopping the importer", e);
			}
		}

		private Status saveJob(long totalRecords, long failedRecords, Status status) {
			return newTxTmpl.execute(txnStatus -> {
				ImportJob dbJob = importJobDao.getJobForUpdate(job.getId());
				dbJob.setTotalRecords(totalRecords);
				dbJob.setFailedRecords(failedRecords);

				if (status != Status.IN_PROGRESS) {
					// either completed or failed
					dbJob.setStatus(status);
				} else if (dbJob.isAskedToStop() || dbJob.isStopped()) {
					// in progress job has been asked to stop
					dbJob.setStatus(Status.STOPPED);
				} else {
					// in progress job
					dbJob.setStatus(status);
				}

				if (!dbJob.isInProgress()) {
					dbJob.setEndTime(Calendar.getInstance().getTime());
				}

				job.setTotalRecords(dbJob.getTotalRecords());
				job.setFailedRecords(dbJob.getFailedRecords());
				job.setStatus(dbJob.getStatus());
				job.setEndTime(dbJob.getEndTime());
				importJobDao.saveOrUpdate(dbJob);
				return dbJob.getStatus();
			});
		}

		private CsvWriter getOutputCsvWriter(ObjectSchema schema, ImportJob job)
		throws IOException {
			char fieldSeparator = Utility.getFieldSeparator();
			if (StringUtils.isNotBlank(job.getFieldSeparator())) {
				fieldSeparator = job.getFieldSeparator().charAt(0);
			} else if (StringUtils.isNotBlank(schema.getFieldSeparator())) {
				fieldSeparator = schema.getFieldSeparator().charAt(0);
			}

			FileWriter writer = new FileWriter(getJobOutputFilePath(job.getId()));
			return CsvFileWriter.createCsvFileWriter(writer, fieldSeparator, '"');
		}
				
		private void closeQuietly(CsvWriter writer) {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) {
											
				}
			}
		}

		private void sendJobStatusNotification() {
			try {
				String entityName = job.getEntityName();
				String op = msg("bulk_import_ops_" + job.getType());
				String [] subjParams = new String[] {
					job.getId().toString(),
					op,
					entityName
				};

				Map<String, Object> props = new HashMap<>();
				props.put("job", job);
				props.put("entityName", entityName);
				props.put("op", op);
				props.put("status", msg("bulk_import_statuses_" + job.getStatus()));
				props.put("atomic", atomic);
				props.put("$subject", subjParams);
				props.put("ccAdmin", isCopyNotifToAdminEnabled());

				String[] rcpts = {job.getCreatedBy().getEmailAddress()};
				EmailUtil.getInstance().sendEmail(JOB_STATUS_EMAIL_TMPL, rcpts, null, props);
			} catch (Throwable t) {
				logger.error("Failed to send import job status e-mail notification", t);
			}
		}

		private void clearSession() {
			SessionUtil.getInstance().clearSession();
		}

		private boolean isCopyNotifToAdminEnabled() {
			return ConfigUtil.getInstance().getBoolSetting("notifications", "cc_import_emails_to_admin", false);
		}

		private static final String JOB_STATUS_EMAIL_TMPL = "import_job_status_notif";

		private static final String EXTENSIONS = "extensions";
	}
}
