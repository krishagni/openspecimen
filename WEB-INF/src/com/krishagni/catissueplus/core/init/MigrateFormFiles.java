package com.krishagni.catissueplus.core.init;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.administrative.repository.FormListCriteria;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;

import edu.common.dynamicextensions.domain.nui.ColumnDef;
import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.Control;
import edu.common.dynamicextensions.domain.nui.FileUploadControl;
import edu.common.dynamicextensions.domain.nui.SignatureControl;
import edu.common.dynamicextensions.domain.nui.SubFormControl;
import edu.common.dynamicextensions.napi.FileControlValue;

public class MigrateFormFiles implements InitializingBean {

	private static final LogUtil logger = LogUtil.getLogger(MigrateFormFiles.class);

	private DaoFactory deDaoFactory;

	private SessionFactory sessionFactory;

	public void setDeDaoFactory(DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (areFormFilesMigrated()) {
			return;
		}

		logger.info("Migrating form files...");
		migrateFormFiles();
	}

	private void migrateFormFiles() {
		int startAt = 0;
		while (true) {
			List<Form> forms = getForms(startAt, 25);
			if (forms.isEmpty()) {
				break;
			}

			startAt += forms.size();
			for (Form form : forms) {
				try {
					logger.info("Migrating files of the form " + form.getCaption() + " (" + form.getName() + "), if any");
					migrateFormFiles(form);
				} catch (Throwable e) {
					logger.error("Error migrating the files of the form: " + form.getCaption() + " (" + form.getName() + "), if any", e);
				}
			}
		}

		logger.info("Congratulations! Form files migrated.");
	}

	@PlusTransactional
	private boolean areFormFilesMigrated() {
		Number count = (Number) sessionFactory.getCurrentSession().createNativeQuery(GET_MIGRATED_FILES_COUNT).uniqueResult();
		return count != null && count.longValue() > 0L;
	}

	private void migrateFormFiles(Form form) {
		Container formDef = getFormDef(form.getName());
		if (formDef != null) {
			migrateFormFiles(formDef, "IDENTIFIER");
		}
	}

	@PlusTransactional
	private void migrateFormFiles(Container formDef, String recordIdColumn) {
		if (!hasFileOrSignature(formDef)) {
			return;
		}

		for (Control ctrl : formDef.getOrderedControlList()) {
			if (ctrl instanceof FileUploadControl || ctrl instanceof SignatureControl) {
				migrateFormFiles(formDef, ctrl, recordIdColumn);
			} else if (ctrl instanceof SubFormControl) {
				migrateFormFiles(((SubFormControl) ctrl).getSubContainer(), "PARENT_RECORD_ID");
			}
		}
	}

	private void migrateFormFiles(Container formDef, Control ctrl, String recordIdColumn) {
		List<String> columns = new ArrayList<>(ctrl.getColumnDefs().stream().map(ColumnDef::getColumnName).toList());
		columns.add(0, recordIdColumn);

		int startAt = 0;
		String sql = String.format(GET_FILES_SQL, String.join(", ", columns), formDef.getDbTableName(), recordIdColumn);
		while (true) {
			List<Object[]> result = getFileIds(sql, startAt, 100);
			if (result.isEmpty()) {
				break;
			}

			startAt += result.size();

			List<FileControlValue> files = new ArrayList<>();
			for (Object[] row : result) {
				FileControlValue file = new FileControlValue();
				file.setRecordId(((Number) row[0]).longValue());
				if (ctrl instanceof SignatureControl) {
					file.setFileName((String) row[1]);
					file.setFileId((String) row[1]);
					if (StringUtils.isNotBlank(file.getFileId())) {
						String type = file.getFileId().substring(file.getFileId().lastIndexOf(".") + 1);
						if (StringUtils.isNotBlank(type)) {
							type = "image/" + type;
						}

						file.setContentType(type);
					}
				} else {
					file.setFileName((String) row[1]);
					file.setContentType((String) row[2]);
					file.setFileId((String) row[3]);
				}

				if (StringUtils.isNotBlank(file.getFileId())) {
					file.setFormId(formDef.getId());
					files.add(file);
				}
			}

			if (!files.isEmpty()) {
				insertFileIds(files);
			}
		}
	}

	@PlusTransactional
	private Container getFormDef(String name) {
		return Container.getContainer(name);
	}

	@PlusTransactional
	private List<Form> getForms(int startAt, int maxResults) {
		return deDaoFactory.getFormDao().getForms(new FormListCriteria().startAt(startAt).maxResults(maxResults));
	}

	@PlusTransactional
	private List<Object[]> getFileIds(String sql, int startAt, int maxResults) {
		return sessionFactory.getCurrentSession().createNativeQuery(sql)
			.setFirstResult(startAt)
			.setMaxResults(maxResults)
			.list();
	}

	@PlusTransactional
	private void insertFileIds(List<FileControlValue> files) {
		sessionFactory.getCurrentSession().doWork(
			connection -> {
				try (PreparedStatement pstmt = connection.prepareStatement(INSERT_FILE_SQL)) {
					for (FileControlValue file : files) {
						pstmt.setLong(1, file.getFormId());
						pstmt.setLong(2, file.getRecordId());
						pstmt.setString(3, file.getFileId());
						pstmt.setString(4, file.getContentType());
						pstmt.setString(5, file.getFilename());
						pstmt.addBatch();
					}

					pstmt.executeBatch();
				}
			}
		);
	}

	private boolean hasFileOrSignature(Container formDef) {
		for (Control ctrl : formDef.getOrderedControlList()) {
			if (ctrl instanceof FileUploadControl || ctrl instanceof SignatureControl) {
				return true;
			} else if (ctrl instanceof SubFormControl) {
				if (hasFileOrSignature(((SubFormControl) ctrl).getSubContainer())) {
					return true;
				}
			}
		}

		return false;
	}

	private static final String GET_MIGRATED_FILES_COUNT = "select count(*) from dyextn_form_files";

	private static final String GET_FILES_SQL = "select %s from %s order by %s";

	private static final String INSERT_FILE_SQL =
		"insert into " +
		"  dyextn_form_files (form_id, record_id, file_id, file_type, filename) " +
		"values " +
		"  (?, ?, ?, ?, ?)";
}
