package com.krishagni.catissueplus.core.exporter.services.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileReader;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvReader;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.exporter.domain.ExportErrorCode;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.events.ExportDetail;
import com.krishagni.catissueplus.core.exporter.events.ExportJobDetail;
import com.krishagni.catissueplus.core.exporter.repository.ExportJobDao;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.catissueplus.core.importer.domain.ObjectSchema;
import com.krishagni.catissueplus.core.importer.services.ObjectSchemaFactory;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.nutility.DeConfiguration;
import edu.common.dynamicextensions.nutility.IoUtil;

public class ExportServiceImpl implements ExportService {
	private static final LogUtil logger = LogUtil.getLogger(ExportServiceImpl.class);

	private Map<String, Supplier<Function<ExportJob, List<? extends Object>>>> genFactories = new HashMap<>();

	private ExportJobDao exportJobDao;

	private ObjectSchemaFactory schemaFactory;

	private ThreadPoolTaskExecutor taskExecutor;

	private int ONLINE_EXPORT_TIMEOUT_SECS = 30;

	public void setSchemaFactory(ObjectSchemaFactory schemaFactory) {
		this.schemaFactory = schemaFactory;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setExportJobDao(ExportJobDao exportJobDao) {
		this.exportJobDao = exportJobDao;
	}

	@Override
	public ResponseEvent<ExportJobDetail> exportObjects(RequestEvent<ExportDetail> req) {
		Pair<ExportJob, Future<Integer>> result = exportObjects(req.getPayload());

		try {
			result.second().get(ONLINE_EXPORT_TIMEOUT_SECS, TimeUnit.SECONDS);
		} catch (TimeoutException te) {
			//
			// Timed out waiting for the export job to finish.
			// An email with export status will be sent to user.
			//
		} catch (OpenSpecimenException ose) {
			throw ose;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}

		return ResponseEvent.response(ExportJobDetail.from(result.first()));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<String> getExportFile(RequestEvent<Long> req) {
		Long jobId = req.getPayload();
		ExportJob job = exportJobDao.getById(jobId);
		if (job == null) {
			return ResponseEvent.userError(ExportErrorCode.JOB_NOT_FOUND, jobId);
		}

		if (!job.isOutputAccessibleBy(AuthUtil.getCurrentUser())) {
			return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
		}

		return ResponseEvent.response(getOutputFile(job));
	}

	@Override
	public void registerObjectsGenerator(String type, Supplier<Function<ExportJob, List<?>>> genFactory) {
		genFactories.put(type, genFactory);
	}

	private Pair<ExportJob, Future<Integer>> exportObjects(ExportDetail detail) {
		ExportJob job = createJob(detail);
		ExportTask task = new ExportTask(job);

		Future<Integer> result;
		if (detail.isSynchronous()) {
			User currentUser = AuthUtil.getCurrentUser();
			try {
				result = CompletableFuture.completedFuture(task.call());
			} finally {
				AuthUtil.setCurrentUser(currentUser);
			}
		} else {
			result = taskExecutor.submit(task);
		}

		return Pair.make(job, result);
	}

	@PlusTransactional
	private ExportJob createJob(ExportDetail detail) {
		ObjectSchema schema = schemaFactory.getSchema(detail.getObjectType(), detail.getParams());
		if (schema == null) {
			throw OpenSpecimenException.userError(ExportErrorCode.INVALID_OBJECT_TYPE, detail.getObjectType());
		}

		Supplier<Function<ExportJob, List<? extends Object>>> generator = genFactories.get(detail.getObjectType());
		if (generator == null) {
			throw  OpenSpecimenException.userError(ExportErrorCode.NO_GEN_FOR_OBJECT_TYPE, detail.getObjectType());
		}

		TimeZone tz = AuthUtil.getUserTimeZone();
		ExportJob job = new ExportJob();
		job.setName(detail.getObjectType());
		job.setCreatedBy(AuthUtil.getCurrentUser());
		job.setCreationTime(Calendar.getInstance().getTime());
		job.setParams(getParams(detail.getParams()));
		job.setSchema(schema);
		job.setRecordIds(detail.getRecordIds());
		job.setDisableNotifs(detail.isDisableNotifs());
		job.param("timeZone", tz != null ? tz.getID() : null);
		exportJobDao.saveOrUpdate(job.markInProgress(), true);
		return job;
	}

	private Map<String, String> getParams(Map<String, String> params) {
		Map<String, String> result = new HashMap<>();
		if (params != null) {
			result.putAll(params);
		}

		return result;
	}

	private class ExportTask implements Callable<Integer> {
		private ExportJob job;

		private Function<ExportJob, List<? extends Object>> generator;

		private Map<String, Integer> fieldInstances = new HashMap<>();

		private CsvWriter writer;

		private CsvReader reader;

		private long written = 0;

		private File filesDir;

		private long filesCount = 0;

		private SimpleDateFormat dateOnlyFormatter;

		private SimpleDateFormat dateFormatter;

		private SimpleDateFormat dateTimeFormatter;

		ExportTask(ExportJob inputJob) {
			job = inputJob;
			generator = genFactories.get(job.getName()).get();

			String timeZoneStr = inputJob.param("timeZone");
			String dateFmt = ConfigUtil.getInstance().getDeDateFmt();
			dateOnlyFormatter = new SimpleDateFormat(dateFmt);
			dateFormatter = new SimpleDateFormat(dateFmt);
			dateTimeFormatter = new SimpleDateFormat(dateFmt + " " + ConfigUtil.getInstance().getTimeFmt());
			if (StringUtils.isNotBlank(timeZoneStr)) {
				try {
					TimeZone tz = TimeZone.getTimeZone(timeZoneStr);
					dateFormatter.setTimeZone(tz);
					dateTimeFormatter.setTimeZone(tz);
				} catch (Exception e) {
					logger.error("Error using the timezone: " + timeZoneStr, e);
				}
			}
		}


		public Integer call() {
			if (!new File(getJobDir(job)).mkdirs()) {
				logger.error("Unable to create jobs directory");
				failed();
				return -1;
			}

			try {
				AuthUtil.setCurrentUser(job.getCreatedBy());
				ExporterContextHolder.getInstance().newContext();

				generateRawRecordsFile();
				generateOutputFile();
				generateOutputZip(job);
				completed();
				return 0;
			} catch (Exception e) {
				failed();
				logger.error("Error exporting records", e);
				return -1;
			} finally {
				ExporterContextHolder.getInstance().clearContext();
				AuthUtil.clearCurrentUser();
				cleanupFiles(job);
				sendJobStatusNotification(job);
			}
		}

		private void generateRawRecordsFile()
		throws IOException {
			ObjectSchema.Record record = job.getSchema().getRecord();

			try {
				openWriter(getRawDataFile(job));

				long records = 0;
				List<? extends Object> objects;
				while (true) {
					objects = getObjects();
					if (CollectionUtils.isEmpty(objects)) {
						break;
					}

					for (Object object : objects) {
						write(getRawDataRow("", "", record, object));
						++records;

						if (records % 50 == 0) {
							updateRecordsCount(records);
						}
					}
				}

				if (records % 50 != 0) {
					updateRecordsCount(records);
				}

				flush();
			} finally {
				closeWriter();
			}
		}

		private List<String> getRawDataRow(String namePrefix, String captionPrefix, ObjectSchema.Record record, Object object) {
			List<String> row = new ArrayList<>();

			for (ObjectSchema.Field field : record.getFields()) {
				String caption = captionPrefix + field.getCaption();

				if (field.isMultiple()) {
					Collection<Object> collection = getCollection(object, field.getAttribute());
					int count = 0;
					for (Object element : collection) {
						row.add(caption + "#" + ++count);
						row.add(element.toString());
					}

					updateFieldCount(namePrefix + field.getAttribute(), count);
				} else {
					String value = getString(object, field);
					if (StringUtils.isNotBlank(value) &&
						("file".equals(field.getType()) || "defile".equals(field.getType()) || "signature".equals(field.getType()))) {
						value = ++filesCount + "_" + value;
						writeFileData(object, field, value);
					}

					row.add(caption);
					row.add(value);
				}
			}

			for (ObjectSchema.Record subRecord : record.getSubRecords()) {
				String srName = namePrefix + subRecord.getAttribute();
				String srCaption = captionPrefix;
				if (StringUtils.isNotBlank(subRecord.getCaption())) {
					srCaption += subRecord.getCaption() + "#";
				}

				if (subRecord.isMultiple()) {
					Collection<Object> collection = getCollection(object, subRecord.getAttribute());
					int count = 0;
					for (Object element : collection) {
						row.addAll(getRawDataRow(srName + ".", srCaption + ++count + "#", subRecord, element));
					}

					updateFieldCount(srName, count);
				} else {
					Object subObject = getObject(object, subRecord.getAttribute());
					if (subObject != null) {
						row.addAll(getRawDataRow(srName + ".", srCaption, subRecord, subObject));
					}
				}
			}

			return row;
		}

		private List<ObjectSchema.Field> getFileFields(ObjectSchema.Record record) {
			return record.getFields().stream().filter(f -> "file".equals(f.getType())).collect(Collectors.toList());
		}

		private void writeFileData(Object object, ObjectSchema.Field field, String filename) {
			try {
				if (filesDir == null) {
					filesDir = new File(getJobDir(job), "files");
				}

				File srcFile = null;
				File destFile = new File(filesDir, filename);
				if (field.getType().equals("file")) {
					String fileVar = field.getFile();
					if (StringUtils.isBlank(fileVar)) {
						fileVar = "documentFile";
					}

					srcFile = (File) getObject(object, fileVar);
				} else if (field.getType().equals("defile")) {
					Map<String, String> fcv = (Map<String, String>) getObject(object, field.getAttribute());
					String fileId = fcv.get("fileId");
					srcFile = new File(DeConfiguration.getInstance().fileUploadDir(), fileId);
				} else if (field.getType().equals("signature")) {
					String fileId = (String) getObject(object, field.getAttribute());
					srcFile = new File(DeConfiguration.getInstance().fileUploadDir(), fileId);
				}

				if (srcFile != null && srcFile.exists()) {
					FileUtils.copyFile(srcFile, destFile);
				} else {
					FileUtils.write(destFile, "File does not exists", Charset.defaultCharset());
				}
			} catch (IOException ioe) {
				throw new RuntimeException("Error writing to file", ioe);
			}
		}

		private void generateOutputFile() {
			try {
				openReader(getRawDataFile(job));
				openWriter(getOutputCsvFile(job));

				write(getHeaderRow("", "", job.getSchema().getRecord()));

				String[] row;
				while ((row = nextLine()) != null) {
					Map<String, String> rowValueMap = new HashMap<>();
					for (int i = 0; i < row.length; i += 2) {
						rowValueMap.put(row[i], row[i + 1]);
					}

					write(getDataRow("", "", job.getSchema().getRecord(), rowValueMap));
				}

				flush();
			} finally {
				closeReader();
				closeWriter();
			}
		}

		private List<String> getHeaderRow(String namePrefix, String captionPrefix, ObjectSchema.Record record) {
			List<String> row = new ArrayList<>();
			for (Object recField : record.getOrderedFields()) {
				row.addAll(getHeaderNames(namePrefix, captionPrefix, recField));
			}

			return row;
		}

		private List<String> getHeaderNames(String namePrefix, String captionPrefix, Object recField) {
			List<String> names = new ArrayList<>();
			if (recField instanceof ObjectSchema.Field) {
				ObjectSchema.Field field = (ObjectSchema.Field) recField;
				String caption = captionPrefix + field.getCaption();
				if (field.isMultiple()) {
					int count = getFieldCount(namePrefix + field.getAttribute());
					for (int i = 1; i <= count; ++i) {
						names.add(caption + "#" + i);
					}
				} else {
					names.add(caption);
				}
			} else if (recField instanceof ObjectSchema.Record) {
				ObjectSchema.Record subRecord = (ObjectSchema.Record) recField;
				String srName = namePrefix + subRecord.getAttribute();
				String srCaption = captionPrefix;
				if (StringUtils.isNotBlank(subRecord.getCaption())) {
					srCaption += subRecord.getCaption() + "#";
				}

				if (subRecord.isMultiple()) {
					int count = getFieldCount(srName);
					for (int i = 1; i <= count; ++i) {
						names.addAll(getHeaderRow(srName + ".", srCaption + i + "#", subRecord));
					}
				} else {
					names.addAll(getHeaderRow(srName + ".", srCaption, subRecord));
				}
			}

			return names;
		}

		private List<String> getDataRow(String namePrefix, String captionPrefix, ObjectSchema.Record record, Map<String, String> valueMap) {
			List<String> row = new ArrayList<>();
			for (Object recField : record.getOrderedFields()) {
				row.addAll(getColumnValues(namePrefix, captionPrefix, recField, valueMap));
			}

			return row;
		}

		private List<String> getColumnValues(String namePrefix, String captionPrefix, Object recField, Map<String, String> valueMap) {
			List<String> values = new ArrayList<>();
			if (recField instanceof ObjectSchema.Field) {
				ObjectSchema.Field field = (ObjectSchema.Field) recField;
				String caption = captionPrefix + field.getCaption();
				if (field.isMultiple()) {
					int count = getFieldCount(namePrefix + field.getAttribute());
					for (int i = 1; i <= count; ++i) {
						String key = caption + "#" + i;
						values.add(valueMap.get(key));
					}
				} else {
					values.add(valueMap.get(caption));
				}
			} else if (recField instanceof ObjectSchema.Record) {
				ObjectSchema.Record subRecord = (ObjectSchema.Record) recField;
				String srName = namePrefix + subRecord.getAttribute();
				String srCaption = captionPrefix;
				if (StringUtils.isNotBlank(subRecord.getCaption())) {
					srCaption += subRecord.getCaption() + "#";
				}

				if (subRecord.isMultiple()) {
					int count = getFieldCount(srName);
					for (int i = 1; i <= count; ++i) {
						values.addAll(getDataRow(srName + ".", srCaption + i + "#", subRecord, valueMap));
					}
				} else {
					values.addAll(getDataRow(srName + ".", srCaption, subRecord, valueMap));
				}
			}

			return values;
		}

		private void updateFieldCount(String field, int count) {
			Integer value = fieldInstances.get(field);
			if (value == null || count > value) {
				fieldInstances.put(field, count == 0 ? 1 : count);
			}
		}

		private int getFieldCount(String field) {
			Integer count = fieldInstances.get(field);
			return count == null ? 1 : count;
		}

		private void openWriter(String file) {
			writer = CsvFileWriter.createCsvFileWriter(new File(file));
		}

		private void write(List<String> row) {
			writer.writeNext(row.toArray(new String[0]));
			++written;

			if (written % 50 == 0) {
				flush();
			}
		}

		private void flush() {
			try {
				writer.flush();
			} catch (Exception e) {
				throw new RuntimeException("Error writing to CSV file", e);
			}
		}

		private void closeWriter() {
			IOUtils.closeQuietly(writer);
			written = 0;
		}

		private void openReader(String file) {
			reader = CsvFileReader.createCsvFileReader(file, false);
		}

		private String[] nextLine() {
			return reader.next() ? reader.getRow() : null;
		}

		private void closeReader() {
			IOUtils.closeQuietly(reader);
		}

		private String getString(Object object, ObjectSchema.Field field) {
			try {
				String result;

				Object value = getObject(object, field.getAttribute());
				if ("dateOnly".equals(field.getType())) {
					result = getDateOnlyString(value);
				} else if ("date".equals(field.getType())) {
					result = getDateString(value);
				} else if ("datetime".equals(field.getType())) {
					result = getDateTimeString(value);
				} else if ("defile".equals(field.getType()) && value instanceof Map) {
					Map<String, String> fcv = (Map<String, String>)value;
					result = fcv.get("filename");
				} else {
					result = Objects.toString(value, "");
				}

				return result;
			} catch (Exception e) {
				throw new IllegalArgumentException("Error obtaining value of property: " + field.getAttribute(), e);
			}
		}

		private Object getObject(Object object, String propertyName) {
			try {
				return BeanUtilsBean2.getInstance().getPropertyUtils().getProperty(object, propertyName);
			} catch (Exception e) {
				throw new IllegalArgumentException("Error obtaining value of property: " + propertyName, e);
			}
		}

		private Collection<Object> getCollection(Object object, String propertyName) {
			try {
				Object value = getObject(object, propertyName);
				if (value == null) {
					return Collections.emptyList();
				} else if (value.getClass().isArray()) {
					return Stream.of((Object[]) value).collect(Collectors.toList());
				} else if (value instanceof Collection) {
					return (Collection<Object>)value;
				} else {
					logger.error("Unknown collection type for " + propertyName + ": " + value.getClass());
					return Collections.emptyList();
				}
			} catch (Exception e) {
				throw new IllegalArgumentException("Error obtaining value of property: " + propertyName, e);
			}
		}

		private String getDateOnlyString(Object input) {
			return getFormattedDateTimeString(dateOnlyFormatter, input);
		}

		private String getDateString(Object input) {
			return getFormattedDateTimeString(dateFormatter, input);
		}

		private String getDateTimeString(Object input) {
			return getFormattedDateTimeString(dateTimeFormatter, input);
		}

		private String getFormattedDateTimeString(SimpleDateFormat formatter, Object input) {
			Date dateObj = null;
			if (input instanceof String) {
				try {
					dateObj = new Date(Long.parseLong((String) input));
				} catch (Exception e) {
					return (String) input;
				}
			} else if (input instanceof Number) {
				dateObj = new Date(((Number) input).longValue());
			} else if (input instanceof Date) {
				dateObj = (Date) input;
			}

			return dateObj == null ? null : formatter.format(dateObj);
		}

		private void updateRecordsCount(long recordsCnt) {
			job.setTotalRecords(recordsCnt);
			saveJob();
		}

		private void completed() {
			job.setEndTime(Calendar.getInstance().getTime());
			job.markCompleted();
			saveJob();
		}

		private void failed() {
			job.setEndTime(Calendar.getInstance().getTime());
			job.markFailed();
			saveJob();
		}

		@PlusTransactional
		private List<? extends Object> getObjects() {
			return generator.apply(job);
		}

		@PlusTransactional
		private void saveJob() {
			exportJobDao.saveOrUpdate(job);
		}
	}

	private String getRawDataFile(ExportJob job) {
		return getJobDir(job) + File.separator + "raw-data.csv";
	}

	private String getOutputCsvFile(ExportJob job) {
		return getJobDir(job) + File.separator + "output.csv";
	}

	private String getOutputFile(ExportJob job) {
		File zipFile = new File(getJobDir(job), "output.zip");
		if (zipFile.exists()) {
			return zipFile.getAbsolutePath();
		}

		return getOutputCsvFile(job);
	}

	private void cleanupFiles(ExportJob job) {
		try {
			new File(getRawDataFile(job)).delete();
			new File(getOutputCsvFile(job)).delete();
			FileUtils.deleteDirectory(new File(getJobDir(job), "files"));
		} catch (Exception e) {
			logger.error("Error cleaning up export job output directory", e);
		}
	}

	private void generateOutputZip(ExportJob job) throws IOException {
		String jobDir = getJobDir(job);
		File jobZipFile = new File(jobDir + ".zip");
		IoUtil.zipFiles(jobDir, jobZipFile.getAbsolutePath(), Collections.singletonList(getRawDataFile(job)));
		FileUtils.moveFile(jobZipFile, new File(jobDir, "output.zip"));
	}

	private void sendJobStatusNotification(ExportJob job) {
		if (job.isDisableNotifs()) {
			return;
		}

		String entityName = getEntityName(job);

		String [] subjParams = {job.getId().toString(), entityName};

		Map<String, Object> props = new HashMap<>();
		props.put("job", job);
		props.put("entityName", entityName);
		props.put("status", getMsg("export_statuses_" + job.getStatus().name().toLowerCase()));
		props.put("$subject", subjParams);
		props.put("timeTaken", TimeUnit.MILLISECONDS.toMinutes(job.getEndTime().getTime() - job.getCreationTime().getTime()));

		String[] rcpts = {job.getCreatedBy().getEmailAddress()};
		EmailUtil.getInstance().sendEmail(JOB_STATUS_EMAIL_TMPL, rcpts, null, props);
	}

	private String getEntityName(ExportJob job) {
		String entityName;

		if (job.getName().equals(EXTENSIONS)) {
			entityName = job.getParams().get("formName") + " (" + job.getParams().get("entityType") + ")";
		} else {
			entityName = getMsg("export_entities_" + job.getName(), job.param("formName"));
		}

		return entityName;
	}


	private String getMsg(String key, Object ... params) {
		return MessageUtil.getInstance().getMessage(key, params);
	}


	private String getJobDir(ExportJob job) {
		File dataDir = new File(ConfigUtil.getInstance().getDataDir());
		return dataDir.getAbsolutePath() + File.separator +
			"export-jobs" + File.separator + job.getId();
	}

	private static final String JOB_STATUS_EMAIL_TMPL = "export_job_status_notif";

	private static final String ID_ATTR = "id";

	private static final String EXTENSIONS = "extensions";
}
