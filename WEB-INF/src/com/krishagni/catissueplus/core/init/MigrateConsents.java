package com.krishagni.catissueplus.core.init;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;


import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.openspecimen.core.migration.domain.Migration.Status;
import com.krishagni.openspecimen.core.migration.domain.Migration.Version;
import com.krishagni.openspecimen.core.migration.events.MigrationDetail;
import com.krishagni.openspecimen.core.migration.services.MigrationService;

public class MigrateConsents implements InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(MigrateConsents.class);
	
	private ConfigurationService cfgSvc;
	
	private Properties migrationProps;
	
	private MigrationService migrationSvc;
	
	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setMigrationProps(Properties migrationProps) {
		this.migrationProps = migrationProps;
	}
	
	public void setMigrationSvc(MigrationService migrationSvc) {
		this.migrationSvc = migrationSvc;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		migrate();
	}
	
	private void migrate() {
		String srcDirPath = migrationProps.getProperty("participant.old_consents_dir");
		if (StringUtils.isBlank(srcDirPath)) {
			logger.info("Old consent directory path not spcified, either no need of migration for this version or missed it.");
			return;
		}
		
		MigrationDetail detail = migrationSvc.getMigration(
				MIGRATION_NAME,
				Version.OS_V11.name(),
				Version.OS_V21.name());
		
		if (detail != null && detail.getStatus().equals(Status.SUCCESS)) {
			logger.info("Migration was done successfully. No consents to migrate");
			return;
		}
		
		Date startTime = Calendar.getInstance().getTime();
		logger.info("Consents migration from version OS v1.1 to OS v2.1 start time: " + startTime);
		
		File srcDir = new File(srcDirPath);
		File dstDir = new File(getConsentDirPath());
		Status status = Status.SUCCESS;
		try {
			FileUtils.copyDirectory(srcDir, dstDir);
		} catch (IOException e) {
			status = Status.FAIL;
			logger.info("Error while copying files from " + srcDir.getAbsolutePath() + " to " + dstDir.getAbsolutePath());
		} finally {
			saveMigration(detail, status);
			Date endTime = Calendar.getInstance().getTime();
			logger.info("Consent migration from version OS v1.1 to OS v2.1 end time: " + endTime);
			logger.info("Total time for migration: " + 
					(endTime.getTime() - startTime.getTime()) / (1000 * 60) + " minutes");
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
	
	private String getConsentDirPath() {
		String defaultConsentDir = cfgSvc.getDataDir() + File.separator + "participant-consents";
		return cfgSvc.getStrSetting(
				ConfigParams.MODULE,
				"participant_consent_dir", 
				defaultConsentDir);
	}
	
	private static final String MIGRATION_NAME = "Participant Consents";
	
}
