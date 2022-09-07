package com.krishagni.catissueplus.core.init;

import java.io.File;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;


import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.openspecimen.core.migration.domain.Migration.Status;
import com.krishagni.openspecimen.core.migration.domain.Migration.Version;
import com.krishagni.openspecimen.core.migration.events.MigrationDetail;
import com.krishagni.openspecimen.core.migration.services.MigrationService;

public class MigrateSurgicalPathologyReports implements InitializingBean {

	private static final LogUtil logger = LogUtil.getLogger(MigrateSurgicalPathologyReports.class);

	private PlatformTransactionManager txnMgr;
	
	private DaoFactory daoFactory;
	
	private ConfigurationService cfgSvc;
	
	private Properties migrationProps;
	
	private MigrationService migrationSvc;
	
	private JdbcTemplate jdbcTemplate;
	
	private Properties appProps = new Properties();
	
	private boolean mysql = true;
	
	private boolean identifiedReports = true;
	
	public void setTxnMgr(PlatformTransactionManager txnMgr) {
		this.txnMgr = txnMgr;
	}

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setMigrationProps(Properties migrationProps) {
		this.migrationProps = migrationProps;
	}

	public void setMigrationSvc(MigrationService migrationSvc) {
		this.migrationSvc = migrationSvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public void setAppProps(Properties appProps) {
		this.appProps = appProps;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		migrateFromFiles();
		mysql = "MYSQL".equalsIgnoreCase(appProps.getProperty(DB_TYPE_PROP));
		migrateFromDatabase(!identifiedReports);
		migrateFromDatabase(identifiedReports);
	}
	
	private void migrateFromFiles() {
		String srcDirPath = migrationProps.getProperty("visit.old_spr_dir");
		if (StringUtils.isBlank(srcDirPath)) {
			logger.info("Old spr directory path not spcified, either no need of migration for this version or missed it.");
			return;
		}
		
		MigrationDetail migrationDetail = migrationSvc.getMigration(
				MIGRATION_NAME, 
				Version.OS_V11.name(),
				Version.OS_V21.name());
		if (migrationDetail != null && migrationDetail.getStatus().equals(Status.SUCCESS)) {
			logger.info("Migration was done successfully.");
			return;
		}
		
		Date startTime = Calendar.getInstance().getTime();
		logger.info("Surgical pathology reports migration from version OS v1.1 to OS v2.1 start time: " + startTime);
		
		File srcDir = new File(srcDirPath);
		List<File[]> fileArray = splitArray(srcDir.listFiles(), 100);
		
		Status status = Status.SUCCESS;
		TransactionTemplate txnTmpl = new TransactionTemplate(txnMgr);
		try {
			for (final File[] files : fileArray) {
				Boolean success = txnTmpl.execute(new TransactionCallback<Boolean>() {
					@Override
					public Boolean doInTransaction(TransactionStatus status) {
						try {
							migrateSprs(files);
						} catch (Exception e) {
							logger.error("Surgical pathology report migration fails with error", e);
							return false;
						}
						return true;
					}
				});
				
				if (!success) {
					status = Status.FAIL;
					break;
				}
			} 			
		} catch(Exception e) {
			status = Status.FAIL;
			logger.error("Surgical pathology report migration fails with error", e);
		} finally {
			saveMigration(migrationDetail, status);
			Date endTime = Calendar.getInstance().getTime();
			logger.info("Surgical pathology report migration for version OS v1.1 to OS v2.1 end time: " + endTime);
			logger.info("Total time for migration: " + 
					(endTime.getTime() - startTime.getTime()) / (1000 * 60) + " minutes");
		}
	}
	
	private void migrateFromDatabase(final boolean identifiedReports) {
		Date startTime = Calendar.getInstance().getTime();
		logger.info("Migration from static table start time: " + startTime);
		
		int totalMigratedReportsCnt = 0;
		final int maxResult = 100;
		boolean moreRecords = true;
		while (moreRecords) {
			TransactionTemplate txnTmpl = new TransactionTemplate(txnMgr);
			Integer migrtedReportsCnt = 
				txnTmpl.execute(new TransactionCallback<Integer>() {
					@Override
					public Integer doInTransaction(TransactionStatus status) {
						final List<SprDetail> sprDetails  = getSprDetailsFromTable(getReportsSql(identifiedReports), maxResult);
						if (CollectionUtils.isEmpty(sprDetails)) {
							logger.info("No surgical pathology reports in static table to migrate from static table");
							return 0;
						}
						
						for (final SprDetail detail : sprDetails) {
							OutputStream outputStream = null;
							try {
								File sprFile = getSprFile(detail.getVisitId());
								FileUtils.writeStringToFile(sprFile, detail.getReportContent());
								
								String sprName = detail.getVisitName() + ".txt"; 
								boolean sprLocked = SPR_LOCKED.equals(detail.getActivityStatus());
								
								jdbcTemplate.update(UPDATE_SPR_DOC_NAME_AND_LOCK_STATUS_SQL, 
										new Object[] {sprName, sprLocked, detail.getVisitId()});
								
								jdbcTemplate.update(DISABLE_OLD_MIGRATED_SPR_SQL, detail.getReportId());
							} catch (Exception e) {
								logger.error("Error while migrating records from static table: ", e);
							} finally {
								IOUtils.closeQuietly(outputStream);
							}
						} 
						return sprDetails.size();
					}
				});
			
			totalMigratedReportsCnt = totalMigratedReportsCnt + migrtedReportsCnt;
			logger.info("Number of reports migrated from static table, till now: " + totalMigratedReportsCnt);
			
			if (migrtedReportsCnt < maxResult) {
				moreRecords = false;
			}
		}
		
		Date endTime = Calendar.getInstance().getTime();
		logger.info("Migration from static table end time: " + endTime);
		logger.info("Total time for migration from static table: " + 
				(endTime.getTime() - startTime.getTime()) / (1000 * 60) + " minutes");		
	}

	private void migrateSprs(final File[] files) throws Exception {
		for (File srcFile : files) {
			try {
				if (srcFile.isDirectory()) {
					continue;
				}
				
				String[] arr = srcFile.getName().split("_", 2);
				Long visitId = Long.parseLong(arr[0]);
				String sprName = arr[1];
				
				Visit visit = daoFactory.getVisitsDao().getById(visitId);
				if (visit == null || visit.getActivityStatus().equals("Disabled")) {
					logger.info("Visit does not exist with idenifier: " + visitId);
					continue;
				}
				
				String sprText = Utility.getFileText(srcFile);
				FileUtils.writeStringToFile(getSprFile(visitId), sprText, (String) null, false);
				visit.setSprName(sprName);
				logger.info("Surgical pathology report migrated successfully for visit id "+ visitId);
			} catch (Exception e) {
				logger.error("Error while migrating file:" + srcFile.getName(), e);
				throw e;
			}
		}
	}

	private void saveMigration(MigrationDetail detail, Status status) {
		if (detail == null) {
			detail = new MigrationDetail();
			detail.setName(MIGRATION_NAME);
			detail.setVersionFrom(Version.OS_V11);
			detail.setVersionTo(Version.OS_V21);
		}
		
		detail.setStatus(status);
		detail.setDate(new Date());
		migrationSvc.saveMigration(detail);
	}
	
	private static List<File[]> splitArray(File[] array, int chunkSize) {
		List<File[]> result = new ArrayList<File[]>();
		int noOfChunks = array.length / chunkSize;
		     
		for (int chunk = 0; chunk < noOfChunks; ++chunk) {
			int startIdx = chunk * chunkSize;
			result.add(Arrays.copyOfRange(array, startIdx, startIdx + chunkSize));
		}
		
		int remaining  = array.length % chunkSize;
		if (remaining > 0) {
			result.add(Arrays.copyOfRange(array, array.length - remaining, array.length));
		}
		
		return result;
	}
	
	private String getReportsSql(boolean identifiedReports) {
		if (mysql) {
			return identifiedReports ? GET_IDENTIFIED_SPR_DETAILS_SQL_MYSQL : GET_DEIDENTIFIED_SPR_DETAILS_SQL_MYSQL;
		} else {
			return identifiedReports ? GET_IDENTIFIED_SPR_DETAILS_SQL_ORACLE : GET_DEIDENTIFIED_SPR_DETAILS_SQL_ORACLE;
		}
	}

	private List<SprDetail> getSprDetailsFromTable(final String getSprDetailsSql, final int maxResult) {
		return jdbcTemplate.query(
				getSprDetailsSql,
				new Object[] {maxResult},
				new RowMapper<SprDetail>() {
					public SprDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
						SprDetail sprDetail = new SprDetail();
						sprDetail.setVisitId(rs.getLong("visitId"));
						sprDetail.setVisitName(rs.getString("visitName"));
						sprDetail.setActivityStatus(rs.getString("activityStatus"));
						sprDetail.setReportContent(rs.getString("reportContent"));
						sprDetail.setReportId(rs.getLong("reportId"));
						return sprDetail;
					}
			 });					
	}
	
