package com.krishagni.catissueplus.core.importer.domain;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.Dom4JDriver;
import com.thoughtworks.xstream.security.NoTypePermission;

public class ObjectSchema {
	private static AtomicInteger idGen = new AtomicInteger(0);

	private String name;

	private boolean flattened;

	private String fieldSeparator;
	
	private Record record;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isFlattened() {
		return flattened;
	}

	public void setFlattened(boolean flattened) {
		this.flattened = flattened;
	}

	public String getFieldSeparator() {
		return fieldSeparator;
	}

	public void setFieldSeparator(String fieldSeparator) {
		if (StringUtils.isNotBlank(fieldSeparator)) {
			fieldSeparator = fieldSeparator.substring(0, 1);
		}

		this.fieldSeparator = fieldSeparator;
	}

	public Record getRecord() {
		return record;
	}

	public void setRecord(Record record) {
		this.record = record;
	}

	public List<String> getKeyColumnNames() {
		return record.getFields().stream().filter(Field::isKey).map(Field::getCaption).collect(Collectors.toList());
	}
	
	public static ObjectSchema parseSchema(String filePath) {
		XStream parser = getSchemaParser();		
		return (ObjectSchema)parser.fromXML(new File(filePath));
	}
	
	public static ObjectSchema parseSchema(InputStream in) {
		XStream parser = getSchemaParser();
		return (ObjectSchema)parser.fromXML(in);
	}
	
	private static XStream getSchemaParser() {
		XStream xstream = new XStream(new PureJavaReflectionProvider(), new Dom4JDriver());
		xstream.addPermission(NoTypePermission.NONE);
		xstream.allowTypes(new Class<?>[] {ObjectSchema.class, Record.class, Field.class});

		xstream.alias("object-schema", ObjectSchema.class);
		xstream.aliasAttribute(ObjectSchema.class, "fieldSeparator", "field-separator");
		
		xstream.alias("record", Record.class);
		xstream.aliasAttribute(Record.class, "type", "type");
		xstream.aliasAttribute(Record.class, "cpBased", "cpBased");
		xstream.aliasAttribute(Record.class, "entityType", "entityType");
		xstream.aliasAttribute(Record.class, "digestAttribute", "digest");
		xstream.addImplicitCollection(Record.class, "subRecords", "record", Record.class);
		
		xstream.alias("field", Field.class);
		xstream.addImplicitCollection(Record.class, "fields", "field", Field.class);
		
		return xstream;
	}

	public static class Record {
		private Integer id = idGen.incrementAndGet();

		private String name;
		
		private String attribute;
		
		private String caption;
		
		private String type;

		private boolean cpBased = true; // for backward compatibility, default value is true
		
		private String entityType;
		
		private boolean multiple;

		private String digestAttribute;
		
		private List<Record> subRecords;
		
		private List<Field> fields;

		private Collection<Object> orderedFields;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAttribute() {
			return attribute;
		}

		public void setAttribute(String attribute) {
			this.attribute = attribute;
		}

		public String getCaption() {
			return StringUtils.trim(caption);
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public boolean isCpBased() {
			return cpBased;
		}

		public void setCpBased(boolean cpBased) {
			this.cpBased = cpBased;
		}

		public String getEntityType() {
			return entityType;
		}

		public void setEntityType(String entityType) {
			this.entityType = entityType;
		}

		public boolean isMultiple() {
			return multiple;
		}

		public void setMultiple(boolean multiple) {
			this.multiple = multiple;
		}

		public String getDigestAttribute() {
			return digestAttribute;
		}

		public void setDigestAttribute(String digestAttribute) {
			this.digestAttribute = digestAttribute;
		}

		public List<Record> getSubRecords() {
			return subRecords == null ? Collections.emptyList() : subRecords;
		}

		public void setSubRecords(List<Record> subRecords) {
			this.subRecords = subRecords;
			this.orderedFields = null;
		}

		public List<Field> getFields() {
			return fields == null ? Collections.emptyList() : fields;
		}

		public void setFields(List<Field> fields) {
			this.fields = fields;
			this.orderedFields = null;
		}

		public Collection<Object> getOrderedFields() {
			if (orderedFields != null) {
				return orderedFields;
			}

			TreeMap<Integer, Object> result = new TreeMap<>();
			if (fields != null) {
				fields.forEach(field -> result.put(field.getId(), field));
			}

			if (subRecords != null) {
				subRecords.forEach(sr -> result.put(sr.getId(), sr));
			}

			orderedFields = result.values();
			return orderedFields;
		}
	}
	
	public static class Field {
		private Integer id = idGen.incrementAndGet();

		private String caption;
		
		private String attribute;
		
		private String type;

		private String file;
		
		private boolean multiple;
		
		private boolean key;

		public Integer getId() {
			return id;
		}

		public String getCaption() {
			return StringUtils.trim(caption);
		}

		public void setCaption(String caption) {
			this.caption = caption;
		}

		public String getAttribute() {
			return attribute;
		}

		public void setAttribute(String attribute) {
			this.attribute = attribute;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getFile() {
			return file;
		}

		public void setFile(String file) {
			this.file = file;
		}

		public boolean isMultiple() {
			return multiple;
		}

		public void setMultiple(boolean multiple) {
			this.multiple = multiple;
		}

		public boolean isKey() {
			return key;
		}

		public void setKey(boolean key) {
			this.key = key;
		}
	}
}
