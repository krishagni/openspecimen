package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

/**
 * Compatibility EnumType for legacy HBM mappings that reference
 * "org.hibernate.type.EnumType" with parameters:
 *  - enumClass: FQCN of the enum
 *  - type: SQL type code (e.g., 12 for VARCHAR)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EnumType implements UserType<Enum>, ParameterizedType {
	private Class<? extends Enum> enumClass;
	private int sqlType = Types.VARCHAR;
	private boolean useOrdinal = false;
	private EnumJavaType<?> enumJavaType;

	@Override
	public void setParameterValues(Properties parameters) {
		if (parameters == null) {
			return;
		}

		String enumClassName = parameters.getProperty("enumClass");
		if (enumClassName != null) {
			try {
				enumClass = (Class<? extends Enum>) Class.forName(enumClassName);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException("Enum class not found: " + enumClassName, e);
			}
		}

		String typeParam = parameters.getProperty("type");
		if (typeParam != null) {
			try {
				sqlType = Integer.parseInt(typeParam);
			} catch (NumberFormatException e) {
				sqlType = Types.VARCHAR;
			}
		}

		useOrdinal = (sqlType == Types.INTEGER || sqlType == Types.SMALLINT || sqlType == Types.TINYINT);
	}

	public void setEnumJavaType(EnumJavaType<?> enumJavaType) {
		this.enumJavaType = enumJavaType;
	}

	@Override
	public int getSqlType() {
		return sqlType;
	}

	@Override
	public Class<Enum> returnedClass() {
		return (Class<Enum>) enumClass;
	}

	@Override
	public Enum nullSafeGet(ResultSet rs, int position, WrapperOptions options)
	throws SQLException {
		Object value = rs.getObject(position);
		if (value == null || enumClass == null) {
			return null;
		}

		if (enumJavaType != null) {
			try {
				if (useOrdinal) {
					int ordinal = (value instanceof Number) ? ((Number) value).intValue() : Integer.parseInt(value.toString());
					return enumJavaType.fromOrdinal(ordinal);
				} else {
					return enumJavaType.fromName(value.toString());
				}
			} catch (Exception e) {
				// Fall back to legacy conversion below
			}
		}

		if (useOrdinal) {
			int ordinal = (value instanceof Number) ? ((Number) value).intValue() : Integer.parseInt(value.toString());
			Enum[] constants = enumClass.getEnumConstants();
			return (ordinal >= 0 && ordinal < constants.length) ? constants[ordinal] : null;
		} else {
			String name = value.toString();
			if (name.isEmpty()) {
				return null;
			}
			return Enum.valueOf((Class) enumClass, name);
		}
	}

	@Override
	public void nullSafeSet(PreparedStatement st, Enum value, int index, WrapperOptions options)
	throws SQLException {
		if (value == null) {
			st.setNull(index, sqlType);
			return;
		}

		if (enumJavaType != null) {
			if (useOrdinal) {
				st.setInt(index, value.ordinal());
			} else {
				st.setString(index, value.name());
			}
			return;
		}

		if (useOrdinal) {
			st.setInt(index, value.ordinal());
		} else {
			st.setString(index, value.name());
		}
	}

	@Override
	public Enum deepCopy(Enum value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(Enum value) {
		return value == null ? null : value.name();
	}

	@Override
	public Enum assemble(Serializable cached, Object owner) {
		if (cached == null || enumClass == null) {
			return null;
		}
		return Enum.valueOf((Class) enumClass, cached.toString());
	}
}
