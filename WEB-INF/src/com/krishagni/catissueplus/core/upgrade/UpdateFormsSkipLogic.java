package com.krishagni.catissueplus.core.upgrade;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.init.AppProperties;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.SubFormControl;
import edu.common.dynamicextensions.domain.nui.UserContext;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class UpdateFormsSkipLogic implements CustomTaskChange {

	private static final LogUtil logger = LogUtil.getLogger(UpdateFormsSkipLogic.class);

	@Override
	public void execute(Database database) throws CustomChangeException {
		try {
			User systemUser = getSystemUser();
			AuthUtil.setCurrentUser(systemUser);

			JdbcConnection conn = (JdbcConnection) database.getConnection();
			List<Long> formIds = getFormIds(conn);
			logger.info("Identified following forms with the skip logic: " + formIds.stream().map(Object::toString).collect(Collectors.joining(", ")));

			int count = 0;
			for (Long formId : formIds) {
				if (updateSkipLogic(systemUser, formId)) {
					++count;
				}
			}

			logger.info("Updated skip logic of " + count + " forms.");
		} catch (Throwable t) {
			throw new CustomChangeException("Failed to migrate forms skip logic.", t);
		} finally {
			AuthUtil.clearCurrentUser();
		}
	}

	@Override
	public String getConfirmationMessage() {
		return "Forms skip logic expressions updated. Modified assignment operator to equality";
	}

	@Override
	public void setUp() throws SetupException {

	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {

	}

	@Override
	public ValidationErrors validate(Database database) {
		return null;
	}

	@PlusTransactional
	private User getSystemUser() {
		DaoFactory daoFactory = OpenSpecimenAppCtxProvider.getBean("biospecimenDaoFactory");
		return daoFactory.getUserDao().getSystemUser();
	}

	@PlusTransactional
	private List<Long> getFormIds(JdbcConnection conn)
	throws DatabaseException, SQLException {
		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			String dbType = AppProperties.getInstance().getProperties()
				.getProperty("database.type", "mysql")
				.toLowerCase();

			String sql = null;
			if ("mysql".equals(dbType)) {
				sql = GET_FORM_IDS_MYSQL;
			} else if ("oracle".equals(dbType)) {
				sql = GET_FORM_IDS_ORA;
			}

			if (sql == null) {
				throw new RuntimeException("Could not identify the database type. Check application.properties for database.type. It should be either mysql or oracle.");
			}

			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();

			List<Long> formIds = new ArrayList<>();
			while (rs.next()) {
				formIds.add(rs.getLong(1));
			}

			return formIds;
		} finally {
			close(rs);
			close(stmt);
		}
	}

	@PlusTransactional
	private boolean updateSkipLogic(User systemUser, Long formId) {
		try {
			Container form = Container.getContainer(formId);
			logger.info("Updating skip logic of the form: " + form.getId() + ": " + form.getName() + ": " + form.getCaption());

			if (updateSkipLogic(form)) {
				logger.info("Detected skip logic using an assignment operator in the form: " + form.getName());
				Container.createContainer(getUserContext(systemUser), form, false);
				logger.info("Updated the skip logic of the form: " + form.getName());
				return true;
			} else {
				logger.info("No skip logic with an assignment operator in the form: " + form.getName());
			}
		} catch (Exception e) {
			logger.error("Error updating the skip logic of form: " + formId, e);
			throw e;
		}

		return false;
	}

	private UserContext getUserContext(User systemUser) {
		return new UserContext() {
			@Override
			public Long getUserId() {
				return systemUser.getId();
			}

			@Override
			public String getUserName() {
				return systemUser.getUsername();
			}

			@Override
			public String getIpAddress() {
				return null;
			}
		};
	}

	private boolean updateSkipLogic(Container form) {
		boolean modified = false;
		for (Control ctrl : form.getOrderedControlList()) {
			if (StringUtils.isBlank(ctrl.getShowWhenExpr())) {
				continue;
			}

			if (updateSkipLogic(ctrl)) {
				modified = true;
			}

			if (ctrl instanceof SubFormControl sfCtrl) {
				if (updateSkipLogic(sfCtrl.getSubContainer())) {
					modified = true;
				}
			}
		}

		return modified;
	}

	private boolean updateSkipLogic(Control ctrl) {
		String showWhen = ctrl.getShowWhenExpr();

		boolean modified = false;
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < showWhen.length(); ++i) {
			char ch = showWhen.charAt(i);
			if (ch == '=') {
				if (i > 0 && (showWhen.charAt(i - 1) == '!' || showWhen.charAt(i - 1) == '=')) {
					//
					// if the previous character is either ! or = then it is case of != or ==
					//
					result.append(ch);
				} else if ((i + 1) < showWhen.length() && showWhen.charAt(i + 1) == '=') {
					//
					// if the next character is = then it is the case of ==
					//
					result.append(ch);
				} else {
					//
					// neither previous character is ! or = nor the next character is = then it is assignment op =
					// replace = with ==
					//
					result.append(ch).append('=');
					modified = true;
				}
			} else {
				result.append(ch);
			}
		}

		ctrl.setShowWhenExpr(result.toString());
		return modified;
	}

	private void close(AutoCloseable obj) {
		try {
			if (obj != null) {
				obj.close();
			}
		} catch (Exception e) {
			logger.warn("Encountered error when closing the result/statement. Error = " + e.getMessage() + ". Ignoring the error.", e);
		}
	}

	private static final String GET_FORM_IDS_MYSQL =
		"select " +
		"  identifier " +
		"from " +
		"  dyextn_containers " +
		"where " +
		"  xml like '%showWhenExpr%' and " +
		"  deleted_on is null " +
		"order by " +
		"  identifier asc";

	private static final String GET_FORM_IDS_ORA =
		"select " +
		"  identifier " +
		"from " +
		"  dyextn_containers " +
		"where " +
		"  dbms_lob.instr(xml, utl_raw.cast_to_raw('showWhenExpr'), 1, 1) > 0 and " +
		"  deleted_on is null " +
		"order by " +
		"  identifier asc";
}
