package krishagni.catissueplus.upgrade;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.sql.DataSource;


import com.krishagni.catissueplus.core.common.util.LogUtil;

import au.com.bytecode.opencsv.CSVWriter;
import edu.common.dynamicextensions.domain.nui.UserContext;
import edu.common.dynamicextensions.ndao.JdbcDao;
import edu.common.dynamicextensions.ndao.JdbcDaoFactory;
import edu.common.dynamicextensions.ndao.ResultExtractor;
import edu.common.dynamicextensions.nutility.IoUtil;


public class MigrateForms {

	private static final LogUtil logger = LogUtil.getLogger(MigrateForms.class);
	
	public static void main(String[] args) 
	throws Exception {
		if (args.length == 0) {
			logger.error("Requires admin username as input. Exiting migration");
			return;
		}
		
		Properties prop = UpgradeUtil.loadInstallProps();
		DataSource ds = DbUtil.getDataSource(prop);
		UpgradeUtil.initializeDE(prop, ds);
		
		if (areLegacyFormsMigrated()) {
			logger.info("Forms have been already migrated to V4 (OS v1.0), skipping form migration!");
			return;
		}
		
		UserContext usrCtx = UpgradeUtil.getUserCtx(args[0]); 
		if (usrCtx == null) {
			logger.error("No active user found! Aborting form migration! Please provide an active username in " + 
					" parameter e.g. -Dusername=\"admin@admin.com\"");
			return;
		} 
		
		Date startTime = Calendar.getInstance().getTime();
		logger.info("Migration start time: " + startTime);
		Thread.sleep(5000);
		Map<Long, List<FormInfo>> containerInfo  = getAllFormsInfo();	
				
		migrateContainers(containerInfo, usrCtx);

		Date endTime = Calendar.getInstance().getTime();
		logger.info("Migration end time: " + endTime);
		logger.info("Total time for migration: " + 
				(endTime.getTime() - startTime.getTime()) / (1000 * 60) + " minutes");		
	}

	private static void migrateContainers(Map<Long, List<FormInfo>> containerInfo, UserContext usrCtx) 
	throws IOException {
		CSVWriter recordsLog = new CSVWriter(new FileWriter(DE_FORMS_RECORD_LOG));
		CSVWriter tabLog = new CSVWriter(new FileWriter(DE_FORMS_OTAB_LOG));
		
		int count = 0;		
		for (Entry<Long, List<FormInfo>> entry : containerInfo.entrySet()) {
			try {
//				MigrateForm migrateForm = new MigrateForm(usrCtx, recordsLog);
//				migrateForm.migrateForm(entry.getKey(), entry.getValue());
//				logObsoleteTables(tabLog, migrateForm);
				++count;
				
				logger.info("Number of forms migrated till now: " + count);
			} catch (Exception e) {
				logger.error("Error migrating container: " + entry.getKey(), e);
			}	
		}
		
		IoUtil.close(recordsLog);
		IoUtil.close(tabLog);
		logger.info("finished migrating " + count + " forms");		
	}
	
	private static boolean areLegacyFormsMigrated() { 
		JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
		
		Integer formsCount = jdbcDao.getResultSet(FORMS_COUNT_SQL, null,
				new ResultExtractor<Integer>() {
					@Override
					public Integer extract(ResultSet rs) throws SQLException {
						return rs.next() ? rs.getInt(1) : null;
					}
				});
		
		logger.info("Found " + formsCount + " existing forms in DYEXTN_CONTAINERS!");
		return formsCount > 0;
	}
	
	private static Map<Long, List<FormInfo>> getAllFormsInfo() {
		try {
			JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
			return jdbcDao.getResultSet(GET_CONTAINER_IDS_SQL, null,  new ResultExtractor<Map<Long, List<FormInfo>>>() {
				@Override
				public Map<Long, List<FormInfo>> extract(ResultSet rs) throws SQLException {
					return extractFormInfo(rs);
				}				
			});
		} catch (Exception e) {
			throw new RuntimeException("Error while retrieving info of all forms ",e);
		}
	}
	
	protected static Map<Long, List<FormInfo>> extractFormInfo(ResultSet rs) throws SQLException {
		Map<Long, List<FormInfo>> containerInfo = new HashMap<Long, List<FormInfo>>();
		
		while(rs.next()) {
			// rs(1)               : containerId
			// rs(2)               : Collection protocol id
			// rs(3)               : Static entity id (Exists only for default forms)
			// rs(4)               : Old form fontext id
			// rs(5), rs(6), rs(7) : Determines to which entity type formContext belongs						
			FormInfo info = new FormInfo();
			info.setCpId((Long)rs.getLong(2) > 0 ? rs.getLong(2) : -1);
			info.setDefaultForm((Long)rs.getLong(3) > 0 ? true : false);
			info.setOldFormCtxId(rs.getLong(4));
			info.setParticipant((Long)rs.getLong(5) > 0 ? true : false);
			info.setScg((Long)rs.getLong(6) > 0 ? true : false);
			info.setSpecimen((Long)rs.getLong(7) > 0 ? true : false);
			
			if (info.getEntityType() == null) {
				continue;
			}
			
			List<FormInfo> formInfos = containerInfo.get(rs.getLong(1));
			if (formInfos == null) {
				formInfos = new ArrayList<FormInfo>();
				containerInfo.put(rs.getLong(1), formInfos);
			} 
			
			formInfos.add(info);
		}
		return containerInfo;
	}
	
//	private static void logObsoleteTables(CSVWriter logWriter, MigrateForm migrator) 
//	throws IOException {
//		logWriter.writeNext(new String[] {
//				migrator.getFormCaption(),
//				migrator.getObsoleteTables()
//		});
//		logWriter.flush();
//	}
	
	private static final String GET_CONTAINER_IDS_SQL = 
			"select " +
			"	c.identifier, cfc.collection_protocol_id, map.static_entity_id , fc.identifier, count(pre.identifier), " +
			"	count(scg_re.identifier), count(sp_re.identifier) " +
			"from dyextn_container c " + 
			"	inner join dyextn_abstract_form_context fc on fc.container_id = c.identifier " +
			"	inner join dyextn_abstract_record_entry re on re.abstract_form_context_id = fc.identifier " + 
			"	left join catissue_participant_rec_ntry pre on pre.identifier = re.identifier " +
			"	left join catissue_scg_rec_ntry scg_re on scg_re.identifier = re.identifier " + 
			"	left join catissue_specimen_rec_ntry sp_re on sp_re.identifier = re.identifier " +
			"	left join catissue_cp_studyformcontext cfc on cfc.study_form_context_id = fc.identifier " + 
			"	left join dyextn_entity_map map on map.container_id = c.identifier " +
			"group by c.identifier, cfc.collection_protocol_id, fc.identifier, map.static_entity_id" ;
	
	private static final String FORMS_COUNT_SQL = 
				"select count(identifier) from dyextn_containers";
	
	private static final String DE_FORMS_RECORD_LOG = "de-forms-record-mapping.csv";
	
	private static final String DE_FORMS_OTAB_LOG = "de-forms-obsolete-tables.csv";
}
