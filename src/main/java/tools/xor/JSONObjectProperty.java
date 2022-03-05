package tools.xor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tools.xor.util.ClassUtil;

public class JSONObjectProperty
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    private static Map<Class, Converter> convertersByClass = new ConcurrentHashMap<Class, Converter>();
    public static final String ISO8601_FORMAT_DATE = "yyyy-MM-dd";
    public static final String ISO8601_FORMAT_TIME = "HH:mm:ss";
    public static final String ISO8601_FORMAT = ISO8601_FORMAT_DATE + "'T'" + ISO8601_FORMAT_TIME + ".SSSZ";
    public static final String ANSI_FORMAT_DATETIME = ISO8601_FORMAT_DATE + " " + ISO8601_FORMAT_TIME;

    private volatile Converter converter;   // For performance optimization
    private final ExtendedProperty property;

    public interface Converter {
        public void setExternal(Settings settings, JSONObject jsonObject, String name, Object object) throws JSONException;
        public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException;

        /**
         * We have to use an array builder since there is no "name" property
         *
         * @param settings under which this operation is performed
         * @param jsonArray object
         * @param object to add to the array
         */
        public void   add(Settings settings, JSONArray jsonArray, Object object);
    }

    /**
     * Find the built-in converters based on the provided class
     * New converters can be registered to the Shape containing the type to 
     * override the converter behavior.
     * 
     * @param clazz for which the built-in converter needs to be found
     * @return built-in converter
     */
    public static Converter findConverter(Class<?> clazz) {
        if(convertersByClass.containsKey(clazz)) {
            return convertersByClass.get(clazz);
        }

        return null;
    }

    abstract public static class AbstractConverter implements Converter {
        @Override
        public void setExternal(Settings settings, JSONObject jsonObject, String name, Object object) throws JSONException {
            jsonObject.put(name, object);
        }
    }


    private static Object getBlobString(Object object) {

        if(object instanceof Blob) {
            Blob blob = (Blob) object;
            try {
                return Base64.getEncoder().encodeToString(blob.getBytes(1,
                        (int)blob.length()));
            }
            catch (SQLException e) {
                throw ClassUtil.wrapRun(e);
            }
        } else if(object instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[])object);
        }
        else {
            return object;
        }
    }

    static {
        convertersByClass.put(Blob.class,
            new AbstractConverter() {

                @Override
                public void setExternal(Settings settings, JSONObject jsonObject, String name, Object object) throws JSONException {
                    jsonObject.put(name, getBlobString(object));
                }

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {

                    try {
                        Blob result = settings.getDataStore().createBlob();
                        result.setBytes(1, Base64.getDecoder().decode(jsonObject.getString(key)));
                        return result;
                    }
                    catch (Exception e) {
                        throw ClassUtil.wrapRun(e);
                    }
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    jsonArray.put(getBlobString(object));
                }
            }
        );

        convertersByClass.put(Boolean.class,
            new AbstractConverter() {

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {

                    return jsonObject.getBoolean(key);
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    jsonArray.put((Boolean) object);
                }
            }
        );
        convertersByClass.put(boolean.class, convertersByClass.get(Boolean.class)); // primitive

        convertersByClass.put(BigDecimal.class,
            new AbstractConverter() {

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                    if(jsonObject.has(key)) {
                        Object value = jsonObject.get(key);
                        if(value instanceof BigDecimal) {
                            return value;
                        } else if(value instanceof Number) {
                            return new BigDecimal(value.toString());
                        } else
                            return new BigDecimal(jsonObject.getString(key));
                    }
                    return null;
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    jsonArray.put((BigDecimal) object);
                }
            }
        );

        convertersByClass.put(BigInteger.class,
            new AbstractConverter() {

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                    if(jsonObject.has(key)) {
                        Object value = jsonObject.get(key);
                        if(value instanceof BigInteger)
                            return value;
                        else
                            return new BigInteger(jsonObject.getString(key));
                    }
                    return null;
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    jsonArray.put((BigInteger) object);
                }
            }
        );

        convertersByClass.put(Double.class,
            new AbstractConverter() {

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                    return jsonObject.getDouble(key);
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    jsonArray.put((Double) object);
                }
            }
        );
        convertersByClass.put(double.class, convertersByClass.get(Double.class)); // primitive

        convertersByClass.put(Float.class,
            new AbstractConverter() {

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                    return (float) jsonObject.getDouble(key);
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    jsonArray.put((Float) object);
                }
            }
        );
        convertersByClass.put(float.class, convertersByClass.get(Float.class)); // primitive

        convertersByClass.put(Integer.class,
            new AbstractConverter() {

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                    return jsonObject.getInt(key);
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    jsonArray.put((Integer) object);
                }
            }
        );
        convertersByClass.put(int.class, convertersByClass.get(Integer.class)); // primitive
        
        
        convertersByClass.put(Byte.class,
                new AbstractConverter() {

                    @Override
                    public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                        return new Integer(jsonObject.getInt(key)).byteValue();
                    }

                    @Override
                    public void add(Settings settings, JSONArray jsonArray, Object object) {
                        jsonArray.put((Byte) object);
                    }
                }
            );        
        convertersByClass.put(byte.class, convertersByClass.get(Integer.class));
        
        convertersByClass.put(Short.class,
                new AbstractConverter() {

                    @Override
                    public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                        return new Integer(jsonObject.getInt(key)).shortValue();
                    }

                    @Override
                    public void add(Settings settings, JSONArray jsonArray, Object object) {
                        jsonArray.put((Short) object);
                    }
                }
            );        
        convertersByClass.put(short.class, convertersByClass.get(Integer.class));

        convertersByClass.put(Long.class,
            new AbstractConverter() {

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                    return jsonObject.getLong(key);
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    jsonArray.put((Long) object);
                }
            }
        );
        convertersByClass.put(long.class, convertersByClass.get(Long.class)); // primitive

        convertersByClass.put(String.class,
            new AbstractConverter() {

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                    return jsonObject.getString(key);
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    jsonArray.put((String) object);
                }
            }
        );

        convertersByClass.put(Date.class,
            new AbstractConverter() {

                @Override
                public void setExternal(Settings settings, JSONObject jsonObject, String name, Object object) throws JSONException {
                    if(settings.getDateForm() == Settings.DateForm.FORMATTED) {
                        DateFormat df = new SimpleDateFormat(settings.getDateFormat());
                        jsonObject.put(name, object == null ? null : df.format(object));
                    } else {
                        jsonObject.put(name, object == null ? null : ((Date)object).getTime());
                    }
                }

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                    Object date = jsonObject.get(key);
                    if(date instanceof Date) {
                        return date;
                    }
                    
                    if(date == null || "".equals(date.toString().trim())) {
                        return null;
                    }
                    
                    if(settings.getDateForm() == Settings.DateForm.FORMATTED) {
                        DateFormat df = new SimpleDateFormat(settings.getDateFormat());
                        String dateString = jsonObject.getString(key);
                        try {
                            return df.parse(dateString);
                        }
                        catch (ParseException e) {
                            logger.warn(
                                "Problem parsing date string: "
                                    + dateString + ", message: " + e.getMessage());
                            return null;
                        }
                    } else {
                        String dateString = jsonObject.getString(key);
                        Long timeInMillis = Long.parseLong(dateString);
                        return new Date(timeInMillis);
                    }
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    if(settings.getDateForm() == Settings.DateForm.FORMATTED) {
                        DateFormat df = new SimpleDateFormat(settings.getDateFormat());
                        jsonArray.put(object == null ? null : df.format(object));
                    } else {
                        jsonArray.put(object == null ? null : ((Date)object).getTime());
                    }
                }
            }
        );
        convertersByClass.put(java.sql.Date.class, convertersByClass.get(Date.class));
        convertersByClass.put(java.sql.Timestamp.class, convertersByClass.get(Date.class));
        convertersByClass.put(java.sql.Time.class, convertersByClass.get(Date.class));

        /**
         * Invoking build() on a JsonObjectBuilder, retains only this property and clears out
         * all the other properties. So build() should be invoked only once at the end of
         * populating all the fields.
         */
        convertersByClass.put(JSONObject.class,
            new AbstractConverter() {

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                    // We cannot handle it here since we do not know the type
                    return jsonObject.get(key);
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    jsonArray.put(object == null ? null : (JSONObject)object);
                }
            }
        );

        convertersByClass.put(JSONArray.class,
            new AbstractConverter() {

                @Override
                public Object toDomain(Settings settings, JSONObject jsonObject, Property property, String key) throws JSONException {
                    // We cannot handle it here since we do not know the type
                    return jsonObject.get(key);
                }

                @Override
                public void add(Settings settings, JSONArray jsonArray, Object object) {
                    jsonArray.put(object == null ? null : (JSONArray)object);
                }
            }
        );
    }

    public JSONObjectProperty(ExtendedProperty property) {
        this.property = property;
    }

    private String getName() {
        return this.property.getName();
    }

    private Type getType() {
        return property.getType();
    }

    public Class<?> getJavaType() {
        return this.property.getType().getInstanceClass();
    }

    public String getStringValue(BusinessObject dataObject)
    {
        Object instance = ClassUtil.getInstance(dataObject);
        if (JSONObject.class.isAssignableFrom(instance.getClass())) {
            JSONObject json = (JSONObject)instance;

            try {
                Object value = json.get(getName());
                if(value instanceof String) {
                    return json.getString(getName());
                } else {
                    return value == null ? null : value.toString();
                }
            } catch (Exception e) {
                return null;
            }
        }

        return getValue(dataObject).toString();
    }

    public Object query(Object dataObject) {
        Object instance = ClassUtil.getInstance(dataObject);
        if(JSONObject.class.isAssignableFrom(instance.getClass())) {
            JSONObject json = (JSONObject) instance;
            try {
                return json.get(getName());
            } catch (JSONException e) {
                // This property was not found
                return null;
            }
        } else {
            // This is at INFO level, because on read we try to read from the JsonObjectBuilder which is not allowed
            logger.info("DataObject instance is not a JsonObject " + instance.getClass().getName());
            return null;
        }
    }

    public Object getValue(BusinessObject dataObject)
    {
        Object instance = ClassUtil.getInstance(dataObject);
        if(JSONObject.class.isAssignableFrom(instance.getClass())) {
            JSONObject json = (JSONObject) instance;
            try {
                Object value = toDomain(dataObject.getSettings(), json, getName());
                return value;
            } catch (JSONException e) {
                // This property was not found
                return null;
            }
        } else {
            // This is at INFO level, because on read we try to read from the JsonObjectBuilder which is not allowed
            logger.info("DataObject instance is not a JsonObject " + instance.getClass().getName());
            return null;
        }

    }

    public void setValue(Settings settings, Object dataObject, Object propertyValue)
    {
        Object instance = ClassUtil.getInstance(dataObject);
        if(JSONObject.class.isAssignableFrom(instance.getClass())) {
            JSONObject jsonObject = (JSONObject) instance;
            try {
                setExternal(settings, jsonObject, getName(), propertyValue);
            } catch (JSONException e) {
                throw ClassUtil.wrapRun(e);
            }
        } else {
            logger.error("DataObject instance is not a JsonObject");
        }
    }
    
    private Converter getConverter() {
        if(this.converter == null) {
            this.converter = property.getConverter();
        }
        
        return this.converter;
    }

    private void setExternal(Settings settings, JSONObject jsonObject, String name, Object propertyValue) throws JSONException {
        if(getConverter() != null) {
            getConverter().setExternal(settings, jsonObject, name, propertyValue);
        } else {
            Object instanceObj = propertyValue;
            if(BusinessObject.class.isAssignableFrom(propertyValue.getClass())) {
                instanceObj = ((BusinessObject)propertyValue).getInstance();
            }
            if(JSONObject.class.isAssignableFrom(instanceObj.getClass())) {
                convertersByClass.get(JSONObject.class).setExternal( settings, jsonObject, name, instanceObj);
            } else if (JSONArray.class.isAssignableFrom(instanceObj.getClass())) {
                convertersByClass.get(JSONArray.class).setExternal( settings, jsonObject, name, instanceObj);
            } else {
                logger.error("setExternal - converter not found for java type: " + getType().getInstanceClass().getName());
            }
        }
    }

    private Object toDomain(Settings settings, JSONObject jsonObject, String key) throws JSONException {
        if(getConverter() != null) {
            return getConverter().toDomain(settings, jsonObject, this.property, key);
        } else {
            if(logger.isDebugEnabled()) {
                logger.debug("Unknown converter for " + getType().getInstanceClass()
                        + ", type name: " + getType().getName());
            }
            return jsonObject.get(key);
        }

    }

    public void addElement(BusinessObject dataObject, Object element) {

        if(!JSONArray.class.isAssignableFrom( dataObject.getInstance().getClass())) {
            throw new IllegalArgumentException("DataObject instance "
                + dataObject.getInstance().getClass() + " is not of type JSONArray");
        }

        JSONArray jsonArray = (JSONArray) dataObject.getInstance();
        if(convertersByClass.containsKey(element.getClass())) {
            convertersByClass.get(element.getClass()).add(dataObject.getSettings(), jsonArray, element);
        } else {

            if(JSONObject.class.isAssignableFrom(element.getClass())) {
                convertersByClass.get(JSONObject.class).add(dataObject.getSettings(), jsonArray, element);
            } else if (JSONArray.class.isAssignableFrom(element.getClass())) {
                convertersByClass.get(JSONArray.class).add(dataObject.getSettings(), jsonArray, element);
            } else {
                logger.error("Element " + element.getClass() + " is not of type JsonValue/JsonObjectBuilder/JsonArrayBuilder");
            }
        }
    }

    public void addMapEntry(Object dataObject, Object key, Object value) {
        if(!JSONObject.class.isAssignableFrom(((BusinessObject) dataObject).getInstance().getClass())) {
            throw new IllegalArgumentException("DataObject is not of type JSONObject");
        }

        JSONObject jsonObject = (JSONObject) ((BusinessObject) dataObject).getInstance();
        try {
            jsonObject.put(key.toString(),  value);
        } catch (JSONException e) {
            throw ClassUtil.wrapRun(e);
        }
    }
}