	private File getSprFile(Long visitId) {
		String path = cfgSvc.getStrSetting(
				ConfigParams.MODULE, 
				ConfigParams.SPR_DIR, 
				getDefaultVisitSprDir());
		path = path + File.separator + visitId + File.separator + "spr.txt";
		
		return new File(path);
	}
	
	private String getDefaultVisitSprDir() {
		return cfgSvc.getDataDir() + File.separator + "visit-sprs";
	}
	
	private static final String GET_DEIDENTIFIED_SPR_DETAILS_SQL_MYSQL =
			"select "+ 
			  "pr.identifier reportId, pr.activity_status activityStatus, " +
			  "dr.scg_id visitId, scg.name visitName, rc.report_data reportContent " + 
			"from " +
			  "catissue_deidentified_report dr " + 
			  "join catissue_pathology_report pr on dr.identifier = pr.identifier " + 
			  "join catissue_report_textcontent rtc on rtc.report_id = pr.identifier " + 
			  "join catissue_report_content rc on rc.identifier = rtc.identifier " +
			  "join catissue_specimen_coll_group scg on dr.scg_id = scg.identifier " +
			"where " +
			  "pr.activity_status <> 'Disabled' and scg.activity_status <> 'Disabled' limit ?";
	
	private static final String GET_DEIDENTIFIED_SPR_DETAILS_SQL_ORACLE =
			"select "+ 
			  "pr.identifier reportId, pr.activity_status activityStatus, " +
			  "dr.scg_id visitId, scg.name visitName, rc.report_data reportContent " + 
			"from " +
			  "catissue_deidentified_report dr " + 
			  "join catissue_pathology_report pr on dr.identifier = pr.identifier " + 
			  "join catissue_report_textcontent rtc on rtc.report_id = pr.identifier " + 
			  "join catissue_report_content rc on rc.identifier = rtc.identifier " +
			  "join catissue_specimen_coll_group scg on dr.scg_id = scg.identifier " +
			"where " +
			  "pr.activity_status <> 'Disabled' and scg.activity_status <> 'Disabled' and rownum <= ?";
	
