package com.krishagni.catissueplus.core.init;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;


import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.events.ContainerTypeDetail;
import com.krishagni.catissueplus.core.administrative.services.ContainerTypeService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class ImportContainerTypes implements InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(ImportContainerTypes.class);

	private ContainerTypeService containerTypeSvc;

	private DaoFactory daoFactory;

	private JdbcTemplate jdbcTemplate;

	private User sysUser;

	public void setContainerTypeSvc(ContainerTypeService containerTypeSvc) {
		this.containerTypeSvc = containerTypeSvc;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		importContainerTypes();
	}

	private void importContainerTypes() {
		try {
			Resource[] resources = new PathMatchingResourcePatternResolver().getResources(CONTAINER_TYPES_DIR + "/*");
			setupSystemUser();

			for (Resource resource : resources) {
				File dir = resource.getFile();
				if (!dir.isDirectory() || isHierarchyCreated(dir.getName())) {
					continue;
				}

				List<ContainerTypeDetail> types = getContainerTypes(dir);
				createContainerTypes(dir.getName(), types);
			}
		} catch (Exception e) {
			logger.error("Error while creating default container types", e);
		} finally {
			AuthUtil.clearCurrentUser();
		}
	}

	@PlusTransactional
	private void setupSystemUser() {
		sysUser = daoFactory.getUserDao().getSystemUser();
		AuthUtil.setCurrentUser(sysUser);
	}

	private List<ContainerTypeDetail> getContainerTypes(File dir)
	throws IOException {
		List<ContainerTypeDetail> types = new ArrayList<>();
		for (File file : dir.listFiles()) {
			types.add(getContainerType(file));
		}

		return sortByHierarchy(types);
	}

	private ContainerTypeDetail getContainerType(File content)
	throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(content, ContainerTypeDetail.class);
	}

	private List<ContainerTypeDetail> sortByHierarchy(List<ContainerTypeDetail> types) {
		Map<String, List<ContainerTypeDetail>> typeChildrenMap = getTypeChildrenMap(types);

		List<ContainerTypeDetail> result = new ArrayList<>();
		List<ContainerTypeDetail> workList = new ArrayList<>();
		workList.add(null);

		while (!typeChildrenMap.isEmpty()) {
			ContainerTypeDetail type = workList.remove(0);
			List<ContainerTypeDetail> children = typeChildrenMap.remove(type != null ? type.getName() : null);
			if (children == null) {
				continue;
			}

			result.addAll(children);
			workList.addAll(children);
		}

		return result;
	}

	private Map<String, List<ContainerTypeDetail>> getTypeChildrenMap(List<ContainerTypeDetail> types) {
		Map<String, List<ContainerTypeDetail>> typeChildrenMap = new HashMap<>();

		for (ContainerTypeDetail type : types) {
			String typeName = type.getCanHold() != null ? type.getCanHold().getName() : null;
			List<ContainerTypeDetail> children = typeChildrenMap.get(typeName);
			if (children == null) {
				children = new ArrayList<>();
				typeChildrenMap.put(typeName, children);
			}

			children.add(type);
		}

		return typeChildrenMap;
	}

	@PlusTransactional
	private void createContainerTypes(String hierarchyName, List<ContainerTypeDetail> types) {
		if (anyContainerTypeExists(types)) {
			logger.error("One or more container types of hierarchy " + hierarchyName + " already exist. Therefore not creating hierarchy.");
			return;
		}

		for (ContainerTypeDetail type : types) {
			ResponseEvent<ContainerTypeDetail> resp = containerTypeSvc.createContainerType(new RequestEvent<>(type));
			resp.throwErrorIfUnsuccessful();
		}

		recordHierarchyCreated(hierarchyName);
	}

	private boolean anyContainerTypeExists(List<ContainerTypeDetail> types) {
		List<String> names = types.stream().map(ContainerTypeDetail::getName).collect(Collectors.toList());
		return !daoFactory.getContainerTypeDao().getByNames(names).isEmpty();
	}

	@PlusTransactional
	private boolean isHierarchyCreated(String hierarchyName) {
		List<String> result = jdbcTemplate.queryForList(GET_CHANGE_LOG_SQL, String.class, getChangeLogId(hierarchyName));
		return CollectionUtils.isNotEmpty(result);
	}

	private void recordHierarchyCreated(String hierarchyName) {
		jdbcTemplate.update(INSERT_CHANGE_LOG_SQL,
			getChangeLogId(hierarchyName), hierarchyName, Calendar.getInstance().getTime());
	}

	private String getChangeLogId(String hierarchyName) {
		return "Container type hierarchy - " + hierarchyName;
	}

	private static final String CONTAINER_TYPES_DIR = "/default-container-types";

	private static final String INSERT_CHANGE_LOG_SQL =
		"insert into databasechangelog " +
		"  (id, author, filename, dateexecuted, orderexecuted, exectype) " +
	    "values " +
		"  (?, 'System', ?, ?, 0, 'EXECUTED')";

	private static final String GET_CHANGE_LOG_SQL = "select id from databasechangelog where id = ?";
}
