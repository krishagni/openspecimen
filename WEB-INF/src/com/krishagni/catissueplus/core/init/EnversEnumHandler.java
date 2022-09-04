package com.krishagni.catissueplus.core.init;

import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.SessionFactory;
import org.hibernate.internal.TypeLocatorImpl;
import org.hibernate.metamodel.model.convert.spi.EnumValueConverter;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;
import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;
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
		TypeLocatorImpl typeHelper = (TypeLocatorImpl) sessionFactory.getTypeHelper();
		TypeResolver typeResolver = (TypeResolver) FieldUtils.readField(typeHelper, "typeResolver", true);
		TypeConfiguration typeConfiguration = (TypeConfiguration) FieldUtils.readField(typeResolver, "typeConfiguration", true);

		MetamodelImplementor metamodel = (MetamodelImplementor) sessionFactory.getMetamodel();
		Map<String, EntityPersister> persisters = metamodel.entityPersisters();
		for (EntityPersister persister : persisters.values()) {
			for (Type type : persister.getPropertyTypes()) {
				if (!(type instanceof CustomType)) {
					continue;
				}

				CustomType customType = (CustomType) type;
				UserType userType = customType.getUserType();
				if (!(userType instanceof EnumType)) {
					continue;
				}

				EnumType enumType = (EnumType) userType;
				EnumJavaTypeDescriptor enumTypeDescriptor = new OsEnumJavaTypeDescriptor(enumType.returnedClass());

				EnumValueConverter converter = (EnumValueConverter) FieldUtils.readDeclaredField(enumType, "enumValueConverter", true);
				FieldUtils.writeDeclaredField(converter, "enumJavaDescriptor", enumTypeDescriptor, true);

				JavaTypeDescriptorRegistry.INSTANCE.addDescriptor(enumTypeDescriptor);
				typeConfiguration.getJavaTypeDescriptorRegistry().addDescriptor(enumTypeDescriptor);
			}
		}
	}

	private static class OsEnumJavaTypeDescriptor<T extends Enum> extends EnumJavaTypeDescriptor<T> {
		private OsEnumJavaTypeDescriptor(Class<T> type) {
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