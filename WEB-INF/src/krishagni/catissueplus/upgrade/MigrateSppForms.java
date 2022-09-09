//package krishagni.catissueplus.upgrade;
//
//import java.io.FileWriter;
//import java.io.IOException;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.Properties;
//
//import javax.sql.DataSource;
//
//
//import com.krishagni.catissueplus.core.common.util.LogUtil;
//
//import au.com.bytecode.opencsv.CSVWriter;
//import edu.common.dynamicextensions.domain.nui.UserContext;
//import edu.common.dynamicextensions.ndao.JdbcDaoFactory;
//import edu.common.dynamicextensions.ndao.ResultExtractor;
//import edu.common.dynamicextensions.nutility.IoUtil;
//
//
//public class MigrateSppForms {
//	private static final LogUtil logger = LogUtil.getLogger(MigrateSppForms.class);
//
//	public static void main(String[] args)
//	throws Exception {
//		LogUtil.setInfoLevel();
//
//		if (args.length == 0) {
//			logger.error("Requires admin username as input. Exiting SPP migration");
//			return;
//		}
//
//		Properties prop = UpgradeUtil.loadInstallProps();
//		DataSource ds = DbUtil.getDataSource(prop);
//		UpgradeUtil.initializeDE(prop, ds);
//
//		UserContext usrCtx = UpgradeUtil.getUserCtx(args[0]);
//		if (usrCtx == null) {
//			logger.error("No active user found! Aborting form migration! Please provide an active username in " +
//					" parameter e.g. -Dusername=\"admin@admin.com\"");
//			return;
//		}
//
//		Date startTime = Calendar.getInstance().getTime();
//		logger.info("SPP migration start time: " + startTime);
//		Thread.sleep(5000);
//		Map<Long, List<FormInfo>> containerInfo  = getSppFormsInfo();
//
//		migrateSppForms(containerInfo, usrCtx);
//
//		Date endTime = Calendar.getInstance().getTime();
//		logger.info("SPP migration end time: " + endTime);
//		logger.info("Total time for SPP migration: " +
//				(endTime.getTime() - startTime.getTime()) / (1000 * 60) + " minutes");
//	}
//
//	private static void migrateSppForms(Map<Long, List<FormInfo>> containerInfo, UserContext usrCtx)
//	throws IOException {
//		CSVWriter recordsLog = new CSVWriter(new FileWriter(SPP_FORMS_RECORD_LOG));
//		CSVWriter tabsLog = new CSVWriter(new FileWriter(SPP_FORMS_OTAB_LOG));
//
//		int count = 0;
//		for (Entry<Long, List<FormInfo>> entry : containerInfo.entrySet()) {
//			try {
////				MigrateSppForm migrateSppForm = new MigrateSppForm(usrCtx, recordsLog);
////				migrateSppForm.migrateForm(entry.getKey(), entry.getValue());
////				logObsoleteTables(tabsLog, migrateSppForm);
////				++count;
//
//				logger.info("Number of SPP forms migrated till now: " + count);
//			} catch (Exception e) {
//				logger.error("Error migrating container: " + entry.getKey(), e);
//			}
//		}
//
//		IoUtil.close(recordsLog);
//		IoUtil.close(tabsLog);
//		logger.info("Finished migrating " + count + " SPP forms");
//	}
//
//	private static Map<Long, List<FormInfo>> getSppFormsInfo() {
//		try {
//			return JdbcDaoFactory.getJdbcDao().getResultSet(
//					GET_SPP_CONTAINER_IDS_SQL,
//					null,
//					new ResultExtractor<Map<Long, List<FormInfo>>>() {
//						@Override
//						public Map<Long, List<FormInfo>> extract(ResultSet rs)
//						throws SQLException {
//							return extractFormInfo(rs);
//						}
//					});
//		} catch (Exception e) {
//			throw new RuntimeException("Error while retrieving info of all SPP forms ",e);
//		}
//	}
//
//	protected static Map<Long, List<FormInfo>> extractFormInfo(ResultSet rs) throws SQLException {
//		Map<Long, List<FormInfo>> containerInfo = new HashMap<Long, List<FormInfo>>();
//
//		while(rs.next()) {
//			// rs(1)               : containerId
//			// rs(2)               : Collection protocol id
//			// rs(3)               : Static entity id (Exists only for default forms)
//			// rs(4)               : Old form fontext id
//			// rs(5)               : SPP record entry count
//
//			List<FormInfo> formInfos = containerInfo.get(rs.getLong(1));
//			if (formInfos == null) {
//				formInfos = new ArrayList<FormInfo>();
//				containerInfo.put(rs.getLong(1), formInfos);
//			}
//
//			FormInfo info = new FormInfo();
//			info.setCpId((Long)rs.getLong(2) > 0 ? rs.getLong(2) : -1);
//			info.setDefaultForm((Long)rs.getLong(3) > 0 ? true : false);
//			info.setOldFormCtxId(rs.getLong(4));
//			info.setSpecimenEvent((Long)rs.getLong(5) > 0 ? true : false);
//
//			formInfos.add(info);
//		}
//
//		return containerInfo;
//	}
//
////	private static void logObsoleteTables(CSVWriter logWriter, MigrateForm migrator)
////	throws IOException {
////		logWriter.writeNext(new String[] {
////				migrator.getFormCaption(),
////				migrator.getObsoleteTables()
////		});
////
////		logWriter.flush();
////	}
//
//	private static final String GET_SPP_CONTAINER_IDS_SQL =
//			"select " +
//			"	c.identifier, cfc.collection_protocol_id, map.static_entity_id , fc.identifier, count(spe_re.identifier) " +
//			"from " +
//			"   dyextn_container c " +
//			"	inner join dyextn_abstract_form_context fc on fc.container_id = c.identifier " +
//			"	inner join dyextn_abstract_record_entry re on re.abstract_form_context_id = fc.identifier " +
//			"   left join catissue_action_application spe_re on spe_re.action_app_record_entry_id = re.identifier " +
//			"	left join catissue_cp_studyformcontext cfc on cfc.study_form_context_id = fc.identifier " +
//			"	left join dyextn_entity_map map on map.container_id = c.identifier " +
//			"group by " +
//			"   c.identifier, cfc.collection_protocol_id, fc.identifier, map.static_entity_id" ;
//
//	private static final String SPP_FORMS_RECORD_LOG = "de-spp-forms-record-mapping.csv";
//
//	private static final String SPP_FORMS_OTAB_LOG = "de-spp-forms-obsolete-tables.csv";
//}
