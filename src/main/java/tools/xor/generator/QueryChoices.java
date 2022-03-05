/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2020, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package tools.xor.generator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.providers.jdbc.JDBCDataStore;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;

/**
 * We treat the input as a Double to be aligned with POI library numeric value of a cell
 * being returned as a double type.
 */
public class QueryChoices extends DefaultGenerator
{
    private String sql;
    private Integer scale; // Needed for BigDecimal
    private Object[] queryValues;
    
    public QueryChoices (String[] arguments)
    {
        super(arguments);
        
        // This can be enhanced to support VISITOR_CONTEXT tokens
        // allowing the query to be parameterized
        this.sql = arguments[0];
        
        if(values.length >= 2) {
            scale = Integer.valueOf(values[1]);
        }
    }
    
    /**
     * Calculates a random index position from 0 to values.size-1
     * @param visitor object
     * @return an int value
     */
    protected int getPosition(StateGraph.ObjectGenerationVisitor visitor) {
        if(getQueryValues(visitor).length == 0) {
            throw new RuntimeException("Choices generator needs to have a minimum of 1 value.");
        }

        // Optimization to avoid invoking random()
        if(getQueryValues(visitor).length == 1) {
            return 0;
        }

        int result =  (int) (ClassUtil.nextDouble() * getQueryValues(visitor).length);
        if(result == getQueryValues(visitor).length) {
            result--;
        }

        return result;
    }    
    
    public Object[] getQueryValues (StateGraph.ObjectGenerationVisitor visitor)
    {
        if(this.queryValues == null) {
            Settings settings = visitor.getSettings();
            JDBCDataStore dataStore = (JDBCDataStore) settings.getDataStore();
            Connection connection = dataStore.getSessionContext().getConnection();
            
            List<Object> result = new LinkedList<>();
            try(PreparedStatement statement = StringTemplate.getStatement(this.sql, connection, visitor); ResultSet rs = statement.executeQuery(); ) {
                while(rs.next()) {
                    result.add(rs.getObject(1));
                }
            } catch (SQLException e) {
                throw ClassUtil.wrapRun(e);
            }
            
            queryValues = new Object[result.size()];
            this.queryValues = result.toArray(queryValues);
        }        
        
        return this.queryValues;
    }    

    @Override
    public Byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        Object value = getQueryValues(visitor)[getPosition(visitor)];
        
        if(value instanceof String) {
            return Double.valueOf(value.toString()).byteValue();            
        } else if(value instanceof Number) {
            return ((Number)value).byteValue();
        } 
        
        return (byte) value;
    }

    @Override
    public Short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        Object value = getQueryValues(visitor)[getPosition(visitor)];
        
        if(value instanceof String) {
            return Double.valueOf(value.toString()).shortValue();            
        } else if(value instanceof Number) {
            return ((Number)value).shortValue();
        } 
        
        return (short) value;        
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (char) getIntValue(visitor).intValue();
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        Object value = getQueryValues(visitor)[getPosition(visitor)];
        
        if(value instanceof String) {
            return Integer.valueOf(value.toString()).intValue();            
        } else if(value instanceof Number) {
            return ((Number)value).intValue();
        } 
        
        return (Integer) value;         
    }

    @Override
    public Long getLongValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        Object value = getQueryValues(visitor)[getPosition(visitor)];
        
        if(value instanceof String) {
            return Double.valueOf(value.toString()).longValue();            
        } else if(value instanceof Number) {
            return ((Number)value).longValue();
        } 
        
        return (long) value;         
    }

    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {
        return new Date(getLongValue(visitor));
    }

    @Override
    public Double getDoubleValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        Object value = getQueryValues(visitor)[getPosition(visitor)];
        
        if(value instanceof String) {
            return Double.valueOf(value.toString());            
        } else if(value instanceof Number) {
            return ((Number)value).doubleValue();
        } 
        
        return (Double) value;  
    }

    @Override
    public Float getFloatValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return getDoubleValue(visitor).floatValue();
    }

    @Override
    public BigDecimal getBigDecimal (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigDecimal(getBigInteger(visitor), this.scale);
    }

    @Override
    public BigInteger getBigInteger (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigInteger(new Long(getLongValue(visitor)).toString());
    }
    
    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        Object value = getQueryValues(visitor)[getPosition(visitor)];
        
        if(value != null) {
            return value.toString();
        }
        
        return null;
    }    
}
