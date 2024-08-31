package com.krishagni.catissueplus.core.biospecimen.domain;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.NotAudited;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

public class BaseEntity {

	private static final LogUtil logger = LogUtil.getLogger(BaseEntity.class);

	private static final Map<String, Set<String>> entityProperties = new ConcurrentHashMap<>();

	private static final Map<String, Boolean> entityNameProperties = new ConcurrentHashMap<>();

	public enum DataEntryStatus {
		DRAFT, COMPLETE;
	}

	protected Long id;

	protected User creator;

	protected Date creationTime;

	protected User updater;

	protected Date updateTime;

	protected DataEntryStatus dataEntryStatus;
	
	protected transient List<Runnable> onSaveProcs;

	protected transient String opComments;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public User getUpdater() {
		return updater;
	}

	public void setUpdater(User updater) {
		this.updater = updater;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public DataEntryStatus getDataEntryStatus() {
		return dataEntryStatus;
	}

	public void setDataEntryStatus(DataEntryStatus dataEntryStatus) {
		this.dataEntryStatus = dataEntryStatus;
	}

	public BaseEntity getRoot() {
		return null;
	}

	public List<Runnable> getOnSaveProcs() {
		return onSaveProcs;
	}

	public void setOnSaveProcs(List<Runnable> onSaveProcs) {
		this.onSaveProcs = onSaveProcs;
	}

	public void addOnSaveProc(Runnable onSaveProc) {
		if (onSaveProcs == null) {
			onSaveProcs = new ArrayList<>();
		}

		onSaveProcs.add(onSaveProc);
	}

	public String getOpComments() {
		return opComments;
	}

	public void setOpComments(String opComments) {
		this.opComments = opComments;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		return prime + ((getId() == null) ? 0 : getId().hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null	|| getClass() != getClassWithoutInitializingProxy(obj)) {
			return false;
		}

		BaseEntity other = (BaseEntity) obj;
		return getId() != null && getId().equals(other.getId());
	}

	public boolean sameAs(Object obj) {
		return equals(obj);
	}

	public String toAuditString() {
		return toAuditString(Collections.emptySet());
	}

	public String toAuditString(Set<String> requiredProps) {
		List<String> props = getAuditStringInclusionProps().stream().sorted().collect(Collectors.toList());

		Set<String> exProps = getAuditStringExclusionProps();
		if (CollectionUtils.isNotEmpty(exProps)) {
			props = props.stream().filter(prop -> !exProps.contains(prop)).collect(Collectors.toList());
		}

		if (CollectionUtils.isNotEmpty(requiredProps)) {
			props = props.stream().filter(requiredProps::contains).collect(Collectors.toList());
		} else {
			requiredProps = Collections.emptySet();
		}

		BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(this);

		StringBuilder result = new StringBuilder();
		for (String prop : props) {
			String value = getPropValue(bean, prop);
			if (requiredProps.contains(prop) || StringUtils.isNotBlank(value)) {
				result.append(prop).append("=").append(value).append(",");
			}
		}

		if (result.length() > 0) {
			result.deleteCharAt(result.length() - 1);
		}

		return result.toString();
	}

	protected Set<String> getAuditStringInclusionProps() {
		return getProperties();
	}

	protected Set<String> getAuditStringExclusionProps() {
		return Collections.emptySet();
	}

	protected Class getClassWithoutInitializingProxy(Object obj) {
		if (obj instanceof HibernateProxy) {
			HibernateProxy proxy = (HibernateProxy) obj;
			LazyInitializer li = proxy.getHibernateLazyInitializer();
			return li.getPersistentClass();
		} else {
			return obj.getClass();
		}
	}

	private Set<String> getProperties() {
		Class<?> klass = getClassWithoutInitializingProxy(this);
		Set<String> properties = entityProperties.get(klass.getName());
		if (properties != null) {
			return properties;
		}

		try {
			properties = new LinkedHashSet<>();

			for (Field field : getClass().getDeclaredFields()) {
				if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
					continue;
				}

				String name = field.getName();
				String capitalizedName = name.substring(0, 1).toUpperCase() + name.substring(1);

				if (!hasMethod("set" + capitalizedName, field.getType())) {
					continue;
				}

				if (!hasMethod("get" + capitalizedName) &&
					(!isBooleanType(field.getType()) || !hasMethod("is" + capitalizedName))) {
					continue;
				}

				properties.add(name);
			}

			entityProperties.put(klass.getName(), properties);
			return properties;
		} catch (Exception e) {
			throw new RuntimeException("Error querying entity properties", e);
		}
	}

