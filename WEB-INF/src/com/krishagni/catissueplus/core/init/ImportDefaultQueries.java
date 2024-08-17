
package com.krishagni.catissueplus.core.init;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.repository.UserDao;
import com.krishagni.catissueplus.core.common.events.ConfigSettingDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.QueryFolder;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;

public class ImportDefaultQueries implements InitializingBean {

	private static final LogUtil LOGGER = LogUtil.getLogger(ImportDefaultQueries.class);

	private PlatformTransactionManager txnMgr;

	private UserDao userDao;

	private DaoFactory daoFactory;

	private ConfigurationService cfgService;
	
	private User sysUser;

	public void setTxnMgr(PlatformTransactionManager txnMgr) {
		this.txnMgr = txnMgr;
	}

	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	public void setCfgService(ConfigurationService cfgService) {
		this.cfgService = cfgService;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		TransactionTemplate txnTmpl = new TransactionTemplate(txnMgr);
		txnTmpl.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRED);
		txnTmpl.execute(new TransactionCallback<Void>() {

			@Override
			public Void doInTransaction(TransactionStatus status) {
				try {
					importQueries();
					return null;
				} catch (Exception e) {
					status.setRollbackOnly();
					throw new RuntimeException(e);
				}
			}
		});
	}

	private void importQueries() throws Exception {
		sysUser = userDao.getSystemUser();
		Resource[] resources = new PathMatchingResourcePatternResolver().getResources(QUERIES_DIRECTORY + "/**");

		Set<SavedQuery> queries = new HashSet<SavedQuery>();
		for (Resource resource : resources) {
			String filename = QUERIES_DIRECTORY + "/" + resource.getFilename();
			LOGGER.info("Importing query from file: " + filename);
			String newDigest = Utility.getResourceDigest(filename);
			byte[] content = IOUtils.toByteArray(resource.getURI());
			Map<String, Object> result = daoFactory.getSavedQueryDao().getQueryChangeLogDetails(filename);
			
			if (result == null) {
				SavedQuery query = insertQuery(filename, content, newDigest);
				if(query.getId() != null){
					queries.add(query);

					if (resource.getFilename().equals(DISTRIBUTION_REPORT_QUERY)) {
						configureDistributionReportQuery(query);
					} else if (resource.getFilename().equals(SHIPMENT_REPORT_QUERY)) {
						configureShipmentReportQuery(query);
					} else if (resource.getFilename().equals(CONTAINER_SHIPMENT_REPORT_QUERY)) {
						configureContainerShipmentReportQuery(query);
					} else if (resource.getFilename().equals(SPECIMEN_KIT_REPORT_QUERY)) {
						configureKitReportQuery(query);
					} else if (resource.getFilename().equals(CONTAINER_SPECIMENS_REPORT_QUERY)) {
						configureContainerSpecimensReportQuery(query);
					} else if (resource.getFilename().equals(CART_SPECIMENS_REPORT_QUERY)) {
						configureCartSpecimensReportQuery(query);
					}
				}
			} else {
				Long queryId = ((Number)result.get("queryId")).longValue();
				String existingDigest = (String)result.get("md5Digest");

				if (existingDigest != null && existingDigest.equals(newDigest)) {
					LOGGER.info("No change found in file " + filename + " since last import");
					continue;
				}
				updateQuery(queryId, filename, content, newDigest);
			}
		}

		shareDefaultQueries(queries);
	}
	
	private SavedQuery insertQuery(String filename, byte[] queryContent, String md5Digest) {
		SavedQuery savedQuery = new SavedQuery();
		try {
			savedQuery.setQueryDefJson(new String(queryContent), true);
			savedQuery.setCreatedBy(sysUser);
			savedQuery.setLastUpdated(new Date());
			savedQuery.setLastUpdatedBy(sysUser);
			daoFactory.getSavedQueryDao().saveOrUpdate(savedQuery);
			insertChangeLog(filename, md5Digest, "INSERTED", savedQuery.getId());
		} catch (Exception e) {
			LOGGER.error("Error saving query definition from file: " + filename, e);
		}
		return savedQuery;
	}

	private void updateQuery(Long queryId, String filename, byte[] queryContent, String md5Digest) {
		try {
			SavedQuery savedQuery = daoFactory.getSavedQueryDao().getQuery(queryId);
			if(savedQuery != null){
				savedQuery.setQueryDefJson(new String(queryContent), true);
				savedQuery.setLastUpdated(Calendar.getInstance().getTime());
				savedQuery.setLastUpdatedBy(sysUser);
				daoFactory.getSavedQueryDao().saveOrUpdate(savedQuery);
			}
			
			insertChangeLog(filename, md5Digest, "UPDATED", queryId);
		} catch (Exception e) {
			LOGGER.error("Error updating query " + queryId + " using definition from file: " + filename, e);
		}
	}

	private void insertChangeLog(String filename, String md5Digest, String status, Long id) {
		daoFactory.getSavedQueryDao().insertQueryChangeLog(filename, md5Digest, status, id);
	}

	private void shareDefaultQueries(Set<SavedQuery> queries) {
		QueryFolder folder = daoFactory.getQueryFolderDao().getByName(DEFAULT_QUERIES);
		if (folder == null) {
			folder = new QueryFolder();
			folder.setOwner(sysUser);
		}
		folder.getSavedQueries().addAll(queries);
		folder.setSharedWithAll(true);
		folder.setName(DEFAULT_QUERIES);
		daoFactory.getQueryFolderDao().saveOrUpdate(folder);
	}

	private void configureCatalogQuery(SavedQuery query) {
		saveDefaultQuerySetting(query, "catalog", "default_query");
	}

	private void configureDistributionReportQuery(SavedQuery query) {
		saveDefaultQuerySetting(query, "common", "distribution_report_query");
	}

	private void configureShipmentReportQuery(SavedQuery query) {
		saveDefaultQuerySetting(query, "common", "shipment_export_report");
	}

	private void configureContainerShipmentReportQuery(SavedQuery query) {
		saveDefaultQuerySetting(query, "common", "shipment_container_report");
	}

	private void configureKitReportQuery(SavedQuery query) {
		saveDefaultQuerySetting(query, "common", "specimen_kit_export_report");
	}

	private void configureContainerSpecimensReportQuery(SavedQuery query) {
		saveDefaultQuerySetting(query, "common", "cont_spmns_report_query");
	}

	private void configureCartSpecimensReportQuery(SavedQuery query) {
		saveDefaultQuerySetting(query, "common", "cart_specimens_rpt_query");
	}

	private void saveDefaultQuerySetting(SavedQuery query, String module, String name) {
		try {
			AuthUtil.setCurrentUser(sysUser);

			Integer queryId = cfgService.getIntSetting(module, name, -1);
			if (queryId != -1) {
				return;
			}

			ConfigSettingDetail cfgDetail = new ConfigSettingDetail();
			cfgDetail.setModule(module);
			cfgDetail.setName(name);
			cfgDetail.setValue(query.getId().toString());

			RequestEvent<ConfigSettingDetail> req = new RequestEvent<>(cfgDetail);
			ResponseEvent<ConfigSettingDetail> resp = cfgService.saveSetting(req);
			resp.throwErrorIfUnsuccessful();
		} finally {
			AuthUtil.clearCurrentUser();
		}
	}

	private static final String DEFAULT_QUERIES = "Default Queries";
	
	private static final String QUERIES_DIRECTORY = "/default-queries";

	private static final String DISTRIBUTION_REPORT_QUERY = "DistributionReport.json";

	private static final String SHIPMENT_REPORT_QUERY = "ShipmentReport.json";

	private static final String CONTAINER_SHIPMENT_REPORT_QUERY = "ContainerShipmentReport.json";

	private static final String SPECIMEN_KIT_REPORT_QUERY = "SpecimenKitReport.json";

	private static final String CONTAINER_SPECIMENS_REPORT_QUERY = "ContainerSpecimensReport.json";

	private static final String CART_SPECIMENS_REPORT_QUERY = "CartSpecimensReport.json";
}