	private static final String GET_IDENTIFIED_SPR_DETAILS_SQL_MYSQL =
			"select "+ 
			  "pr.identifier reportId, pr.activity_status activityStatus, " +
			  "dr.scg_id visitId, scg.name visitName, rc.report_data reportContent " + 
			"from " +
			  "catissue_identified_report dr " + 
			  "join catissue_pathology_report pr on dr.identifier = pr.identifier " + 
			  "join catissue_report_textcontent rtc on rtc.report_id = pr.identifier " + 
			  "join catissue_report_content rc on rc.identifier = rtc.identifier " +
			  "join catissue_specimen_coll_group scg on dr.scg_id = scg.identifier " +
			"where " +
			  "pr.activity_status <> 'Disabled' and scg.activity_status <> 'Disabled' and scg.spr_name is null limit ?";
	
	private static final String GET_IDENTIFIED_SPR_DETAILS_SQL_ORACLE =
			"select "+ 
			  "pr.identifier reportId, pr.activity_status activityStatus, " +
			  "dr.scg_id visitId, scg.name visitName, rc.report_data reportContent " + 
			"from " +
			  "catissue_identified_report dr " + 
			  "join catissue_pathology_report pr on dr.identifier = pr.identifier " + 
			  "join catissue_report_textcontent rtc on rtc.report_id = pr.identifier " + 
			  "join catissue_report_content rc on rc.identifier = rtc.identifier " +
			  "join catissue_specimen_coll_group scg on dr.scg_id = scg.identifier " +
			"where " +
			  "pr.activity_status <> 'Disabled' and scg.activity_status <> 'Disabled' and scg.spr_name is null and rownum <= ?";
	
	private static final String UPDATE_SPR_DOC_NAME_AND_LOCK_STATUS_SQL =
			"update " +
			  "catissue_specimen_coll_group " +
			"set " +
			  "spr_name = ? , spr_locked = ? " +
			"where " +
			  "identifier = ? " ;
	
	private static final String DISABLE_OLD_MIGRATED_SPR_SQL = 
			"update " +
			  "catissue_pathology_report " +
			"set " +
			  "activity_status = 'Disabled' " +
			"where "+
			  "identifier = ?" ;
	
	private static final String SPR_LOCKED = "Locked";
	
	private static final String MIGRATION_NAME = "Surgical Pathology Reports";
	
	private static final String DB_TYPE_PROP = "database.type";

	
	private class SprDetail {
		private Long visitId;
		 
		private String visitName;
		 
		private Long reportId;
		 
		private String activityStatus;
		 
		private String reportContent;
		 
		public Long getVisitId() {
			return visitId;
		}

		public void setVisitId(Long visitId) {
			this.visitId = visitId;
		}

		public String getVisitName() {
			return visitName;
		}

		public void setVisitName(String visitName) {
			this.visitName = visitName;
		}

		public Long getReportId() {
			return reportId;
		}

		public void setReportId(Long reportId) {
			this.reportId = reportId;
		}

		public String getActivityStatus() {
			return activityStatus;
		}

		public void setActivityStatus(String activityStatus) {
			this.activityStatus = activityStatus;
		}

		public String getReportContent() {
			return reportContent;
		}

		public void setReportContent(String reportContent) {
			this.reportContent = reportContent;
		}
	}

}
