package com.krishagni.catissueplus.core.upgrade;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.init.AppProperties;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class ReportDisposedSpecimensWithoutDisposalEvent implements CustomTaskChange {
	private static final LogUtil logger = LogUtil.getLogger(ReportDisposedSpecimensWithoutDisposalEvent.class);

	@Override
	public void execute(Database database) throws CustomChangeException {
		try {
			JdbcConnection conn = (JdbcConnection)database.getConnection();
			Path reportFile = Paths.get(getDataDir(conn)).toAbsolutePath().normalize().resolve(REPORT_FILE);

			try (
				PreparedStatement stmt = conn.prepareStatement(GET_SPECIMENS_SQL);
				ResultSet rs = stmt.executeQuery()
			) {
				if (!rs.next()) {
					logger.info("No disposed specimens without disposal events found.");
					return;
				}

				Files.createDirectories(reportFile.getParent());

				try (
					BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(reportFile.toFile()));
					CsvFileWriter csvFileWriter = CsvFileWriter.createCsvFileWriter(bufferedWriter, ',', '"')
				) {
					csvFileWriter.writeNext(
						new String[] {
							"Collection Protocol",
							"Specimen ID",
							"Specimen Label",
							"Specimen Barcode"
						}
					);

					do { // do-while because rs.next is invoked before entering into this loop
						csvFileWriter.writeNext(
							new String[] {
								rs.getString("short_title"),
								rs.getString("identifier"),
								rs.getString("label"),
								rs.getString("barcode")
							}
						);
					} while (rs.next());
				}
			}

			logger.warn("Disposed specimens without disposal events were found. Report: " + reportFile);
		} catch (Exception e) {
			throw new CustomChangeException("Error reporting disposed specimens without disposal events", e);
		}
	}

	@Override
	public String getConfirmationMessage() {
		return "Reported disposed specimens without disposal events";
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

	private static final String REPORT_FILE = "disposed-specimens-without-disposal-events.csv";

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

	private static final String APP_DATA_DIR = "app.data_dir";

	private static final String GET_SPECIMENS_SQL =
		"select " +
		"  cp.short_title, s.identifier, s.label, s.barcode " +
		"from " +
		"  catissue_specimen s " +
		"  inner join catissue_collection_protocol cp on cp.identifier = s.collection_protocol_id " +
		"where " +
		"  s.activity_status = 'Closed' and " +
		"  not exists (" +
		"    select " +
		"      1 " +
		"    from " +
		"      catissue_disposal_event_param d " +
		"      inner join catissue_form_record_entry re on re.record_id = d.identifier and re.object_id = s.identifier " +
		"      inner join catissue_form_context fc on fc.identifier = re.form_ctxt_id " +
		"      inner join dyextn_containers f on f.identifier = fc.container_id and f.name = 'SpecimenDisposalEvent' " +
		"    where " +
		"      re.activity_status = 'ACTIVE' " +
		"  ) " +
		"order by " +
		"  cp.short_title, s.identifier";
}
