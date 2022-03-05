package tools.xor.view;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.ParameterMode;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import tools.xor.JSONObjectProperty;
import tools.xor.providers.jdbc.DBTranslator;
import tools.xor.util.ClassUtil;

@XmlAccessorType(XmlAccessType.FIELD)
public class BindParameter implements Comparable<BindParameter>
{

	@XmlAttribute
	public String name;         // Optional if attribute is specified, required for non-view parameters
	
	@XmlAttribute
	public String attribute;    // view parameter
	
	@XmlAttribute
	public String type;         // java.sql.Types constant name, mandatory
	
	@XmlAttribute
	int scale;           // Required for NUMERIC/DECIMAL OUT parameters
	
	@XmlAttribute
	String defaultValue;
	
	@XmlAttribute
	ParameterMode mode = ParameterMode.IN;  // Default is IN

	@XmlAttribute
	boolean returnType;
	
	@XmlAttribute
	public Integer position;

	@XmlAttribute
	public String dateFormat;

	@XmlTransient
	private Object defaultValueObject;

	static final Map<Class, JavaConverter> convertersByJavaType = new ConcurrentHashMap<>();
	static final Map<Integer, SQLConverter> convertersBySQLType = new ConcurrentHashMap<>();
	static final Map<String, Integer> typeMap = new HashMap<>();
	static final String DATE_DELIM = ",";

	static final JavaConverter defaultJavaConverter = new JavaConverter()
	{
		@Override public Object byJavaType (ResultSet rs, int columnIndex) throws SQLException
		{
			return rs.getObject(columnIndex);
		}
	};

	/**
	 * Create an instance of the BindParameter
	 * @param position is a required field
	 * @param name optional
	 * @return new BindParameter instance
	 */
	public static BindParameter instance(Integer position, String name) {
		BindParameter bp = new BindParameter();

		bp.position = position;
		bp.name = name;

		return bp;
	}

	public BindParameter copy() {
		BindParameter result = new BindParameter();
		result.name = name;
		result.attribute = attribute;
		result.type = type;
		result.scale = scale;
		result.defaultValue = defaultValue;
		result.mode = mode;
		result.returnType = returnType;
		result.position = position;
		result.dateFormat = dateFormat;

		return result;
	}

/*

	byte[]	VARBINARY or LONGVARBINARY
	java.sql.Date	DATE
	java.sql.Time	TIME
	java.sql.Timestamp	TIMESTAMP
*/