	private boolean hasMethod(String methodName, Class<?> ... parameters) {
		try {
			Method method = getClass().getDeclaredMethod(methodName, parameters);
			return method != null && method.getDeclaredAnnotation(NotAudited.class) == null;
		} catch (NoSuchMethodException nsme) {
			return false;
		}
	}

	private boolean isBooleanType(Class<?> klass) {
		return klass.getSimpleName().equalsIgnoreCase("boolean");
	}

	private String getPropValue(BeanWrapper bean, String prop) {
		String result;

		try {
			Object value = bean.getPropertyValue(prop);
			result = toObjectString(value);
		} catch (Exception e) {
			result = "Error - " + e.getMessage();
			logger.error("Encountered error when obtaining the value of the property " + prop + " on bean", e);
		}

		return result;
	}

	private String toObjectString(Object obj) {
		String result = null;

		if (isSimpleType(obj)) {
			result = obj.toString();
		} else if (obj instanceof BigDecimal) {
			result = ((BigDecimal) obj).setScale(6, RoundingMode.HALF_UP).toString();
		} else if (obj instanceof Number) {
			result = obj.toString();
		} else if (obj instanceof Iterable) {
			result = toCollectionString((Iterable) obj);
		} else if (isAssignableFrom(BaseEntity.class, obj)) {
			result = getObjId(obj);
		} else if (obj instanceof Map) {
			result = Utility.mapToJson((Map) obj);
		} else if (obj != null) {
			result = getObjId(obj);
		}

		return result;
	}

	private boolean isSimpleType(Object value) {
		if (value == null) {
			return false;
		}

		return value instanceof String ||
			ClassUtils.isPrimitiveOrWrapper(value.getClass()) ||
			value instanceof Date ||
			value.getClass().isEnum();
	}

	private String toCollectionString(Iterable<?> collection) {
		try {
			StringBuilder collStr = new StringBuilder();
			for (Object element : collection) {
				if (isDeleted(element)) {
					continue;
				}

				collStr.append(toObjectString(element)).append(",");
			}

			if (collStr.length() > 0) {
				collStr.deleteCharAt(collStr.length() - 1);
			}

			return collStr.length() > 0 ? "[" + collStr.toString() + "]" : null;
		} catch (Exception e) {
			logger.error("Error converting the collection of elements to string", e);
			return null;
		}
	}

	private boolean isDeleted(Object obj) {
		if (!isAssignableFrom(BaseEntity.class, obj)) {
			return false;
		}

		try {
			Object status = PropertyAccessorFactory.forBeanPropertyAccess(obj).getPropertyValue("activityStatus");
			return status != null && status.toString().equals(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		} catch (Exception e) {
		}

		return false;
	}

	private boolean isAssignableFrom(Class<?> superClass, Object value) {
		return value != null && superClass.isAssignableFrom(value.getClass());
	}

	private String getObjId(Object value) {
		if (value == null) {
			return StringUtils.EMPTY;
		}

		BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(value);
		String id = null, name = null;
		try {
			Object idObj = bean.getPropertyValue("id");
			id = idObj == null ? StringUtils.EMPTY : idObj.toString();
		} catch (Exception e) {
			if (isAssignableFrom(BaseEntity.class, value)) {
				id = "Unknown property: " + value.getClass().getName() + ".id: " + e.getMessage();
			}
		}

		try {
			String className = getClassWithoutInitializingProxy(value).getName();
			if (!entityNameProperties.containsKey(className)) {
				PropertyDescriptor[] descriptors = bean.getPropertyDescriptors();
				entityNameProperties.put(className, Arrays.stream(descriptors).anyMatch(d -> d.getName().equals("name")));
			}

			if (entityNameProperties.get(className)) {
				Object nameObj = bean.getPropertyValue("name");
				name = nameObj == null ? StringUtils.EMPTY : nameObj.toString();
			}
		} catch (Exception e) {
			// name = "Unknown property: " + value.getClass().getName() + ".name: " + e.getMessage();
		}

		String result = "{";
		if (id != null) {
			result += "id=" + id;
		}

		if (name != null) {
			if (id != null) {
				result += ", ";
			}

			result += "name=" + name;
		}

		result += "}";
		return result;
	}
}