package com.krishagni.catissueplus.core.init;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;
import org.springframework.beans.factory.InitializingBean;

public class EnversEnumHandler implements InitializingBean {

	SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void afterPropertiesSet()
	throws Exception {
		SessionFactoryImplementor sessionFactoryImpl = (SessionFactoryImplementor) sessionFactory;
		TypeConfiguration typeConfiguration = sessionFactoryImpl.getTypeConfiguration();
		JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();

		MappingMetamodel metamodel = (MappingMetamodel) sessionFactory.getMetamodel();
//		MetamodelImplementor metamodel = (MetamodelImplementor) sessionFactory.getMetamodel();
//		Map<String, EntityPersister> persisters = metamodel.entityPersisters();
		metamodel.forEachEntityDescriptor(
			persister -> {
				try {
					for (Type type : persister.getPropertyTypes()) {
						if (!(type instanceof CustomType)) {
							continue;
						}

						CustomType customType = (CustomType) type;
						UserType userType = customType.getUserType();
						if (!isEnumType(userType)) {
							continue;
						}

						Class enumClass = getEnumClass(userType);
						if (enumClass == null) {
							continue;
						}

						EnumJavaType enumTypeDescriptor = new OsEnumJavaType(enumClass);
						((org.hibernate.type.EnumType) userType).setEnumJavaType(enumTypeDescriptor);
						javaTypeRegistry.addDescriptor(enumTypeDescriptor);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		);
	}

	private boolean isEnumType(UserType userType) {
		return userType != null && "org.hibernate.type.EnumType".equals(userType.getClass().getName());
	}

	private Class getEnumClass(UserType userType) {
		try {
			Object enumClass = userType.getClass().getMethod("getEnumClass").invoke(userType);
			if (enumClass instanceof Class) {
				return (Class) enumClass;
			}
		} catch (Exception e) {
			// Fall back below
		}

		try {
			Object enumClass = userType.getClass().getMethod("returnedClass").invoke(userType);
			if (enumClass instanceof Class) {
				return (Class) enumClass;
			}
		} catch (Exception e) {
			return null;
		}

		return null;
	}

	private static class OsEnumJavaType<T extends Enum<T>> extends EnumJavaType<T> {
		private OsEnumJavaType(Class<T> type) {
			super(type);
		}

		public T fromName(String relationalForm) {
			if ( relationalForm == null ) {
				return null;
			}

			try {
				return super.fromName(relationalForm);
			} catch (IllegalArgumentException iae) {
				try {
					return super.fromOrdinal(Integer.parseInt(relationalForm));
				} catch (Exception e) {
					throw iae;
				}
			}
		}
	}
}
