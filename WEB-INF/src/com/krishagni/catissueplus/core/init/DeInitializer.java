package com.krishagni.catissueplus.core.init;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;


import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.ui.PvControlFactory;
import com.krishagni.catissueplus.core.de.ui.SiteControlFactory;
import com.krishagni.catissueplus.core.de.ui.StorageContainerControlFactory;
import com.krishagni.catissueplus.core.de.ui.UserControlFactory;
import com.krishagni.catissueplus.core.query.SpecimenQtyProcFactory;

import edu.common.dynamicextensions.domain.nui.factory.ControlManager;
import edu.common.dynamicextensions.napi.FormDataFilter;
import edu.common.dynamicextensions.napi.FormDataManager;
import edu.common.dynamicextensions.nutility.DeConfiguration;
import edu.common.dynamicextensions.query.PathConfig;
import edu.common.dynamicextensions.query.ResultPostProcManager;

public class DeInitializer implements InitializingBean {
	private static final String QUERY_PATH_CFG = "/com/krishagni/catissueplus/core/de/query/paths.xml";
	
	private PlatformTransactionManager transactionManager;
	
	private ConfigurationService cfgSvc;
	
	private DataSource dataSource;

	private FormDataManager formDataMgr;

	private Map<String, FormDataFilter> preFormSaveFilters = new HashMap<String, FormDataFilter>();

	private Map<String, FormDataFilter> postFormSaveFilters = new HashMap<String, FormDataFilter>();

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setFormDataMgr(FormDataManager formDataMgr) {
		this.formDataMgr = formDataMgr;
	}

	public void setPreFormSaveFilters(Map<String, FormDataFilter> preFormSaveFilters) {
		this.preFormSaveFilters = preFormSaveFilters;
	}

	public void setPostFormSaveFilters(Map<String, FormDataFilter> postFormSaveFilters) {
		this.postFormSaveFilters = postFormSaveFilters;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		File dirFile = new File(cfgSvc.getDataDir(), "de-file-data");
		if (!dirFile.exists()) {
			if (!dirFile.mkdirs()) {
				throw new RuntimeException("Error couldn't create directory for storing de file data");
			}
		}
		
		DeConfiguration.getInstance()
			.dataSource(dataSource, transactionManager)
			.fileUploadDir(dirFile.getAbsolutePath())
			.dateFormat(cfgSvc.getDeDateFormat())
			.timeFormat(cfgSvc.getTimeFormat());

		ControlManager.getInstance().registerFactory(UserControlFactory.getInstance());			
		ControlManager.getInstance().registerFactory(StorageContainerControlFactory.getInstance());
		ControlManager.getInstance().registerFactory(SiteControlFactory.getInstance());
		ControlManager.getInstance().registerFactory(PvControlFactory.getInstance());

		InputStream in = null;
		try {
			in = Utility.getResourceInputStream(QUERY_PATH_CFG);
			PathConfig.initialize(in);			
		} finally {
			IOUtils.closeQuietly(in);
		}
		
		cfgSvc.registerChangeListener("common", (name, value) -> {
			if (!name.equals("locale") && !name.equals("date_format") && !name.equals("de_date_format") && !name.equals("time_format")) {
				return;
			}

			DeConfiguration.getInstance()
				.dateFormat(cfgSvc.getDeDateFormat())
				.timeFormat(cfgSvc.getTimeFormat());
		});

		setFormFilters();
		ResultPostProcManager.getInstance().addFactory("specimenqty", new SpecimenQtyProcFactory());
	}

	private void setFormFilters() {
		for (Map.Entry<String, FormDataFilter> filterEntry : preFormSaveFilters.entrySet()) {
			if (filterEntry.getKey().equals("all")) {
				formDataMgr.getFilterMgr().addPreFilter(filterEntry.getValue());
			} else {
				formDataMgr.getFilterMgr().addPreFilter(filterEntry.getKey(), filterEntry.getValue());
			}
		}

		for (Map.Entry<String, FormDataFilter> filterEntry : postFormSaveFilters.entrySet()) {
			if (filterEntry.getKey().equals("all")) {
				formDataMgr.getFilterMgr().addPostFilter(filterEntry.getValue());
			} else {
				formDataMgr.getFilterMgr().addPostFilter(filterEntry.getKey(), filterEntry.getValue());
			}
		}
	}
}
