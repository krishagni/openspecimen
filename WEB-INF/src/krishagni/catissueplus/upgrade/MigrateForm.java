//package krishagni.catissueplus.upgrade;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.sql.Blob;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Timestamp;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import krishagni.catissueplus.beans.FormRecordEntryBean.Status;
//import krishagni.catissueplus.dto.FormDetailsDTO;
//
//import org.apache.commons.lang.ArrayUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//
//import au.com.bytecode.opencsv.CSVWriter;
//import edu.common.dynamicextensions.domain.Attribute;
//import edu.common.dynamicextensions.domain.BaseAbstractAttribute;
//import edu.common.dynamicextensions.domain.CategoryAttribute;
//import edu.common.dynamicextensions.domain.CategoryEntity;
//import edu.common.dynamicextensions.domain.EntityRecord;
//import edu.common.dynamicextensions.domain.UserDefinedDE;
//import edu.common.dynamicextensions.domain.nui.CheckBox;
//import edu.common.dynamicextensions.domain.nui.ComboBox;
//import edu.common.dynamicextensions.domain.nui.Container;
//import edu.common.dynamicextensions.domain.nui.Control;
//import edu.common.dynamicextensions.domain.nui.Control.LabelPosition;
//import edu.common.dynamicextensions.domain.nui.DataType;
//import edu.common.dynamicextensions.domain.nui.DatePicker;
//import edu.common.dynamicextensions.domain.nui.DatePicker.DefaultDateType;
//import edu.common.dynamicextensions.domain.nui.FileUploadControl;
//import edu.common.dynamicextensions.domain.nui.Label;
//import edu.common.dynamicextensions.domain.nui.ListBox;
//import edu.common.dynamicextensions.domain.nui.MultiSelectCheckBox;
//import edu.common.dynamicextensions.domain.nui.MultiSelectListBox;
//import edu.common.dynamicextensions.domain.nui.NumberField;
//import edu.common.dynamicextensions.domain.nui.PermissibleValue;
//import edu.common.dynamicextensions.domain.nui.PvDataSource;
//import edu.common.dynamicextensions.domain.nui.PvDataSource.Ordering;
//import edu.common.dynamicextensions.domain.nui.PvVersion;
//import edu.common.dynamicextensions.domain.nui.RadioButton;
//import edu.common.dynamicextensions.domain.nui.SelectControl;
//import edu.common.dynamicextensions.domain.nui.StringTextField;
//import edu.common.dynamicextensions.domain.nui.SubFormControl;
//import edu.common.dynamicextensions.domain.nui.TextArea;
//import edu.common.dynamicextensions.domain.nui.TextField;
//import edu.common.dynamicextensions.domain.nui.UserContext;
//import edu.common.dynamicextensions.domaininterface.AbstractAttributeInterface;
//import edu.common.dynamicextensions.domaininterface.AssociationInterface;
//import edu.common.dynamicextensions.domaininterface.AttributeInterface;
//import edu.common.dynamicextensions.domaininterface.AttributeMetadataInterface;
//import edu.common.dynamicextensions.domaininterface.AttributeTypeInformationInterface;
//import edu.common.dynamicextensions.domaininterface.BaseAbstractAttributeInterface;
//import edu.common.dynamicextensions.domaininterface.BooleanTypeInformationInterface;
//import edu.common.dynamicextensions.domaininterface.CategoryAssociationInterface;
//import edu.common.dynamicextensions.domaininterface.CategoryAttributeInterface;
//import edu.common.dynamicextensions.domaininterface.DateTypeInformationInterface;
//import edu.common.dynamicextensions.domaininterface.DoubleTypeInformationInterface;
//import edu.common.dynamicextensions.domaininterface.EntityInterface;
//import edu.common.dynamicextensions.domaininterface.FloatTypeInformationInterface;
//import edu.common.dynamicextensions.domaininterface.FormControlNotesInterface;
//import edu.common.dynamicextensions.domaininterface.NumericTypeInformationInterface;
//import edu.common.dynamicextensions.domaininterface.PermissibleValueInterface;
//import edu.common.dynamicextensions.domaininterface.SemanticPropertyInterface;
//import edu.common.dynamicextensions.domaininterface.StringTypeInformationInterface;
//import edu.common.dynamicextensions.domaininterface.TaggedValueInterface;
//import edu.common.dynamicextensions.domaininterface.UserDefinedDEInterface;
//import edu.common.dynamicextensions.domaininterface.databaseproperties.TablePropertiesInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.AbstractContainmentControlInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.CheckBoxInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.ComboBoxInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.ContainerInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.ControlInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.DatePickerInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.FileUploadInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.LabelInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.ListBoxInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.MultiSelectCheckBoxInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.RadioButtonInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.SelectInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.TextAreaInterface;
//import edu.common.dynamicextensions.domaininterface.userinterface.TextFieldInterface;
//import edu.common.dynamicextensions.domaininterface.validationrules.RuleInterface;
//import edu.common.dynamicextensions.domaininterface.validationrules.RuleParameterInterface;
//import edu.common.dynamicextensions.entitymanager.CategoryManager;
//import edu.common.dynamicextensions.entitymanager.EntityManager;
//import edu.common.dynamicextensions.exception.DynamicExtensionsSystemException;
//import edu.common.dynamicextensions.napi.ControlValue;
//import edu.common.dynamicextensions.napi.FileControlValue;
//import edu.common.dynamicextensions.napi.FormData;
//import edu.common.dynamicextensions.napi.FormDataManager;
//import edu.common.dynamicextensions.napi.impl.FormDataManagerImpl;
//import edu.common.dynamicextensions.ndao.DbSettingsFactory;
//import edu.common.dynamicextensions.ndao.JdbcDao;
//import edu.common.dynamicextensions.ndao.JdbcDaoFactory;
//import edu.common.dynamicextensions.ndao.ResultExtractor;
//import edu.common.dynamicextensions.nutility.FileUploadMgr;
//import edu.common.dynamicextensions.nutility.IoUtil;
//import edu.common.dynamicextensions.processor.ProcessorConstants;
//import edu.common.dynamicextensions.util.DataValueMapUtility;
//import edu.common.dynamicextensions.util.DynamicExtensionsUtility;
//import edu.emory.mathcs.backport.java.util.Collections;
//import edu.wustl.cab2b.server.cache.EntityCache;
//
//public class MigrateForm {
//	protected class FormMigrationCtxt {
//		protected Container newForm;
//		
//		protected Control sfCtrl;
//		
//		protected Map<BaseAbstractAttributeInterface, Object> fieldMap = 
//				new HashMap<BaseAbstractAttributeInterface, Object>();
//		
//		public FormMigrationCtxt() {
//			
//		}
//	}
//	
//	private static Logger logger = Logger.getLogger(MigrateForm.class);
//	
//	private static EntityCache entityCache = null;
//	
//	private static Map<String, String> datePatternMap = null;
//	
//	private boolean verticalCtrlAlignment = false;
//	
//	private UserContext usrCtx = null;
//	
//	private int userDefId = 0;
//	
//	private static Set<String> containerNames = new HashSet<String>();
//	
//	private static final String specialChars = "[+-/*(){}%. ]";
//	
//	private static Map<String, Long> staticEntityMap = null;
//
//	private FormMigrationCtxt formMigrationCtxt = null;
//	
//	private ContainerInterface oldForm = null;
//	
//	private CSVWriter recordsLog;
//	
//	private int logEntriesCnt = 0;
//	
//	private Set<String> obsoleteTables = new HashSet<String>();
//	
//	static {
//		
//		entityCache = EntityCache.getInstance();
//		
//		datePatternMap = new HashMap<String, String>();
//		datePatternMap.put("DateAndTime", "MM-dd-yyyy HH:mm");
//		datePatternMap.put("MonthAndYear", "MM-yyyy");
//		datePatternMap.put("YearOnly", "yyyy");
//		
//		staticEntityMap = getStaticEntityIdsMap();
//		
//		logger.setLevel(Level.INFO);
//	}
//	
//	public MigrateForm(UserContext usrCtx) {
//		this.usrCtx = usrCtx;
//	}
//	
//	public MigrateForm(UserContext usrCtx, CSVWriter recordsLog) {
//		this.usrCtx = usrCtx;
//		this.recordsLog = recordsLog;		
//	}
//	
//	private static Map<String, Long> getStaticEntityIdsMap() {
//		JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
//		return jdbcDao.getResultSet(GET_STATIC_ENTITY_ID_NAME_SQL, null, new ResultExtractor<Map<String, Long>>() {
//			@Override
//			public Map<String, Long>  extract(ResultSet rs) throws SQLException {
//				Map<String, Long> staticEntityMap = new HashMap<String, Long>();
//				while(rs.next()) {					
//					staticEntityMap.put(getEntityType(rs.getString("name")), rs.getLong("identifier"));					
//				}
//				
//				return staticEntityMap;
//			}
//		});
//		
//	}
//
//	protected static String getEntityType(String entityType) {
//		if (entityType.equals("edu.wustl.catissuecore.domain.Specimen")) {
//			entityType = "defaultSpecimen";
//		} else if (entityType.equals("edu.wustl.catissuecore.domain.SpecimenCollectionGroup")) {
//			entityType = "defaultSpecimenCollectionGroup";
//		} else if (entityType.equals("edu.wustl.catissuecore.domain.Participant")) {
//			entityType = "defaultParticipant";
//		} else if (entityType.equals("edu.wustl.catissuecore.domain.deintegration.ParticipantRecordEntry")) {
//			entityType = "Participant";
//		} else if (entityType.equals("edu.wustl.catissuecore.domain.deintegration.SpecimenRecordEntry")) {
//			entityType = "Specimen";
//		} else if (entityType.equals("edu.wustl.catissuecore.domain.deintegration.SCGRecordEntry")) {
//			entityType = "SpecimenCollectionGroup";
//		} else if (entityType.equals("edu.wustl.catissuecore.domain.deintegration.ActionApplicationRecordEntry")) {
//			entityType = "SpecimenEvent";
//		}
//		
//		return entityType;
//	}
//	
//	public void migrateForm(Long containerId, List<FormInfo> formInfos) 
//	throws Exception {		
//		oldForm = getOldFormDefinition(containerId);
//		if (oldForm == null) {
//			return;
//		}
//		
//		logger.info("Starting to migrate form: " + oldForm.getCaption() + "(" + containerId + ")");		
//		
//		formMigrationCtxt = getNewFormDefinition(oldForm);
//		if (formMigrationCtxt == null) {
//			return;
//		}
//		
//		JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
//
//		Long newFormId = saveForm(jdbcDao);
//		logger.info("New form id after migration " + newFormId);
//
//		if (newFormId == null) {
//			logger.warn("There is no new form for the old form " + oldForm.getCaption());
//			return;
//		}
//		
//		attachForm(jdbcDao, formInfos);
//	}
//	
//	public String getFormCaption() {
//		return formMigrationCtxt.newForm.getCaption();
//	}
//	
//	public String getObsoleteTables() {
//		return StringUtils.join(obsoleteTables, ",");
//	}
//
//	
//	////////////////////////////////////////////////////////////////////////////////////////
//	//
//	// Container/form migration logic
//	//
//	////////////////////////////////////////////////////////////////////////////////////////
//	
//
//	private ContainerInterface getOldFormDefinition(Long containerId) 
//	throws Exception {
////		HibernateDAO dao = null;		
//		ContainerInterface container = null;
//		
////		try {
////			dao = DynamicExtensionsUtility.getHibernateDAO();
////			String objectType = edu.common.dynamicextensions.domain.userinterface.Container.class.getName();
////			container = (ContainerInterface)dao.retrieveById(objectType, containerId); 
////		} catch (Exception e) {
////			logger.error("Error obtaining container: " + containerId + e);
////		} finally {
////			DynamicExtensionsUtility.closeDAO(dao);			
////		}
//		
//		return container;		
//	}
//
//
//	protected FormMigrationCtxt getNewFormDefinition(ContainerInterface oldForm) 
//	throws Exception {
//
//		FormMigrationCtxt formMigrationCtxt = new FormMigrationCtxt();
//		EntityInterface entity = null;
//		
//		if (oldForm.getAbstractEntity() instanceof CategoryEntity) {
//			CategoryEntity catEntity = (CategoryEntity)oldForm.getAbstractEntity();
//			entity = catEntity.getEntity();
//		} else {
//			entity = (EntityInterface)oldForm.getAbstractEntity();
//		}		
//		obsoleteTables.add(entity.getTableProperties().getName());
//		
//		String entityName = entity.getName();
//		String oldFormCaption = oldForm.getCaption() != null ? oldForm.getCaption() : entityName;
//		Container newForm = new Container();
//		newForm.setCaption(getCaption(oldFormCaption));
//		newForm.setName(getUniqueFormName(entityName));
//		
//		int seqOffset = 0;
//		formMigrationCtxt.newForm = newForm;
//		
//		for (ControlInterface oldCtrl : oldForm.getAllControlsUnderSameDisplayLabel()) {
//			Control newCtrl = null;
//			Object mapCtxt = null;
//			if (oldCtrl.getHeading() != null) {
//				Label heading = getHeading(oldCtrl);
//				heading.setSequenceNumber(oldCtrl.getSequenceNumber() + seqOffset);
//				ensureUniqueUdn(newForm, heading);
//				newForm.addControl(heading);
//				
//				seqOffset++;
//			}
//			
//			for (Label note : getNotes(oldCtrl)) {
//				note.setSequenceNumber(oldCtrl.getSequenceNumber() + seqOffset);
//				ensureUniqueUdn(newForm, note);
//				newForm.addControl(note);
//				seqOffset++;
//			}				
//								
//			if (oldCtrl instanceof AbstractContainmentControlInterface) {
//				AbstractContainmentControlInterface oldSfCtrl = (AbstractContainmentControlInterface)oldCtrl; 
//				FormMigrationCtxt sfCtxt = getNewFormDefinition(oldSfCtrl.getContainer());				
//				
//				newCtrl = getNewSubFormControl(oldSfCtrl, sfCtxt.newForm);
//				sfCtxt.sfCtrl = newCtrl;
//				
//				mapCtxt = sfCtxt;
//			} else {
//				newCtrl = getNewControl(oldCtrl);
//				mapCtxt = newCtrl;
//			}
//			
//			newCtrl.setSequenceNumber(oldCtrl.getSequenceNumber() + seqOffset);
//			ensureUniqueUdn(newForm, newCtrl);
//			newForm.addControl(newCtrl);
//
//			formMigrationCtxt.fieldMap.put(oldCtrl.getBaseAbstractAttribute(), mapCtxt);			
//		}
//		
//		return formMigrationCtxt;
//	}
//
//	private Control getNewControl(ControlInterface oldCtrl) {
//		Control ctrl = null; 
//
//		if (oldCtrl instanceof CheckBoxInterface) {
//			ctrl = getNewCheckBox((CheckBoxInterface)oldCtrl);
//		} else if (oldCtrl instanceof DatePickerInterface) {
//			ctrl = getNewDatePicker((DatePickerInterface)oldCtrl);
//		} else if (oldCtrl instanceof LabelInterface) {
//			ctrl = getNewLabel((LabelInterface)oldCtrl);
//		} else if (oldCtrl instanceof FileUploadInterface) {
//			ctrl = getNewFileControl((FileUploadInterface)oldCtrl);
//		} else if (oldCtrl instanceof TextAreaInterface) {
//			ctrl = getNewTextArea((TextAreaInterface)oldCtrl);
//		} else if (oldCtrl instanceof TextFieldInterface) {
//			ctrl = getNewTextField((TextFieldInterface)oldCtrl);
//		} else if (oldCtrl instanceof RadioButtonInterface) {
//			ctrl = getNewRadioButton((RadioButtonInterface)oldCtrl);			
//		} else if (oldCtrl instanceof MultiSelectCheckBoxInterface) {
//			ctrl = getNewMultiSelectCheckBox((MultiSelectCheckBoxInterface)oldCtrl);
//		} else if (oldCtrl instanceof ComboBoxInterface) {
//			ctrl = getNewComboBox((ComboBoxInterface)oldCtrl);
//		} else if (oldCtrl instanceof ListBoxInterface) {
//			ctrl = getNewListBox((ListBoxInterface)oldCtrl);
//		} else {
//			throw new RuntimeException("Unknown control type: " + oldCtrl);
//		}		
//						
//		return ctrl;
//	}
//	
//	private void addToOldTable(ControlInterface oldCtrl) {
//		if (!(oldCtrl.getBaseAbstractAttribute() instanceof AssociationInterface)) {
//			return;
//		}
//		
//		AssociationInterface assoc = (AssociationInterface)oldCtrl.getBaseAbstractAttribute();
//		EntityInterface targetEntity = assoc.getTargetEntity();
//		if (targetEntity == null) {
//			return;
//		}
//		
//
//		TablePropertiesInterface tabProps = targetEntity.getTableProperties();
//		if (tabProps == null) {
//			return;
//		}
//		
//		if (tabProps.getName() != null) {
//			obsoleteTables.add(tabProps.getName());
//		}
//	}
//		
//	private SubFormControl getNewSubFormControl(
//			AbstractContainmentControlInterface oldCtrl, Container newSubContainer) {		
//		SubFormControl sfCtrl = new SubFormControl();
//		setCtrlProps(sfCtrl, oldCtrl);
//		
//		newSubContainer.setName(sfCtrl.getName());
//		sfCtrl.setSubContainer(newSubContainer);
//		sfCtrl.setNoOfEntries(oldCtrl.isCardinalityOneToMany() ? -1 : 0);
//		
//		//		
//		// TODO: setting of properties - showAddMoreLink and pasteButtonEnabled
//		//		
//		return sfCtrl;
//	}
//	
//	private Label getHeading(ControlInterface oldCtrl) {
//		Label label = new Label();
//		label.setName("heading" + oldCtrl.getId());
//		label.setUserDefinedName("heading" + oldCtrl.getId());
//		label.setCaption(oldCtrl.getHeading());
//		label.setHeading(true);
//		label.setLabelPosition(verticalCtrlAlignment ? LabelPosition.TOP : LabelPosition.LEFT_SIDE);
//		label.setShowLabel(true);
//		return label;
//	}
//	
//	private List<Label> getNotes(ControlInterface oldCtrl) {		
//		List<FormControlNotesInterface> oldNotes = oldCtrl.getFormNotes();
//		
//		List<Label> newNotes = new ArrayList<Label>();
//		if (oldNotes == null) {
//			return newNotes;
//		}
//		
//		int i = 0;
//		for (FormControlNotesInterface oldNote : oldNotes) {
//			Label note = new Label();
//			note.setName("note" + oldCtrl.getId() + "_" + i);
//			note.setUserDefinedName("note" + oldCtrl.getId() + "_" + i);
//			note.setCaption(oldNote.getNote());
//			note.setNote(true);
//			note.setLabelPosition(verticalCtrlAlignment ? LabelPosition.TOP : LabelPosition.LEFT_SIDE);
//			note.setShowLabel(true);
//			newNotes.add(note);				
//			
//			++i;
//		}
//	
//		return newNotes;
//	}
//	
//	private CheckBox getNewCheckBox(CheckBoxInterface oldCheckBox) {
//		CheckBox checkBox = new CheckBox();
//		setCtrlProps(checkBox, oldCheckBox);
//		
//		String defValue = getDefaultValue(oldCheckBox);
//		boolean isChecked = defValue != null && 
//				(defValue.equalsIgnoreCase("true") || defValue.equals("1"));		
//		checkBox.setDefaultValueChecked(isChecked); 
//		return checkBox;
//	}
//	
//	private DatePicker getNewDatePicker(DatePickerInterface oldCtrl) {
//		edu.common.dynamicextensions.domain.userinterface.DatePicker oldDatePicker = 
//				(edu.common.dynamicextensions.domain.userinterface.DatePicker) oldCtrl;
//		
//		DatePicker datePicker = new DatePicker();
//		setCtrlProps(datePicker, oldDatePicker);
//		
//		String dateValueType = oldDatePicker.getDateValueType(); 
//		if (dateValueType != null && dateValueType.equals(ProcessorConstants.DATE_VALUE_TODAY)) {
//			datePicker.setDefaultDateType(DefaultDateType.CURRENT_DATE);
//		}		
//				
//		datePicker.setShowCalendar(bool(oldDatePicker.getShowCalendar()));
//		
//		AttributeMetadataInterface attrMetadata = (AttributeMetadataInterface)
//				oldDatePicker.getBaseAbstractAttribute();
//		AttributeTypeInformationInterface attrTypeInfo = attrMetadata.getAttributeTypeInformation();		
//		if (attrTypeInfo instanceof DateTypeInformationInterface) {
//			DateTypeInformationInterface dateTypeInfo = (DateTypeInformationInterface) attrTypeInfo;
//
//			String format = null;
//			if (dateTypeInfo.getFormat() != null) {
//				format = datePatternMap.get(dateTypeInfo.getFormat());
//				if (format != null) {
//					datePicker.setFormat(format);
//				}
//			}
//		} else {
//			logger.info("Date picker control refers to entity attribute that is not of date type " +
//					"Control name: " + oldCtrl.getCaption() + 
//					", attribute id: " + oldCtrl.getBaseAbstractAttribute().getId());
//		}
//		
//		RuleInterface rule = null;
//		if ((rule = getRule(oldCtrl, "allowPastAndPresentDateOnly")) != null) {
//			datePicker.addValidationRule("allowPastAndPresentDateOnly", null);
//		} else if ((rule = getRule(oldCtrl, "allowfuturedate")) != null) {
//			datePicker.addValidationRule("allowfuturedate", null);
//		}
//		
//		if ((rule = getRule(oldCtrl, "dateRange")) != null) {
//			Collection<RuleParameterInterface> ruleParams = rule.getRuleParameterCollection();
//			if (ruleParams != null) {
//				Map<String, String> params = new HashMap<String, String>();
//				for (RuleParameterInterface ruleParam : ruleParams) {
//					params.put(ruleParam.getName(), ruleParam.getValue());
//				}
//				
//				if (!params.isEmpty() && params.containsKey("min") && params.containsKey("max")) {
//					datePicker.addValidationRule("dateRange", params);
//				}
//			}			
//		}
//		
//		return datePicker;
//	}
//	
//	private Label getNewLabel(LabelInterface oldCtrl) {
//		Label label = new Label();
//		setCtrlProps(label, oldCtrl);
//		return label;
//	}
//	
//	private FileUploadControl getNewFileControl(FileUploadInterface oldCtrl) {
//		FileUploadControl fileCtrl = new FileUploadControl();
//		setCtrlProps(fileCtrl, oldCtrl);
//		return fileCtrl;
//	}
//	
//	private TextArea getNewTextArea(TextAreaInterface oldCtrl) {
//		edu.common.dynamicextensions.domain.userinterface.TextArea oldTextArea = 
//				(edu.common.dynamicextensions.domain.userinterface.TextArea) oldCtrl;
//		
//		TextArea textArea = new TextArea();
//		setCtrlProps(textArea, oldTextArea);		
//		
//		textArea.setNoOfColumns(oldTextArea.getColumns());
//		textArea.setNoOfRows(oldTextArea.getRows());
//		textArea.setDefaultValue(getDefaultValue(oldCtrl));
//		addCharSetRuleIfPresent(textArea, oldCtrl);
//		
//		return textArea;
//	}
//	
//	private TextField getNewTextField(TextFieldInterface oldCtrl) {	
//		AttributeTypeInformationInterface attrType = getDataType(oldCtrl);
//		
//		TextField textField = null; 
//		if (attrType instanceof NumericTypeInformationInterface) {
//			textField = getNewNumberField(oldCtrl);
//		} else {
//			textField = getNewStringTextField(oldCtrl);
//		}
//		
//		textField.setDefaultValue(getDefaultValue(oldCtrl));
//		return textField;
//	}
//	
//	private StringTextField getNewStringTextField(TextFieldInterface oldCtrl) {
//		StringTextField textField = new StringTextField();
//		setCtrlProps(textField, oldCtrl);
//		
//		textField.setNoOfColumns(oldCtrl.getColumns());
//		textField.setUrl(bool(oldCtrl.getIsUrl()));
//		textField.setPassword(bool(oldCtrl.getIsPassword()));
//		
//		AttributeTypeInformationInterface attrType = getDataType(oldCtrl);
//		if (attrType instanceof StringTypeInformationInterface) {
//			StringTypeInformationInterface stringType = (StringTypeInformationInterface)attrType;
//			Integer maxSize = stringType.getSize();
//			if (maxSize != null) {
//				textField.setMinLength(0);
//				textField.setMaxLength(maxSize);
//			}
//		}
//		
//		addCharSetRuleIfPresent(textField, oldCtrl);
//		return textField;
//	}
//	
//
//	private NumberField getNewNumberField(TextFieldInterface oldCtrl) {
//		NumberField numberField = new NumberField();
//		setCtrlProps(numberField, oldCtrl);
//
//		AttributeTypeInformationInterface typeInfo = getDataType(oldCtrl);
//		NumericTypeInformationInterface numInfo = (NumericTypeInformationInterface)typeInfo;
//				
//		numberField.setNoOfColumns(oldCtrl.getColumns());		
//		numberField.setNoOfDigits(numInfo.getDigits() != null ? numInfo.getDigits() : 19); 	
//		
//		if (numInfo instanceof DoubleTypeInformationInterface || 
//				numInfo instanceof FloatTypeInformationInterface) {
//			Integer decimalPlaces = numInfo.getDecimalPlaces(); 
//			if (decimalPlaces != null && decimalPlaces > 0) {
//				numberField.setNoOfDigitsAfterDecimal(decimalPlaces);
//			} else {
//				numberField.setNoOfDigitsAfterDecimal(5);
//			}
//		}
//		
//		numberField.setMeasurementUnits(numInfo.getMeasurementUnits());
//		
//		RuleInterface rule = getRule(oldCtrl, "range");
//		if (rule != null) {
//			Collection<RuleParameterInterface> ruleParams = rule.getRuleParameterCollection();
//			if (ruleParams != null) {
//				for (RuleParameterInterface ruleParam : ruleParams) {
//					if (ruleParam.getName().equals("min")) {
//						numberField.setMinValue(ruleParam.getValue());
//					} else if (ruleParam.getName().equals("max")) {
//						numberField.setMaxValue(ruleParam.getValue());
//					}
//				}
//			}
//		}
//		
//		return numberField;
//	}
//	
//	private RadioButton getNewRadioButton(RadioButtonInterface oldCtrl) {
//		RadioButton radioButton = new RadioButton();
//		setCtrlProps(radioButton, oldCtrl);
//
//		PvDataSource result = getPvDataSource(radioButton, oldCtrl);		
//		radioButton.setPvDataSource(result);
//		radioButton.setOptionsPerRow(getOptionsPerRow(oldCtrl));
//		
//		return radioButton;
//	}
//	
//	private MultiSelectCheckBox getNewMultiSelectCheckBox(MultiSelectCheckBoxInterface oldCtrl) {
//		MultiSelectCheckBox multiSelectCb = new MultiSelectCheckBox();
//		setSelectProps(multiSelectCb, oldCtrl);
//		multiSelectCb.setOptionsPerRow(getOptionsPerRow(oldCtrl));
//		
//		addToOldTable(oldCtrl);
//		return multiSelectCb;
//	}
//	
//	private ComboBox getNewComboBox(ComboBoxInterface oldCtrl) {
//		ComboBox comboBox = new ComboBox();
//		setSelectProps(comboBox, oldCtrl);
//		
//		comboBox.setNoOfColumns(oldCtrl.getColumns() == null ? 0 : oldCtrl.getColumns());
//		comboBox.setMinQueryChars(3); // TODO: Check whether this is appropriate
//		return comboBox;
//	}
//	
//	private ListBox getNewListBox(ListBoxInterface oldCtrl) {
//		ListBox listBox = null;
//		if (oldCtrl.getIsMultiSelect() != null && oldCtrl.getIsMultiSelect()) {
//			listBox = new MultiSelectListBox();
//			addToOldTable(oldCtrl);
//		} else {
//			listBox = new ListBox();
//		}
//		
//		setSelectProps(listBox, oldCtrl);
//		if (oldCtrl.getNoOfRows() != null) {
//			listBox.setNoOfRows(oldCtrl.getNoOfRows() == null ? 0 : oldCtrl.getNoOfRows());
//		}		
//		listBox.setAutoCompleteDropdownEnabled(bool(oldCtrl.getIsUsingAutoCompleteDropdown()));
//		listBox.setMinQueryChars(3);
//						
//		return listBox;		
//	}
//	
//	private void setSelectProps(SelectControl newCtrl, SelectInterface oldCtrl) {
//		setCtrlProps(newCtrl, oldCtrl);
//		newCtrl.setPvDataSource(getPvDataSource(newCtrl, oldCtrl));		
//	}
//	
//	private void setCtrlProps(Control newCtrl, ControlInterface oldCtrl) {
//		String ctrlName = getCtrlName(oldCtrl);
//		newCtrl.setName(ctrlName.concat(oldCtrl.getId().toString()));
//		newCtrl.setUserDefinedName(ctrlName);
//		newCtrl.setCaption(getCaption(oldCtrl.getCaption()));
//		newCtrl.setCustomLabel(getCustomLabel(oldCtrl));
//		newCtrl.setLabelPosition(verticalCtrlAlignment ? LabelPosition.TOP : LabelPosition.LEFT_SIDE);
//		newCtrl.setToolTip(oldCtrl.getTooltip());
//		newCtrl.setPhi(isPhi(oldCtrl));
//		newCtrl.setMandatory(isMandatory(oldCtrl));
//		newCtrl.setSequenceNumber(oldCtrl.getSequenceNumber());
//		newCtrl.setxPos(oldCtrl.getYPosition());
//		newCtrl.setShowLabel(showLabel(oldCtrl));
//		newCtrl.setShowInGrid(showInGrid(oldCtrl));
//		
//		BaseAbstractAttributeInterface attribute = oldCtrl.getBaseAbstractAttribute();
//		if (attribute != null) {
//			Collection<SemanticPropertyInterface> semanticProps = attribute.getSemanticPropertyCollection();
//			if (semanticProps != null && !semanticProps.isEmpty()) {
//				SemanticPropertyInterface sp = semanticProps.iterator().next();
//				newCtrl.setConceptCode(sp.getConceptCode());
//				newCtrl.setConceptDefinition(sp.getConceptDefinition());
//			}
//		}
//	}
//	
//	private void ensureUniqueUdn(Container c, Control ctrl) {
//		String useDefName = ctrl.getUserDefinedName();
//		if (c.getUserDefCtrlNames().contains(useDefName)) {
//			ctrl.setUserDefinedName(useDefName + (++userDefId));
//		}
//	}
//
//	private String getCtrlName(ControlInterface ctrl) {
//		String name = "";
//		
//		if (ctrl instanceof LabelInterface) {
//			name = new StringBuilder("label").append(ctrl.getId()).toString();
//		} else if (ctrl instanceof AbstractContainmentControlInterface) {
//			name = getAssociationName(ctrl.getBaseAbstractAttribute());
//		} else {
//			name = ctrl.getBaseAbstractAttribute().getName();
//			int idx = name.lastIndexOf(" Category Attribute");
//			if (idx != -1) {
//				name = name.substring(0, idx);
//			}			
//		} 
//		
//		return StringUtils.deleteWhitespace(name).replaceAll(specialChars, "_");
//	}
//	
//	private String getAssociationName(BaseAbstractAttributeInterface attr) {
//		String name = attr.getName();
//		if (attr instanceof CategoryAssociationInterface) {
//			name = getLastPart(attr.getName(), 3);
//		}
//		
//		return name;
//	}	
//	
//	private String getLastPart(String name, int startIdx) {
//		String[] nameParts = name.split("[\\[\\]]");
//		int numParts = nameParts.length;			
//		return new StringBuilder(nameParts[numParts - startIdx])
//			.append(nameParts[numParts - (startIdx - 1)]).toString();		
//	}
//	
//	private String getCustomLabel(ControlInterface ctrl) {
//		BaseAbstractAttribute attr = (BaseAbstractAttribute) ctrl.getBaseAbstractAttribute();
//		return getTaggedValue(attr.getTaggedValueCollection(), "displayLabel");
//	}
//	
//	private boolean isPhi(ControlInterface ctrl) {
//		BaseAbstractAttributeInterface battr = ctrl.getBaseAbstractAttribute();
//		boolean isPhi = false;
//		if (battr instanceof AttributeInterface) {
//			isPhi = bool(((AttributeInterface) battr).getIsIdentified());
//		}
//		// FIXME :: For Association Inerface phi ll not be there .. rite ? 
////		} else if (battr instanceof AssociationInterface) {
////			AssociationInterface assoc = (AssociationInterface)battr;
////			for (AbstractAttributeInterface attr1 : assoc.getTargetEntity().getAllAbstractAttributes()) {
////				if (attr1.getName().equals("id")) {
////					continue;
////				}
////				
////				isPhi = ctrl.getAttibuteMetadataInterface().geta().getIsIdentified();
//////				AttributeInterface attr = (AttributeInterface)battr;
//////				isPhi = bool(battr.ggetIsIdentified());
////			}				
////		}
//		
//		return isPhi;
//	}
//	
//	private boolean isMandatory(ControlInterface ctrl) {
//		RuleInterface rule = getRule(ctrl, "required");
//		return rule != null;
//	}
//	
//	private boolean showLabel(ControlInterface ctrl) {
//		Boolean showLabel = ((edu.common.dynamicextensions.domain.userinterface.Control)ctrl)
//				.getShowLabel();
//		return showLabel != null ? showLabel : true;
//	}
//
//	private boolean showInGrid(ControlInterface ctrl) {
//		boolean showInGrid = false;
//		
//		BaseAbstractAttributeInterface attr = ctrl.getBaseAbstractAttribute();		
//		if (attr != null) {
//			showInGrid = Boolean.parseBoolean(getTaggedValue(attr.getTaggedValueCollection(), "showingrid"));
//		}
//		
//		return showInGrid;
//	}
//	
//	private String getDefaultValue(ControlInterface ctrl) {
//		AttributeMetadataInterface abstractAttribute = (AttributeMetadataInterface)ctrl.getAttibuteMetadataInterface();
//		return abstractAttribute.getDefaultValue();
//	}
//	
//	private AttributeTypeInformationInterface getDataType(ControlInterface ctrl) {
//		BaseAbstractAttributeInterface baseAttr = ctrl.getBaseAbstractAttribute();
//		AttributeInterface attr = null;
//
//		if (baseAttr instanceof CategoryAttributeInterface) {
//			CategoryAttributeInterface catAttr = (CategoryAttributeInterface)baseAttr;
//			baseAttr = catAttr.getAbstractAttribute();
//			
////			if (catAttr.getAbstractAttribute() instanceof AttributeInterface) {
////				attr = (AttributeInterface)catAttr.getAbstractAttribute();
////			} else if (catAttr.getAbstractAttribute() instanceof AssociationInterface) { // case of multi-select control
////				attr = getMultiSelectAttr((AssociationInterface)catAttr.getAbstractAttribute());
////			}			
//		} 
//		
//		if (baseAttr instanceof AttributeInterface) {
//			attr = (AttributeInterface)baseAttr;
//		} else if (baseAttr instanceof AssociationInterface) {
//			attr = getMultiSelectAttr((AssociationInterface)baseAttr);			
//		} else {
//			if (baseAttr != null) {
//				logger.error("Unknown attribute type: " + baseAttr.getClass());
//			}
//			
//			throw new RuntimeException(ctrl.getCaption() + " is neither category attribute nor simple attribute");
//		}
//		
//		return attr.getAttributeTypeInformation();
//	}
//	
//	private AttributeInterface getMultiSelectAttr(AssociationInterface association) {
//		AttributeInterface attr = null;
//		
//		for (AbstractAttributeInterface attr1 : association.getTargetEntity().getAllAbstractAttributes()) {
//			if (attr1.getName().equals("id")) {
//				continue;
//			}
//			
//			attr = (AttributeInterface)attr1;
//			break;
//		}
//		
//		return attr;
//	}
//	
//	private PvDataSource getPvDataSource(SelectControl newSelectCtrl, ControlInterface oldCtrl) {
//		AttributeTypeInformationInterface attrInfo = getDataType(oldCtrl);
//
//		PvDataSource pvDataSource = new PvDataSource();
//		
//		DataType dataType = DataType.STRING;
//		String dateFormat = null;		
//		if (attrInfo instanceof BooleanTypeInformationInterface) {
//			dataType = DataType.BOOLEAN;
//		} else if (attrInfo instanceof DateTypeInformationInterface) {
//			DateTypeInformationInterface dateType = (DateTypeInformationInterface)attrInfo;
//			dataType = DataType.DATE;
//			dateFormat = dateType.getFormat();
//		} else if (attrInfo instanceof DoubleTypeInformationInterface || 
//				attrInfo instanceof FloatTypeInformationInterface) {
//			dataType = DataType.FLOAT;
//		} else if (attrInfo instanceof NumericTypeInformationInterface) {
//			dataType = DataType.INTEGER;
//		}
//
//		BaseAbstractAttributeInterface baseAttr = oldCtrl.getBaseAbstractAttribute();
//
//		List<PermissibleValue> pvs = null;
//		Collection<PermissibleValueInterface> defPvs = null;
//		boolean isOrdered = false;
//		if (baseAttr instanceof CategoryAttributeInterface) {
//			CategoryAttribute cattr = (CategoryAttribute)baseAttr;
//			UserDefinedDE ude = (UserDefinedDE)cattr.getDataElement();
//			pvs = getPvs(ude);
//			defPvs = ude.getPermissibleValueCollection();
//			isOrdered = ude.getIsOrdered();
//			
//		} else if (baseAttr instanceof AssociationInterface) {
//			Attribute mattr = (Attribute)getMultiSelectAttr((AssociationInterface)baseAttr);
//			UserDefinedDE ude = (UserDefinedDE)mattr.getDataElement();
//			pvs = getPvs(ude);
//			defPvs = ude.getPermissibleValueCollection();
//			isOrdered = ude.getIsOrdered();
//			
//		} else {
//			UserDefinedDEInterface userDataElement = (UserDefinedDEInterface) oldCtrl.getAttibuteMetadataInterface().getDataElement();
//			pvs = getPvs(userDataElement);
//			defPvs = userDataElement.getPermissibleValueCollection();
//			isOrdered = userDataElement.getIsOrdered();
//		}
//		PvVersion pvVersion = new PvVersion();
//		pvVersion.setPermissibleValues(pvs);
//			
//		
//		if (defPvs != null && defPvs.size() > 0) {				
//			pvVersion.setDefaultValue(getPv(defPvs.iterator().next()));								
//		}
//		
//		pvDataSource.setDataType(dataType);
//		pvDataSource.setDateFormat(dateFormat);
//		pvDataSource.getPvVersions().add(pvVersion);
//		
//		if (isOrdered) {
//			pvDataSource.setOrdering(Ordering.ASC);
//		}
//
//		return pvDataSource;
//	}
//	
//	private List<PermissibleValue> getPvs(UserDefinedDEInterface userDataElement) {
//		List<PermissibleValue> result = new ArrayList<PermissibleValue>();
//		
//		Collection<PermissibleValueInterface> oldPvs = userDataElement.getPermissibleValues();
//		for (PermissibleValueInterface oldPv : oldPvs) {
//			PermissibleValue newPv = getPv(oldPv);
//			result.add(newPv);
//		}
//					
//		return result;	
//	}
//	
//	private PermissibleValue getPv(PermissibleValueInterface oldPv) {		
//		PermissibleValue newPv = new PermissibleValue();
//		newPv.setValue(oldPv.getValueAsObject().toString());
//		newPv.setOptionName(oldPv.getValueAsObject().toString());
//		
//		Collection<SemanticPropertyInterface> semanticProps = oldPv.getSemanticPropertyCollection();
//		if (semanticProps != null) {
//			Iterator<SemanticPropertyInterface> iterator = semanticProps.iterator();
//			if (iterator.hasNext()) {
//				SemanticPropertyInterface semanticProp = iterator.next();
//				newPv.setConceptCode(semanticProp.getConceptCode());
//			}
//		}
//		
//		return newPv;		
//	}
//	
//	private String getUniqueFormName(String name) {
//		String uniqueName = StringUtils.deleteWhitespace(name).replaceAll(specialChars, "_");
//		int i = 0;
//		while (containerNames.contains(uniqueName)) {                 
//			uniqueName = name + "_" + ++i;
//		}
//		containerNames.add(uniqueName);
//		return uniqueName;
//    }
//	
//	private RuleInterface getRule(ControlInterface ctrl, String ruleName) {
//		BaseAbstractAttributeInterface battr = ctrl.getBaseAbstractAttribute();
//		if (!(battr instanceof AttributeMetadataInterface)) {
//			return null;
//		}
//		
//		AttributeMetadataInterface attr = (AttributeMetadataInterface)battr;
//		Collection<RuleInterface> rules = attr.getRuleCollection();		
//		RuleInterface rule = getRule(rules, ruleName);
//		return rule;
//	}
//	
//	private RuleInterface getRule(Collection<RuleInterface> rules, String ruleName) {
//		RuleInterface result = null;
//		
//		for (RuleInterface rule : rules) {
//			if (rule.getName().equals(ruleName)) {
//				result = rule;
//				break;
//			}
//		}
//		
//		return result;
//	}
//	
//	private void addCharSetRuleIfPresent( Control newCtrl, ControlInterface oldCtrl) {
//		RuleInterface rule = getRule(oldCtrl, "characterSet");
//		if (rule != null) {
//			newCtrl.addValidationRule("characterSet", null);
//		}		
//	}
//	
//	private int getOptionsPerRow(ControlInterface oldCtrl) {
//		BaseAbstractAttributeInterface battr = null;
//		if (oldCtrl != null) {
//			battr = oldCtrl.getBaseAbstractAttribute();
//		}
//		
//		String column = null;
//		if (battr != null) {
//			column = getTaggedValue(battr.getTaggedValueCollection(), "column");
//		}
//		
//		int optionsPerRow = 3;
//		try {
//			if (column != null) {
//				optionsPerRow = Integer.parseInt(column);
//			}				
//		} catch (Exception e) {
//			optionsPerRow = 3;
//			logger.error("Parsing of string to integer failed for optionsPerRow"+ e.getStackTrace());
//		}
//		
//		return optionsPerRow;
//	}
//		
//	
//	private String getTaggedValue(Collection<TaggedValueInterface> taggedValueCollection, String attr) {
//		String attrVal = null;
//		for (TaggedValueInterface tag  : taggedValueCollection) {
//			if (tag.getKey().equals(attr)) {
//				attrVal = tag.getValue();
//			}
//		}
//		return attrVal;
//	}
//
//	private Long saveForm(JdbcDao jdbcDao) 
//	throws Exception {
//		Long formId = null;		
//		
//		try {
//			formId = formMigrationCtxt.newForm.save(usrCtx, jdbcDao);
//		} catch (Exception e) {
//			logger.error("Error saving container"+e.getStackTrace());
//		}
//		return formId;		
//	}
//	
//	private boolean bool(Boolean booleanVal) {
//		return booleanVal != null ? booleanVal : false;
//	}
//
//	private void attachForm(JdbcDao jdbcDao, List<FormInfo> formInfos) throws Exception {
//		attachForm(jdbcDao, formMigrationCtxt.newForm.getId(), formInfos);
//	}
//		
//	protected void attachForm(JdbcDao jdbcDao, Long newFormId, List<FormInfo> formInfos)
//	throws Exception {
//		for (FormInfo info : formInfos) {
//			String entityType = info.getEntityType();
//			Long cpId = info.getCpId();
//			
//			FormDetailsDTO dto = new FormDetailsDTO();
//			dto.setContainerId(newFormId);
//			dto.setEntityType(entityType);
//			dto.setCpId(cpId);
//
//			insertForm(dto);
//			
//			info.setNewFormCtxId(dto.getId());
//			migrateFormData(info);			
//		}
//	}	
//	
//	/////////////////////////////////////////////////////////////////////////////////////////
//	//
//	// Form data migration logic
//	//
//	/////////////////////////////////////////////////////////////////////////////////////////
//	public class RecordObject {
//		Long recordId;
//		Long objectId;
//	}
//	
//	protected void migrateFormData(FormInfo info) 
//	throws Exception{
//		long t1 = System.currentTimeMillis();
//			
//		List<RecordObject> recAndObjectIds = getRecordAndObjectIds(info.getOldFormCtxId());
//		logger.info("Number of records to migrate for form : " + oldForm.getCaption() + "(" + oldForm.getId() + ")" + 
//				" with form context id : " + info.getOldFormCtxId() + " is : " + recAndObjectIds.size());
//		if (recAndObjectIds.size() == 0) {
//			return;
//		}
//		
//	
//		EntityInterface entity = null;
//		CategoryEntity catEntity = null;
//		
//		if (oldForm.getAbstractEntity() instanceof CategoryEntity) {
//			catEntity = (CategoryEntity)oldForm.getAbstractEntity();
//			entity = catEntity.getEntity();
//		} else {
//			entity = (EntityInterface)oldForm.getAbstractEntity();
//		}
//		
//		String tableName = entity.getTableProperties().getName();			
//		String recordIdCol = getRecordIdCol(info, entity.getId(), tableName);
//		
//		if (recordIdCol == null) {
//			throw new RuntimeException("Could not determine record id column for " + tableName + 
//					", old container : " + oldForm.getId());
//		}
//		
//		logger.info("Using record ID column: " + recordIdCol + 
//				" for table: " + tableName +
//				" while migrating: " + oldForm.getId());
//		
//		for (RecordObject recObj : recAndObjectIds) {
//			
//			try {
//				Long oldRecId = getRecordId(tableName, recordIdCol, recObj.recordId); 
//				if (oldRecId == null) {
//					continue;
//				}
//				
//				Map<BaseAbstractAttributeInterface, Object> record = null;
//				if (catEntity != null) {
//					record = CategoryManager.getInstance().getRecordById(catEntity, oldRecId);
//				} else {
//					Map<AbstractAttributeInterface, Object> entityRec = EntityManager.getInstance().getRecordById(entity, oldRecId);
//					record = new HashMap<BaseAbstractAttributeInterface, Object>(entityRec);
//				}
//				
//				DataValueMapUtility.updateDataValueMapDataLoading(record, oldForm);
//				migrateFormData(recObj, record, info);						
//			} catch (DynamicExtensionsSystemException e) {
//				if (!e.getMessage().startsWith("Exception in execution query :: Unhooked data present in database for recordEntryId")) {
//					throw e;
//				} else {
//					log(recObj.recordId, null, e.getMessage());
//				}
//				logger.warn(e.getMessage());
//			}
//		}
//		
//		logger.info("Migrated records for form: " + oldForm.getCaption() + "(" + oldForm.getId() + ")" + 
//				", number of records = " + recAndObjectIds.size() + 
//				", time = " + (System.currentTimeMillis() - t1) / 1000 + " seconds"); 
//	}
//	
//	private String getRecordIdCol(FormInfo info, Long entityId, String tableName) {
//		String recordIdCol = "DYEXTN_AS_" + getHookEntityId(info, oldForm.getId()) + "_" + entityId;
//		if (doesColumnExists(tableName, recordIdCol)) {
//			return recordIdCol;
//		}
//		
//		if (info.isDefaultForm()) {
//			return null;
//		}
//		
//		info.setDefaultForm(true);
//		recordIdCol = "DYEXTN_AS_" + getHookEntityId(info, oldForm.getId()) + "_" + entityId;
//		if (doesColumnExists(tableName, recordIdCol)) {
//			info.setDefaultForm(false);
//			return recordIdCol;
//		}
//		
//		return null;				
//	}
//	
//	private boolean doesColumnExists(String table, String column) {
//		try {
//			String sql = String.format(CHECK_REC_ID_EXISTS_SQL, column, table, column);
//			JdbcDaoFactory.getJdbcDao().getResultSet(sql, null, new ResultExtractor<Boolean>() {
//				@Override
//				public Boolean extract(ResultSet rs) throws SQLException {
//					return true;
//				}
//			});
//			
//			return true;
//		} catch (Exception e) {
//			return false;
//		}		
//	}
//	
//	private Long getRecordId(String tableName, String recordIdCol, Long oldRecId) {
//		String query = String.format(GET_ID_FROM_ASSO_ID_SQL, tableName, recordIdCol);
//		JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
//		List<Object> param = new ArrayList<Object>();
//		param.add(oldRecId);
//		return jdbcDao.getResultSet(query, param, new ResultExtractor<Long>() {
//			@Override
//			public Long  extract(ResultSet rs) throws SQLException {
//				return rs.next() ? rs.getLong(1) : null; 
//			}			
//		});
//	}
//
//	private Long getHookEntityId(FormInfo info, Long containerId) {
//		List<Object> params = new ArrayList<Object>();
//		params.add(containerId);
//		
//		Long key = null;
//		
//		if (info.isDefaultForm()) {
//			key = staticEntityMap.get("default"+info.getEntityType());
//		} else {
//			key = staticEntityMap.get(info.getEntityType());
//		}
//		
//		return key;
//	}
//
//	protected List<RecordObject> getRecordAndObjectIds(Long oldCtxId) {										
//		return JdbcDaoFactory.getJdbcDao().getResultSet(
//				GET_RECORD_AND_OBJECT_IDS_SQL, 
//				Collections.singletonList(oldCtxId), 
//				new ResultExtractor<List<RecordObject>>() {
//					@Override
//					public List<RecordObject> extract(ResultSet rs)
//					throws SQLException {
//						List<RecordObject> recAndObjectIds = new ArrayList<RecordObject>();
//
//						while (rs.next()) {
//							RecordObject recObj = new RecordObject();
//							recObj.recordId = rs.getLong(1);
//							Long scgId = rs.getLong(2), 
//								pId    = rs.getLong(3), 
//								spId   = rs.getLong(4);
//
//							if (scgId != null && scgId > 0) { // SCG
//								recObj.objectId = scgId;
//							} else if (pId != null && pId > 0) { // Participant
//								recObj.objectId = pId;
//							} else if (spId != null && spId > 0) { // Specimen
//								recObj.objectId = spId;
//							} else {
//								recObj.objectId = -1L;
//							}
//
//							if (recObj.objectId == -1) {
//								continue;
//							}
//
//							recAndObjectIds.add(recObj);
//						}
//
//						return recAndObjectIds;
//					}
//		});
//	}
//	
//	public void insertForm(FormDetailsDTO form) {
//		try {
//			List<Object> params = new ArrayList<Object>();
//			params.add(form.getContainerId());
//			params.add(form.getEntityType());
//			params.add(form.getCpId());
//			params.add(0);
//			params.add(true);
//			
//			String query = DbSettingsFactory.getProduct().equals("Oracle") ? INSERT_FORM_CTXT_SQL_ORACLE 
//							: INSERT_FORM_CTXT_SQL_MYSQL;
//			Number id = JdbcDaoFactory.getJdbcDao().executeUpdateAndGetKey(query, params, "identifier");
//		
//			form.setId(id.longValue());							
//		} catch (Exception e) {
//			throw new RuntimeException("Error inserting form context", e);
//		}
//	}
//	
//	private void migrateFormData(RecordObject recObj, Map<BaseAbstractAttributeInterface, Object> oldRecord, FormInfo info) 
//	throws Exception {
//
//		JdbcDao jdbcDao = JdbcDaoFactory.getJdbcDao();
//		FormData newRecord = getFormData(jdbcDao, recObj, formMigrationCtxt, oldForm, oldRecord);
//		FormDataManager formDataMgr = new FormDataManagerImpl(false);
//		Long newRecordId = formDataMgr.saveOrUpdateFormData(null, newRecord, jdbcDao);
//		
//		List<Object> params = new ArrayList<Object>();
//		params.add(info.getNewFormCtxId());
//		params.add(recObj.objectId);
//		params.add(newRecordId);
//		params.add(usrCtx.getUserId());
//		params.add(new Timestamp(Calendar.getInstance().getTimeInMillis()));
//		params.add(Status.ACTIVE.toString());
//
//		String query = DbSettingsFactory.getProduct().equals("Oracle") ? INSERT_RECORD_ENTRY_SQL_ORACLE 
//						: INSERT_RECORD_ENTRY_SQL_MYSQL;
//		jdbcDao.executeUpdate(query, params);
//		
//		log(recObj.recordId, newRecordId, "");
//	}
//	
//			
//	protected FormData getFormData(
//			JdbcDao jdbcDao, RecordObject recObj, 
//			FormMigrationCtxt formMigrationCtxt, ContainerInterface oldForm,
//			Map<BaseAbstractAttributeInterface, Object> oldFormData) {
//				
//		Container newForm = formMigrationCtxt.newForm;
//		Map<BaseAbstractAttributeInterface, Object> fieldMap = formMigrationCtxt.fieldMap;
//		
//		FormData formData = new FormData(newForm);		
//		for (ControlInterface oldCtrl : oldForm.getAllControlsUnderSameDisplayLabel()) {
//			BaseAbstractAttributeInterface oldAttr = oldCtrl.getBaseAbstractAttribute();
//
//			Object newValue = null;
//			if (oldAttr instanceof CategoryAttributeInterface || oldAttr instanceof AttributeInterface) {
//				Control newControl = (Control)fieldMap.get(oldAttr);
//				Object oldValue = oldFormData.get(oldAttr);
//				if (oldValue instanceof List) {
//					newValue = getMultiSelectValues((List<Map<BaseAbstractAttributeInterface, Object>>)oldValue); 
//				} else if (oldCtrl instanceof FileUploadInterface){
//					newValue = getNewFileControlValue(jdbcDao, oldFormData, (AttributeInterface)oldAttr);
//				} else {
//					newValue = oldValue;
//				}
//				ControlValue cv = new ControlValue(newControl, newValue);
//				formData.addFieldValue(cv);				
//			} else if ((oldAttr instanceof CategoryAssociationInterface || oldAttr instanceof AssociationInterface) 
//					&& oldCtrl instanceof AbstractContainmentControlInterface) {				
//				FormMigrationCtxt sfMigrationCtxt = (FormMigrationCtxt)fieldMap.get(oldAttr);				
//				Control newSfCtrl = sfMigrationCtxt.sfCtrl;
//				
//				List<Map<BaseAbstractAttributeInterface, Object>> oldSfRecs = (List) oldFormData.get(oldAttr);
//				List<FormData> newSfData = new ArrayList<FormData>();
//				
//				AbstractContainmentControlInterface oldSfCtrl = (AbstractContainmentControlInterface)oldCtrl;
//				for (Map<BaseAbstractAttributeInterface, Object> oldSfRec : oldSfRecs) {
//					newSfData.add(getFormData(jdbcDao, recObj, sfMigrationCtxt, oldSfCtrl.getContainer(), oldSfRec));
//				}
//				
//				ControlValue cv = new ControlValue(newSfCtrl, newSfData);
//				formData.addFieldValue(cv);
//			} else if (oldAttr instanceof AssociationInterface) { // multiselect 
//				Control newControl = (Control)fieldMap.get(oldAttr);
//				Object oldValue = oldFormData.get(oldAttr);
//				if (oldValue instanceof List) {
//					newValue = getMultiSelectValues((List<Map<BaseAbstractAttributeInterface, Object>>)oldValue); 
//				} else {
//					newValue = oldValue;
//				}
//				
//				ControlValue cv = new ControlValue(newControl, newValue);
//				formData.addFieldValue(cv);								
//			} else {
//				System.err.println("[ERROR]: Unknown attribute type: " + oldAttr.getName() + ": " + oldAttr.getClass());
//			}
//		}
//		
//		return formData;
//	}
//	
//	private void log(Long oldRecId, Long newRecId, String failReason) 
//	throws IOException {
//		if (recordsLog == null) {
//			return;
//		}
//		
//		String status = "SUCCESS";
//		if (newRecId == null) {
//			status = "FAIL";
//		} else {
//			failReason = "";
//		}
//		
//		recordsLog.writeNext(new String[] {
//				getFormCaption(),
//				oldRecId.toString(),
//				newRecId.toString(),
//				status,
//				failReason
//		});
//		
//		logEntriesCnt++;
//		if (logEntriesCnt >= 25) {
//			logEntriesCnt = 0;
//			recordsLog.flush();
//		}
//	}
//	
//	private FileControlValue getNewFileControlValue(JdbcDao jdbcDao, Map<BaseAbstractAttributeInterface, Object> oldFormData, 
//			AttributeInterface attr) {
//
//		EntityInterface entity = attr.getEntity();
//		EntityRecord entityRecord = new EntityRecord();
//		entityRecord.setId(entity.getId());
//		entityRecord.setName(entity.getName());
//		Long recordId = (Long)oldFormData.get(entityRecord);
//		if (recordId == null) {
//			logger.warn("Could not obtain record id in " + oldFormData);
//			logger.warn("Key used was: " + entity.getId() + ":" + entity.getName());
//			return null;
//		}
//		
//		String tableName = attr.getEntity().getTableProperties().getName();
//		String columnName = attr.getColumnProperties().getName();
//		
//		FileControlValue fcv = null;
//		String sql = String.format(GET_FILE_CONTENT, columnName, columnName, columnName, tableName);
//		List<Object> params = new ArrayList<Object>();
//		params.add(recordId);
//			
//		fcv = jdbcDao.getResultSet(sql, params, new ResultExtractor<FileControlValue>() {
//			@Override
//			public FileControlValue extract(ResultSet rs) throws SQLException {
//				FileControlValue fcv = new FileControlValue();
//				if (rs.next()) {
//					Blob fileContent   = rs.getBlob(1);
//					String fileName    = rs.getString(2);
//					String contentType = rs.getString(3);
//					
//					fcv = new FileControlValue();
//					fcv.setFileName(fileName);
//					fcv.setContentType(contentType);	
//					
//					if (fileContent == null) {
//						return fcv;
//					}
//				
//					String fileId = FileUploadMgr.getInstance().saveFile(fileContent.getBinaryStream());
//					fcv.setFileId(fileId);
//				}	
//				return fcv;
//			}
//		});
//		
//		return fcv;
//	}
//	
//	private File copyBlobToTempFile(Blob blob) {
//		File file = null;
//		FileOutputStream fout = null;
//		
//		try {
//			file = File.createTempFile("form-migrate", ".dat");
//			fout = new FileOutputStream(file);
//			IoUtil.copy(blob.getBinaryStream(), fout);			
//		} catch (Exception e) {
//			IoUtil.delete(file);			
//			throw new RuntimeException("Error copying blob data to file", e);
//		} finally {
//			IoUtil.close(fout);
//		}
//		
//		return file;
//	}
//	
//	private String[] getMultiSelectValues(List<Map<BaseAbstractAttributeInterface, Object>> msValuesMap) {
//		List<String> values = new ArrayList<String>();
//		
//		for (Object dataValue : msValuesMap) {
//			if (dataValue == null) {
//				continue;
//			}
//			
//			Map<BaseAbstractAttributeInterface, Object> valueMap = (Map<BaseAbstractAttributeInterface, Object>) dataValue;
//			for (Object obj : valueMap.values()) {
//				if (obj.toString() == null || obj.toString().trim().isEmpty()) {
//					continue;
//				}
//				
//				values.add(obj.toString());
//			}
//		}
//		
//		return values.toArray(new String[0]);
//	}
//	
//	//
//	// Converts camel case caption to regular caption
//	// thicknessInMicrons => Thickness In Microns
//	//
//	private String getCaption(String input) {
//		String[] tokens = splitByCamelCase(input);
//		if (tokens.length > 0) {
//			tokens[0] = StringUtils.capitalize(tokens[0]);
//		}
//		
//		StringBuilder result = new StringBuilder();
//		for (int i = 0; i < tokens.length; ++i) {
//			if (tokens[i] == null || tokens[i].trim().isEmpty()) {
//				continue;
//			}
//						
//			if (result.length() > 0) {
//				result.append(" ");
//			}			
//			result.append(tokens[i].trim());
//		}
//		
//		return result.toString();		
//	}
//
//	private String[] splitByCamelCase(String str) {
//		if (str == null) {
//			return null;
//		}
//
//		if (str.length() == 0) {
//			return ArrayUtils.EMPTY_STRING_ARRAY;
//		}
//
//		char[] c = str.trim().toCharArray();
//		List<String> list = new ArrayList<String>();
//		int tokenStart = 0;
//		int currentType = Character.getType(c[tokenStart]);
//		for (int pos = tokenStart + 1; pos < c.length; pos++) {
//			int type = Character.getType(c[pos]);
//			if (type == currentType) {
//				continue;
//			}
//			
//			if (type == Character.LOWERCASE_LETTER && currentType == Character.UPPERCASE_LETTER) {
//				int newTokenStart = pos - 1;
//				if (newTokenStart != tokenStart) {
//					list.add(new String(c, tokenStart, newTokenStart - tokenStart));
//					tokenStart = newTokenStart;
//				}
//			} else {
//				list.add(new String(c, tokenStart, pos - tokenStart));
//				tokenStart = pos;
//			}
//			
//			currentType = type;
//		}
//
//		list.add(new String(c, tokenStart, c.length - tokenStart));
//		return list.toArray(new String[list.size()]);
//	}
//	
//	
//	private static final String GET_STATIC_ENTITY_ID_NAME_SQL = 
//		"select " +
//		"	dam.identifier, dam.name " +
//		"from " +
//		"	dyextn_abstract_metadata dam " + 
//		"	inner join dyextn_entity de on de.identifier = dam.identifier "+ 
//		"where dam.name in " +
//		" ( " +
//		"	'edu.wustl.catissuecore.domain.Participant', " +
//		"	'edu.wustl.catissuecore.domain.Specimen', " +
//		"	'edu.wustl.catissuecore.domain.SpecimenCollectionGroup', " +
//		"	'edu.wustl.catissuecore.domain.deintegration.ParticipantRecordEntry', " +
//		"	'edu.wustl.catissuecore.domain.deintegration.SpecimenRecordEntry', " +
//		"	'edu.wustl.catissuecore.domain.deintegration.SCGRecordEntry', " +
//		"	'edu.wustl.catissuecore.domain.deintegration.ActionApplicationRecordEntry'" +
//		" ) ";
//	
//	private static final String GET_RECORD_AND_OBJECT_IDS_SQL =
//		"select " +
//		"	re.identifier, specimen_collection_group_id, cpr.identifier, specimen_id " +
//		"from dyextn_abstract_form_context afc " +
//		"	inner join dyextn_abstract_record_entry re on re.abstract_form_context_id = afc.identifier " +
//		"	left join ( " +
//		"	  catissue_participant_rec_ntry p_re " + 
//		"	  inner join catissue_coll_prot_reg cpr on cpr.participant_id = p_re.participant_id " +
//		"	) on p_re.identifier = re.identifier " +
//		"	left join catissue_scg_rec_ntry scg_re on scg_re.identifier = re.identifier " + 
//		"	left join catissue_specimen_rec_ntry sp_re on sp_re.identifier = re.identifier " +
//		"where afc.identifier = ?";
//
//	
//	private static final String GET_FILE_CONTENT = 
//			"select %s, %s_file_name, %s_content_type from %s where IDENTIFIER = ?";
//	
//	
//	private static final String GET_ID_FROM_ASSO_ID_SQL =
//			" select identifier from %s where %s= ?";
//	
//	private static final String INSERT_FORM_CTXT_SQL_MYSQL = 
//			"insert into catissue_form_context values(default, ?, ?, ?, ?, ?)";
//	
//	
//	private static final String INSERT_FORM_CTXT_SQL_ORACLE = 
//			"insert into catissue_form_context values(CATISSUE_FORM_CONTEXT_SEQ.NEXTVAL, ?, ?, ?, ?, ?)";
//	
//	
//	private static final String INSERT_RECORD_ENTRY_SQL_MYSQL = 
//			"insert into catissue_form_record_entry values(default, ?, ?, ?, ?, ?, ?)";
//	
//	private static final String INSERT_RECORD_ENTRY_SQL_ORACLE = 
//			"insert into catissue_form_record_entry values(CATISSUE_FORM_REC_ENTRY_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?)";
//	
//	private static final String CHECK_REC_ID_EXISTS_SQL = 
//			"select %s from %s where %s < 0";
//	
//}
