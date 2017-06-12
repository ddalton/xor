package tools.xor.view;

import jdk.nashorn.internal.codegen.CompilerConstants;
import tools.xor.util.ClassUtil;

import javax.persistence.ParameterMode;
import javax.xml.bind.annotation.XmlAttribute;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.PreparedStatement;
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
	public String name;         // Optional if attribute is specified, required for non-view parameters
	
	@XmlAttribute
	public String attribute;    // view parameter
	
	@XmlAttribute
	String type;         // java.sql.Types constant name, mandatory
	
	@XmlAttribute
	int scale;           // Required for NUMERIC/DECIMAL OUT parameters
	
	@XmlAttribute
	String defaultValue;
	
	@XmlAttribute
	ParameterMode mode = ParameterMode.IN;  // Default is IN

	boolean returnType;
	
	@XmlAttribute
	public int position;

	static final Map<Class, JavaConverter> convertersByJavaType = new ConcurrentHashMap<>();
	static final Map<Integer, SQLConverter> convertersBySQLType = new ConcurrentHashMap<>();
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

		// Sets the value on a PreparedStatement
		public void javaToSQL(PreparedStatement ps, int parameterIndex, Object value) throws
			SQLException;


		public Object sQLToJava(CallableStatement cs, int parameterIndex) throws
			SQLException;
	}

	static {
		convertersBySQLType.put(
			Types.ARRAY,

				new SQLConverter() {

					@Override public void javaToSQL (PreparedStatement ps,
													 int parameterIndex,
													 Object value) throws SQLException
					{
						ps.setArray(parameterIndex, (Array)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setLong(parameterIndex, (Long)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setBytes(parameterIndex, (byte[])value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setBoolean(parameterIndex, (Boolean)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setBlob(parameterIndex, (Blob)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setString(parameterIndex, (String)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setNString(parameterIndex, (String)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setClob(parameterIndex, (Clob)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setDate(parameterIndex, (Date)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setBigDecimal(parameterIndex, (BigDecimal)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setDouble(parameterIndex, (Double)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setFloat(parameterIndex, (Float)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setInt(parameterIndex, (Integer)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setNClob(parameterIndex, (NClob)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setTime(parameterIndex, (Time)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setTimestamp(parameterIndex, (Timestamp)value);
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

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					ps.setByte(parameterIndex, (Byte)value);
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

	@XmlAttribute
	public boolean isReturnType() {
		return this.returnType;
	}

	public void setReturnType (boolean value)
	{
		this.returnType = value;
	}

	public void setValue(PreparedStatement ps, Object value) {
		int typeValue = 0;
		try {
			typeValue = Integer.parseInt(type);
		}
		catch (NumberFormatException e) {
			// It is probably using the type name, so let us try to get it using reflection
			try {
				Field f = Types.class.getField(type);
				typeValue = f.getInt(null);
			}
			catch (Exception e1) {
				throw ClassUtil.wrapRun(e1);
			}
		}
		SQLConverter converter = convertersBySQLType.get(typeValue);
		try {
			converter.javaToSQL(ps, position, value);
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
