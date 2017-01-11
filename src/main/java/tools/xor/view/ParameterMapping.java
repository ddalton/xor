package tools.xor.view;

import tools.xor.util.ClassUtil;

import javax.persistence.ParameterMode;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.sql.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParameterMapping {

	@XmlAttribute
	String name;         // Optional if attribute is specified, required for non-view parameters
	
	@XmlAttribute
	String attribute;    // view parameter
	
	@XmlAttribute
	String type;         // java.sql.Types constant name, mandatory
	
	@XmlAttribute
	int scale;           // Required for NUMERIC/DECIMAL OUT parameters
	
	@XmlAttribute
	String defaultValue;
	
	@XmlAttribute
	ParameterMode mode = ParameterMode.IN;  // Default is IN
	
	@XmlTransient
	int position;

	static Map<Class, JavaConverter> convertersByJavaType = new ConcurrentHashMap<Class, JavaConverter>();
	static Map<Integer, SQLConverter> convertersBySQLType = new ConcurrentHashMap<Integer, SQLConverter>();
/*

	byte[]	VARBINARY or LONGVARBINARY
	java.sql.Date	DATE
	java.sql.Time	TIME
	java.sql.Timestamp	TIMESTAMP
*/

	static {
		convertersByJavaType.put(
			String.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int parameterIndex) throws
					SQLException
				{
					return rs.getString(parameterIndex);
				}
			});

		convertersByJavaType.put(
			BigDecimal.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int parameterIndex) throws
					SQLException
				{
					return rs.getBigDecimal(parameterIndex);
				}
			});

		convertersByJavaType.put(
			Boolean.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int parameterIndex) throws
					SQLException
				{
					return rs.getBoolean(parameterIndex);
				}
			});

		convertersByJavaType.put(
			Integer.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int parameterIndex) throws
					SQLException
				{
					return rs.getInt(parameterIndex);
				}
			});

		convertersByJavaType.put(
			Long.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int parameterIndex) throws
					SQLException
				{
					return rs.getLong(parameterIndex);
				}
			});

		convertersByJavaType.put(
			Float.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int parameterIndex) throws
					SQLException
				{
					return rs.getFloat(parameterIndex);
				}
			});

		convertersByJavaType.put(
			Double.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int parameterIndex) throws
					SQLException
				{
					return rs.getDouble(parameterIndex);
				}
			});

		convertersByJavaType.put(
			Byte[].class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int parameterIndex) throws
					SQLException
				{
					return rs.getBytes(parameterIndex);
				}
			});

		convertersByJavaType.put(
			Date.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int parameterIndex) throws
					SQLException
				{
					return rs.getDate(parameterIndex);
				}
			});

		convertersByJavaType.put(
			Time.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int parameterIndex) throws
					SQLException
				{
					return rs.getTime(parameterIndex);
				}
			});

		convertersByJavaType.put(
			Timestamp.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int parameterIndex) throws
					SQLException
				{
					return rs.getTimestamp(parameterIndex);
				}
			});
	}

	public interface JavaConverter {
		public Object byJavaType(ResultSet rs, int parameterIndex) throws
			SQLException;
	}

	public interface SQLConverter {

		// Sets the value on a CallableStatement
		public void javaToSQL(CallableStatement cs, int parameterIndex, Object value) throws
			SQLException;


		public Object sQLToJava(CallableStatement cs, int parameterIndex) throws
			SQLException;
	}

	static {
		convertersBySQLType.put(
			Types.ARRAY,

				new SQLConverter() {

					@Override public void javaToSQL (CallableStatement cs,
													 int parameterIndex,
													 Object value) throws SQLException
					{
						cs.setArray(parameterIndex, (Array)value);
					}

					@Override public Object sQLToJava (CallableStatement cs,
													   int parameterIndex) throws SQLException
					{
						return cs.getArray(parameterIndex);
					}
				}
			);

		convertersBySQLType.put(
			Types.BIGINT,

			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setLong(parameterIndex, (Long)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getLong(parameterIndex);
				}
			}
		);

		SQLConverter binaryConverter =
			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setBytes(parameterIndex, (byte[])value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getBytes(parameterIndex);
				}
			};
		convertersBySQLType.put(Types.BINARY, binaryConverter);
		convertersBySQLType.put(Types.VARBINARY, binaryConverter);
		convertersBySQLType.put(Types.LONGVARBINARY, binaryConverter);

		SQLConverter booleanConverter =
			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setBoolean(parameterIndex, (Boolean)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getBoolean(parameterIndex);
				}
			};

		convertersBySQLType.put(Types.BIT, booleanConverter);
		convertersBySQLType.put(Types.BOOLEAN, booleanConverter);

		convertersBySQLType.put(
			Types.BLOB,

			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setBlob(parameterIndex, (Blob)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getBlob(parameterIndex);
				}
			}
		);

		SQLConverter stringConverter =
			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setString(parameterIndex, (String)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getString(parameterIndex);
				}
			};
		convertersBySQLType.put(Types.CHAR, stringConverter);
		convertersBySQLType.put(Types.VARCHAR, stringConverter);
		convertersBySQLType.put(Types.LONGVARCHAR, stringConverter);

		SQLConverter nstringConverter =
			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setNString(parameterIndex, (String)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getNString(parameterIndex);
				}
			};
		convertersBySQLType.put(Types.NCHAR, nstringConverter);
		convertersBySQLType.put(Types.NVARCHAR, nstringConverter);
		convertersBySQLType.put(Types.LONGNVARCHAR, nstringConverter);

		convertersBySQLType.put(
			Types.CLOB,

			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setClob(parameterIndex, (Clob)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getClob(parameterIndex);
				}
			}
		);

		convertersBySQLType.put(
			Types.DATE,

			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setDate(parameterIndex, (Date)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getDate(parameterIndex);
				}
			}
		);

		SQLConverter bigdecimalConverter =
			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setBigDecimal(parameterIndex, (BigDecimal)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getBigDecimal(parameterIndex);
				}
			};
		convertersBySQLType.put(Types.DECIMAL, bigdecimalConverter);
		convertersBySQLType.put(Types.NUMERIC, bigdecimalConverter);

		convertersBySQLType.put(
			Types.DOUBLE,

			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setDouble(parameterIndex, (Double)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getDouble(parameterIndex);
				}
			}
		);

		SQLConverter floatConverter =
			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setFloat(parameterIndex, (Float)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getFloat(parameterIndex);
				}
			};
		convertersBySQLType.put(Types.FLOAT, floatConverter);
		convertersBySQLType.put(Types.REAL, floatConverter);

		SQLConverter integerConverter =
			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setInt(parameterIndex, (Integer)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getInt(parameterIndex);
				}
			};
		convertersBySQLType.put(Types.INTEGER, integerConverter);
		convertersBySQLType.put(Types.SMALLINT, integerConverter);

		convertersBySQLType.put(
			Types.NCLOB,

			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setNClob(parameterIndex, (NClob)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getNClob(parameterIndex);
				}
			}
		);

		convertersBySQLType.put(
			Types.TIME,

			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setTime(parameterIndex, (Time)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getTime(parameterIndex);
				}
			}
		);

		convertersBySQLType.put(
			Types.TIMESTAMP,

			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setTimestamp(parameterIndex, (Timestamp)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getTimestamp(parameterIndex);
				}
			}
		);

		convertersBySQLType.put(
			Types.TINYINT,

			new SQLConverter() {

				@Override public void javaToSQL (CallableStatement cs,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					cs.setByte(parameterIndex, (Byte)value);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getByte(parameterIndex);
				}
			}
		);
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void setMode(ParameterMode mode) {
		this.mode = mode;
	}

	public void setValue(CallableStatement cs, Object value) {
		SQLConverter converter = convertersBySQLType.get(type);
		try {
			converter.javaToSQL(cs, position, value);
		}
		catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	static public Object getValue(Class clazz, ResultSet rs, int parameterIndex) {
		try {
			return convertersByJavaType.get(clazz).byJavaType(rs, parameterIndex);
		}
		catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}
	}
}