	static {
		typeMap.put("ARRAY",Types.ARRAY);
		typeMap.put("BIGINT",Types.BIGINT);
		typeMap.put("BINARY",Types.BINARY);
		typeMap.put("VARBINARY",Types.VARBINARY);
		typeMap.put("LONGVARBINARY",Types.LONGVARBINARY);
		typeMap.put("BIT",Types.BIT);
		typeMap.put("BOOLEAN",Types.BOOLEAN);
		typeMap.put("BLOB",Types.BLOB);
		typeMap.put("CHAR",Types.CHAR);
		typeMap.put("VARCHAR",Types.VARCHAR);
		typeMap.put("LONGVARCHAR",Types.LONGVARCHAR);
		typeMap.put("NCHAR",Types.NCHAR);
		typeMap.put("NVARCHAR",Types.NVARCHAR);
		typeMap.put("LONGNVARCHAR",Types.LONGNVARCHAR);
		typeMap.put("CLOB",Types.CLOB);
		typeMap.put("DATE",Types.DATE);
		typeMap.put("DECIMAL",Types.DECIMAL);
		typeMap.put("NUMERIC",Types.NUMERIC);
		typeMap.put("DOUBLE",Types.DOUBLE);
		typeMap.put("FLOAT",Types.FLOAT);
		typeMap.put("REAL",Types.REAL);
		typeMap.put("INTEGER",Types.INTEGER);
		typeMap.put("SMALLINT",Types.SMALLINT);
		typeMap.put("NCLOB",Types.NCLOB);
		typeMap.put("TIME",Types.TIME);
		typeMap.put("TIMESTAMP",Types.TIMESTAMP);
		typeMap.put("TINYINT",Types.TINYINT);

		convertersByJavaType.put(
			String.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int columnIndex) throws
					SQLException
				{
					return rs.getString(columnIndex);
				}
			});

		convertersByJavaType.put(
			BigDecimal.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int columnIndex) throws
					SQLException
				{
					return rs.getBigDecimal(columnIndex);
				}
			});

		convertersByJavaType.put(
			Boolean.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int columnIndex) throws
					SQLException
				{
					return rs.getBoolean(columnIndex);
				}
			});

		convertersByJavaType.put(
			Integer.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int columnIndex) throws
					SQLException
				{
					return rs.getInt(columnIndex);
				}
			});

		convertersByJavaType.put(
			Long.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int columnIndex) throws
					SQLException
				{
					return rs.getLong(columnIndex);
				}
			});

		convertersByJavaType.put(
			Float.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int columnIndex) throws
					SQLException
				{
					return rs.getFloat(columnIndex);
				}
			});

		convertersByJavaType.put(
			Double.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int columnIndex) throws
					SQLException
				{
					return rs.getDouble(columnIndex);
				}
			});

		convertersByJavaType.put(
			Byte[].class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int columnIndex) throws
					SQLException
				{
					return rs.getBytes(columnIndex);
				}
			});

		convertersByJavaType.put(
			Date.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int columnIndex) throws
					SQLException
				{
					return rs.getDate(columnIndex);
				}
			});

		convertersByJavaType.put(
			Time.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int columnIndex) throws
					SQLException
				{
					return rs.getTime(columnIndex);
				}
			});

		convertersByJavaType.put(
			Timestamp.class,
			new JavaConverter()
			{
				@Override public Object byJavaType (ResultSet rs, int columnIndex) throws
					SQLException
				{
					return rs.getTimestamp(columnIndex);
				}
			});
	}

	private static Date getSqlDate(String value) {
		if(value.indexOf(DATE_DELIM) == -1) {
			throw new RuntimeException("Format expected and should be of the form: <format>,<value>");
		}
		String format = value.substring(0, value.indexOf(DATE_DELIM));
		SimpleDateFormat formatter = new SimpleDateFormat(format);

		try {
			java.util.Date date = formatter.parse(value.substring(value.indexOf(DATE_DELIM) + DATE_DELIM.length()));
			return new Date(date.getTime());
		}
		catch (ParseException e) {
			throw ClassUtil.wrapRun(e);
		}
	}

	@Override public int compareTo (BindParameter o)
	{
		return this.position - o.position;
	}

	public interface JavaConverter {
		public Object byJavaType(ResultSet rs, int columnIndex) throws
			SQLException;

		default public void javaToSQL(PreparedStatement ps, int parameterIndex, Object value) throws
			SQLException
		{
			ps.setObject(parameterIndex, value);
		}
	}

	public interface SQLConverter {

		// Sets the value on a PreparedStatement
		public void javaToSQL(PreparedStatement ps, int parameterIndex, Object value) throws
			SQLException;

		public Object sQLToJava(CallableStatement cs, int parameterIndex) throws
			SQLException;

		public Object sQLToJava(ResultSet rs, int parameterIndex) throws
			SQLException;

		default public void setDataContext(Object value) {
			// Users can set any custom data needed by the converter
		}

		default public Object stringToSQLType(String value) {
			return value;
		}
	}

	static {
		convertersBySQLType.put(
			Types.ARRAY,

				new SQLConverter() {

					@Override public void javaToSQL (PreparedStatement ps,
													 int parameterIndex,
													 Object value) throws SQLException
					{
						if(value instanceof Array) {
							ps.setArray(parameterIndex, (Array)value);
						} else {
							ps.setObject(parameterIndex, value);
						}
					}

					@Override public Object sQLToJava (CallableStatement cs,
													   int parameterIndex) throws SQLException
					{
						return cs.getArray(parameterIndex);
					}

					@Override public Object sQLToJava (ResultSet rs,
													   int parameterIndex) throws SQLException
					{
						return rs.getArray(parameterIndex);
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
					Long bigInt = null;
					if(value instanceof String) {
						bigInt = Long.valueOf(value.toString());
					} else if (value instanceof Long) {
						bigInt = (Long)value;
					} else if(value instanceof Number) {
						bigInt = ((Number)value).longValue();
					} else {
						throw new RuntimeException("Unsupported value type for BIGINT converter");
					}
					ps.setLong(parameterIndex, bigInt);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getLong(parameterIndex);
				}

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getLong(parameterIndex);
				}

				public Object stringToSQLType(String value) {
					return Long.valueOf(value);
				}
			}
		);

		SQLConverter binaryConverter =
			new SQLConverter() {

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					byte[] bytes = null;
					if(value instanceof String) {
						bytes = value.toString().getBytes();
					} else if(value instanceof byte[]) {
						bytes = (byte[]) value;
					} else {
						throw new RuntimeException("Unsupported value type for binary converter");
					}
					ps.setBytes(parameterIndex, bytes);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getBytes(parameterIndex);
				}

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getBytes(parameterIndex);
				}

				public Object stringToSQLType(String value) {
					return value.getBytes();
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
					Boolean bool = null;
					if(value instanceof String) {
						bool = Boolean.valueOf(value.toString());
					} else if(value instanceof Boolean) {
						bool = (Boolean) value;
					} else {
						throw new RuntimeException("Unsupported value type for boolean converter");
					}
					ps.setBoolean(parameterIndex, bool);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getBoolean(parameterIndex);
				}

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getBoolean(parameterIndex);
				}

				public Object stringToSQLType(String value) {
					return Boolean.parseBoolean(value);
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

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getBlob(parameterIndex);
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

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getString(parameterIndex);
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

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getNString(parameterIndex);
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

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getClob(parameterIndex);
				}
			}
		);

		SQLConverter dateConverter =
			new SQLConverter() {

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					Date date = null;
					if(value instanceof String) {
						DateFormat df = new SimpleDateFormat(JSONObjectProperty.ISO8601_FORMAT_DATE);
						try {
							date = new Date(df.parse(value.toString()).getTime());
						}
						catch (ParseException e) {
							throw new RuntimeException("Unable to parse date value: " + value + ", the desired format is: " + JSONObjectProperty.ISO8601_FORMAT_DATE);
						}
					} else if(value instanceof Date || value instanceof java.util.Date) {
						date = (Date) value;
					} else {
						throw new RuntimeException("Unsupported value type for Date converter");
					}
					ps.setDate(parameterIndex, date);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getDate(parameterIndex);
				}

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getDate(parameterIndex);
				}

				/**
				 * The value should contain the date format in the form <format>,<value>
				 * @param value contains the date to be parsed
				 * @return date object
				 */
				@Override public Object stringToSQLType(String value) {
					return getSqlDate(value);
				}
			};
        convertersBySQLType.put(Types.DATE, dateConverter);
        convertersBySQLType.put(Types.TIMESTAMP, dateConverter);

		SQLConverter bigdecimalConverter =
			new SQLConverter() {

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					BigDecimal result = null;
					if(value instanceof String) {
						result = new BigDecimal(value.toString());
					} else if(value instanceof BigDecimal) {
						result = (BigDecimal)value;
					} else if(value instanceof Number) {
						result = new BigDecimal(value.toString());
					} else {
						throw new RuntimeException("Unsupported value type for BigDecimal converter");
					}
					ps.setBigDecimal(parameterIndex, result);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getBigDecimal(parameterIndex);
				}

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getBigDecimal(parameterIndex);
				}


				@Override public Object stringToSQLType(String value) {
					return new BigDecimal(value);
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
					Double result = null;
					if(value instanceof String) {
						result = Double.valueOf(value.toString());
					} else if(value instanceof Double) {
						result = (Double)value;
					} else if(value instanceof Number) {
						result = ((Number)value).doubleValue();
					} else {
						throw new RuntimeException("Unsupported value type for Double converter");
					}
					ps.setDouble(parameterIndex, result);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getDouble(parameterIndex);
				}

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getDouble(parameterIndex);
				}

				@Override public Object stringToSQLType(String value) {
					return Double.parseDouble(value);
				}
			}
		);

		SQLConverter floatConverter =
			new SQLConverter() {

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					Float result = null;
					if(value instanceof String) {
						result = Float.valueOf(value.toString());
					} else if(value instanceof Float) {
						result = (Float)value;
					} else if(value instanceof Number) {
						result = ((Number)value).floatValue();
					} else {
						throw new RuntimeException("Unsupported value type for Float converter");
					}
					ps.setFloat(parameterIndex, result);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getFloat(parameterIndex);
				}

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getFloat(parameterIndex);
				}

				@Override public Object stringToSQLType(String value) {
					return Float.parseFloat(value);
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
					Integer result = null;
					if(value instanceof String) {
						result = Integer.valueOf(value.toString());
					} else if(value instanceof Integer) {
						result = (Integer)value;
					} else if(value instanceof Number) {
						result = ((Number)value).intValue();
					} else {
						throw new RuntimeException("Unsupported value type for Integer converter");
					}
					ps.setInt(parameterIndex, result);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getInt(parameterIndex);
				}

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getInt(parameterIndex);
				}

				@Override public Object stringToSQLType(String value) {
					return Integer.parseInt(value);
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

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getNClob(parameterIndex);
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
					Time time = null;
					if(value instanceof String) {
						time = Time.valueOf(value.toString());
					} else if(value instanceof Time) {
						time = (Time) value;
					} else if(value instanceof java.util.Date) {
						time = new Time(((java.util.Date)value).getTime());
					} else {
						throw new RuntimeException("Unsupported value type for Time converter");
					}
					ps.setTime(parameterIndex, time);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getTime(parameterIndex);
				}

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getTime(parameterIndex);
				}

				@Override public Object stringToSQLType(String value) {
					return getSqlDate(value);
				}
			}
		);

		convertersBySQLType.put(
			Types.TIMESTAMP,

			new SQLConverter() {

				private String dateFormat;

				@Override public void setDataContext(Object value) {
					this.dateFormat = (String) value;
				}

				@Override public void javaToSQL (PreparedStatement ps,
												 int parameterIndex,
												 Object value) throws SQLException
				{
					Timestamp timestamp = null;
					if(value != null) {
					    if(value instanceof String) {
					        DateFormat df = new SimpleDateFormat(this.dateFormat != null ? this.dateFormat : JSONObjectProperty.ISO8601_FORMAT);
					        try {
					            timestamp = new Timestamp(df.parse(value.toString()).getTime());
					        }
					        catch (ParseException e) {
					            throw new RuntimeException("Unable to parse date value: " + value + ", the desired format is: " + JSONObjectProperty.ISO8601_FORMAT);
					        }
					    } else if(value instanceof Timestamp) {
					        timestamp = (Timestamp)value;
                        } else if(value instanceof Date) {
                            timestamp = new Timestamp(((Date) value).getTime());					        
					    } else if(value instanceof java.util.Date) {
					        timestamp = new Timestamp(((java.util.Date)value).getTime());
					    } else {
					        throw new RuntimeException("Unsupported value type for Timestamp converter: " + value.getClass().getName());
					    }
					}
					ps.setTimestamp(parameterIndex, timestamp);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getTimestamp(parameterIndex);
				}

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getTimestamp(parameterIndex);
				}

				@Override public Object stringToSQLType(String value) {
					return getSqlDate(value);
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
					Byte result = null;
					if(value instanceof String) {
						result = Byte.valueOf(value.toString());
					} else if(value instanceof Byte) {
						result = (Byte) value;
					} else if(value instanceof Number) {
						result = ((Number)value).byteValue();
					} else {
						throw new RuntimeException("Unsupported value type for TINYINT converter");
					}
					ps.setByte(parameterIndex, result);
				}

				@Override public Object sQLToJava (CallableStatement cs,
												   int parameterIndex) throws SQLException
				{
					return cs.getByte(parameterIndex);
				}

				@Override public Object sQLToJava (ResultSet rs,
												   int parameterIndex) throws SQLException
				{
					return rs.getByte(parameterIndex);
				}

				@Override public Object stringToSQLType(String value) {
					return Byte.valueOf(value);
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

	public void setDateFormat(String format) {
		this.dateFormat = format;
	}

	public boolean isReturnType() {
		return this.returnType;
	}

	public void setReturnType (boolean value)
	{
		this.returnType = value;
	}

	public Object getDefaultValue() {
		if(type == null) {
			throw new RuntimeException("type is required to get the default value");
		}

		if(defaultValueObject == null && defaultValue != null) {
			defaultValueObject = convertersBySQLType.get(getType(type)).stringToSQLType(defaultValue);
		}

		return defaultValueObject;
	}

	public static int getType(String type) {
		if(typeMap.containsKey(type)) {
			return typeMap.get(type);
		}

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

		return typeValue;
	}
	
	public Integer getPosition() {
	    return this.position;
	}

	private static SQLConverter getSQLConverter(DBTranslator translator, int typeValue) {
		SQLConverter result = translator.getSQLConverter(typeValue);

		if(result == null) {
			result = convertersBySQLType.get(typeValue);
		}

		return result;
	}

	public void setValue(PreparedStatement ps, DBTranslator translator, Object value) {
		if(type != null && !"".equals(type)) {
			int typeValue = getType(type);

			SQLConverter converter = getSQLConverter(translator, typeValue);
			try {
				if (this.dateFormat != null) {
					converter.setDataContext(this.dateFormat);
				}
				converter.javaToSQL(ps, position, value);
			}
			catch (SQLException e) {
				throw ClassUtil.wrapRun(e);
			}
		} else {
			try {
				defaultJavaConverter.javaToSQL(ps, position, value);
			}
			catch (SQLException e) {
				throw ClassUtil.wrapRun(e);
			}
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

	static public Object getValue(DBTranslator translator, int type, ResultSet rs, int parameterIndex) {
		try {
			return getSQLConverter(translator, type).sQLToJava(rs, parameterIndex);
		}
		catch (SQLException e) {
			throw ClassUtil.wrapRun(e);
		}
	}
}
