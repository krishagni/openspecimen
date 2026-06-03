package com.krishagni.catissueplus.core.upgrade;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.init.AppProperties;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class UpdateSystemWorkflowContainerFields implements CustomTaskChange {
	private static final LogUtil logger = LogUtil.getLogger(UpdateSystemWorkflowContainerFields.class);

	@Override
	public void execute(Database database) throws CustomChangeException {
		try {
			JdbcConnection conn = (JdbcConnection) database.getConnection();
			String workflowFile = getSettingValue(conn, GET_SYS_WORKFLOWS_SQL);
			if (StringUtils.isBlank(workflowFile) || workflowFile.startsWith(CLASSPATH_PREFIX)) {
				return;
			}

			Path dataDir = Paths.get(getDataDir(conn)).toAbsolutePath().normalize();
			Path workflowPath = dataDir.resolve(CONFIG_SETTING_FILES_DIR).resolve(workflowFile).normalize();
			if (!workflowPath.startsWith(dataDir)) {
				throw new CustomChangeException("System workflow file path escapes data directory: " + workflowFile);
			}

			if (!Files.exists(workflowPath)) {
				logger.warn("System workflow file does not exist: " + workflowPath + ". Skipping field path migration.");
				return;
			}

			String oldContent = Files.readString(workflowPath, StandardCharsets.UTF_8);
			String newContent = updateFieldPaths(oldContent);
			if (!oldContent.equals(newContent)) {
				Files.writeString(workflowPath, newContent, StandardCharsets.UTF_8);
			}
		} catch (CustomChangeException e) {
			throw e;
		} catch (Exception e) {
			throw new CustomChangeException("Error updating system workflow container fields", e);
		}
	}

	@Override
	public String getConfirmationMessage() {
		return "Updated container field paths in system workflows.";
	}

	@Override
	public void setUp() throws SetupException {

	}

	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {

	}

	@Override
	public ValidationErrors validate(Database database) {
		return new ValidationErrors();
	}

	private String getDataDir(JdbcConnection conn) throws Exception {
		String dataDir = getSettingValue(conn, GET_DATA_DIR_SQL);
		if (StringUtils.isBlank(dataDir)) {
			AppProperties appProps = AppProperties.getInstance();
			Properties props = appProps != null ? appProps.getProperties() : null;
			dataDir = props != null ? props.getProperty(APP_DATA_DIR) : null;
		}

		return StringUtils.isNotBlank(dataDir) ? dataDir : Paths.get("").toAbsolutePath().toString();
	}

	private String getSettingValue(JdbcConnection conn, String sql) throws Exception {
		try (
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery()
		) {
			return rs.next() ? rs.getString("value") : null;
		}
	}

	private String updateFieldPaths(String content) {
		return content
			.replace("Specimen.specimenPosition.containerName", "Specimen.specimenPosition.container.name")
			.replace("Specimen.specimenPosition.containerDisplayName", "Specimen.specimenPosition.container.displayName")
			.replace("Specimen.specimenPosition.containerBarcode", "Specimen.specimenPosition.container.barcode")
			.replace("Specimen.specimenPosition.containerSite", "Specimen.specimenPosition.container.site")
			.replace("Specimen.checkoutPosition.containerName", "Specimen.checkoutPosition.container.name")
			.replace("Specimen.checkoutPosition.containerDisplayName", "Specimen.checkoutPosition.container.displayName")
			.replace("Specimen.checkoutPosition.containerBarcode", "Specimen.checkoutPosition.container.barcode")
			.replace("Specimen.checkoutPosition.containerSite", "Specimen.checkoutPosition.container.site");
	}

	private static final String GET_SYS_WORKFLOWS_SQL =
		"select " +
		"  s.value " +
		"from " +
		"  os_cfg_settings s " +
		"  inner join os_cfg_props p on p.identifier = s.property_id " +
		"  inner join os_modules m on m.identifier = p.module_id " +
		"where " +
		"  m.name = 'biospecimen' and " +
		"  p.name = 'sys_workflows' and " +
		"  s.activity_status = 'Active'";

	private static final String GET_DATA_DIR_SQL =
		"select " +
		"  s.value " +
		"from " +
		"  os_cfg_settings s " +
		"  inner join os_cfg_props p on p.identifier = s.property_id " +
		"  inner join os_modules m on m.identifier = p.module_id " +
		"where " +
		"  m.name = 'common' and " +
		"  p.name = 'data_dir' and " +
		"  s.activity_status = 'Active'";

	private static final String CLASSPATH_PREFIX = "classpath:";

	private static final String CONFIG_SETTING_FILES_DIR = "config-setting-files";

	private static final String APP_DATA_DIR = "app.data_dir";
}
