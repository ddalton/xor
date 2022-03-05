/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2019, Dilip Dalton
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.Property;
import tools.xor.util.graph.StateGraph;

/**
 * Takes a Query data field value and tokenizes it and then
 * extracts a single token
 * For example:
 * QUERY_DATA.1.0 - Represents the first token of the first query field
 * 
 * This generator is useful for populating join tables, whose columns
 * need to get data from multiple tables
 * 
 * NOTE: The start index of a query field is 1 and the sub field is 0
 */
public class QueryDataSubField extends DefaultGenerator
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());    

    // default delimiter
    private static final String DELIMITER = "@-@";

    private String delimiter = DELIMITER;

    public QueryDataSubField (String[] arguments)
    {
        super(arguments);

        // Second argument represents the delimiter
        if(arguments.length >= 2) {
            this.delimiter = arguments[1];
        }
    }

    private int getIndex() {
        String indexStr = values[0].trim().substring(QUERY_DATA.length());
        return Integer.valueOf(indexStr.split("\\.")[0]);
    }
    
    private int getSubIndex() {
        String indexStr = values[0].trim().substring(QUERY_DATA.length());
        return Integer.valueOf(indexStr.split("\\.")[1]);
    }    

    private String getValue(StateGraph.ObjectGenerationVisitor visitor) {
        Object[] queryData = (Object[])visitor.getContext();
        
        if(logger.isDebugEnabled()) {
            List<String> record = new ArrayList<>();
            for(Object obj: queryData) {
                record.add(obj==null?"":(obj.toString()+":"+obj.getClass().getName()));
            }
            logger.debug("QueryDataSubField#getValue -> " + String.join(",", record));
        }
        
        Object multiValued = queryData[getIndex()];
        if(multiValued == null) {
            return null;
        }
        
        String[] values = multiValued.toString().split(Pattern.quote(delimiter));
        return values[getSubIndex()];
    }

    @Override
    public Byte getByteValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (byte)getIntValue(visitor).intValue();
    }

    @Override
    public Short getShortValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (short)getIntValue(visitor).intValue();
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return (char) getIntValue(visitor).intValue();
    }

    @Override
    public Integer getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Integer.valueOf(getValue(visitor).toString());
    }

    @Override
    public Long getLongValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Long.valueOf(getValue(visitor).toString());
    }

    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {
        // The value returned should be in millis
        long timeInMillis = getLongValue(visitor);
        return new Date(timeInMillis);
    }

    @Override
    public Double getDoubleValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValue(visitor).toString());
    }

    @Override
    public Float getFloatValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        return Double.valueOf(getValue(visitor).toString()).floatValue();
    }

    @Override
    public BigDecimal getBigDecimal (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigDecimal(getValue(visitor).toString());
    }

    @Override
    public BigInteger getBigInteger (StateGraph.ObjectGenerationVisitor visitor)
    {
        return new BigInteger(getValue(visitor).toString());
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        return getValue(visitor).toString();
    }
}
