/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.Settings;
import tools.xor.util.Constants;
import tools.xor.util.graph.StateGraph;
import tools.xor.view.QueryView;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class DefaultGenerator implements Generator
{
    public static final String PARENT_LINK = "[PARENT]";
    public static final String ROOT_LINK = "[ROOT]";
    public static final String GLOBAL_SEQ = "[GLOBAL_SEQ]";
    public static final String THREAD_NO = "[THREAD_NO]";
    public static final String ENTITY_SIZE = "[ENTITY_SIZE]";

    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());
    private String[] values;

    public DefaultGenerator (String[] arguments)
    {
        this.values = arguments;
    }

    public String[] getValues ()
    {
        return values;
    }

    @Override
    public byte getByteValue ()
    {
        byte minimum = Byte.MIN_VALUE;
        byte maximum = Byte.MAX_VALUE;

        if (values.length >= 1) {
            minimum = Byte.parseByte(values[0]);
        }
        if (values.length >= 2) {
            maximum = Byte.parseByte(values[1]);
        }

        byte range = (byte) (maximum - minimum);
        return (byte) (minimum + ((byte)(Math.random() * range)));
    }

    @Override
    public short getShortValue ()
    {
        short minimum = Short.MIN_VALUE;
        short maximum = Short.MAX_VALUE;

        if (values.length >= 1) {
            minimum = Short.parseShort(values[0]);
        }
        if (values.length >= 2) {
            maximum = Short.parseShort(values[1]);
        }

        short range = (short) (maximum - minimum);
        return (short) (minimum + ((short)(Math.random() * range)));
    }

    @Override
    public char getCharValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        char minimum = Character.MIN_VALUE;
        char maximum = Character.MAX_VALUE;

        if (values.length >= 1) {
            minimum = (char) Integer.valueOf(values[0]).intValue();
        }
        if (values.length >= 2) {
            maximum = (char) Integer.valueOf(values[1]).intValue();
        }

        char range = (char) (maximum - minimum);
        return (char) (minimum + ((char)(Math.random() * range)));
    }

    @Override
    public int getIntValue (StateGraph.ObjectGenerationVisitor visitor)
    {
        int minimum = Integer.MIN_VALUE;
        int maximum = Integer.MAX_VALUE;

        if (values.length >= 1) {
            minimum = Double.valueOf(values[0]).intValue();
        }
        if (values.length >= 2) {
            maximum = Double.valueOf(values[1]).intValue();
        }

        int range = maximum - minimum;
        return minimum + ((int)(Math.random() * range));
    }

    @Override
    public long getLongValue ()
    {
        long minimum = Long.MIN_VALUE;
        long maximum = Long.MAX_VALUE;

        if (values.length >= 1) {
            minimum = Long.parseLong(values[0]);
        }
        if (values.length >= 2) {
            maximum = Long.parseLong(values[1]);
        }

        long range = maximum - minimum;
        return minimum + ((long)(Math.random() * range));
    }

    @Override
    public Date getDateValue() {
        long minimum = 0;
        long maximum = (new Date()).getTime() + (1000*3600*24*365*2); // 2 years in future

        long  range = maximum - minimum;
        return new Date((long) (minimum + (Math.random() * range)));
    }

    @Override
    public Double getDoubleValue ()
    {
        double minimum = Double.MIN_VALUE;
        double maximum = Double.MAX_VALUE;

        if (values.length >= 1) {
            minimum = Double.parseDouble(values[0]);
        }
        if (values.length >= 2) {
            maximum = Double.parseDouble(values[1]);
        }

        double range = maximum - minimum;
        return minimum + (Math.random() * range);
    }

    @Override
    public Float getFloatValue ()
    {
        float minimum = Float.MIN_VALUE;
        float maximum = Float.MAX_VALUE;

        if (values.length >= 1) {
            minimum = Float.parseFloat(values[0]);
        }
        if (values.length >= 2) {
            maximum = Float.parseFloat(values[1]);
        }

        float range = maximum - minimum;
        return minimum + ((float)(Math.random() * range));
    }

    @Override
    public BigDecimal getBigDecimal ()
    {
        BigDecimal minimum = BigDecimal.ONE;
        BigDecimal maximum = new BigDecimal((new Long(Long.MAX_VALUE)).toString());

        if (values.length >= 1) {
            minimum = new BigDecimal(values[0]);
        }
        if (values.length >= 2) {
            maximum = new BigDecimal(values[1]);
        }

        return maximum.subtract(minimum).multiply( new BigDecimal( (new Double(Math.random())).toString() ) );
    }

    @Override
    public BigInteger getBigInteger ()
    {
        BigInteger minimum = BigInteger.ONE;
        BigInteger maximum = new BigInteger((new Long(Long.MAX_VALUE)).toString());

        if (values.length >= 1) {
            minimum = new BigInteger(values[0]);
        }
        if (values.length >= 2) {
            maximum = new BigInteger(values[1]);
        }

        long range = maximum.longValue() - minimum.longValue();
        return new BigInteger((new Long((long) (minimum.longValue() + (Math.random() * range)))).toString());
    }

    /**
     * Calculates a random index position from 0 to values.size-1
     * @return an int value
     */
    protected int getPosition() {
        if(getValues().length == 0) {
            throw new RuntimeException("Choices generator needs to have a minimum of 1 value.");
        }

        // Optimization to avoid invoking random()
        if(getValues().length == 1) {
            return 0;
        }

        int result =  (int) (Math.random() * (getValues().length+1));
        if(result == getValues().length) {
            result--;
        }

        return result;
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        int pos = getPosition();
        return getValues()[pos];
    }

    @Override public EntityType getSubType (EntityType entityType)
    {
        List<EntityType> subTypes = new ArrayList<EntityType>(entityType.getSubtypes());
        subTypes.add(entityType);

        int index =  (int) (Math.random() * (subTypes.size()+1));
        if(index == subTypes.size()) {
            index--;
        }

        return subTypes.get(index);
    }

    @Override public void validate (ExtendedProperty property)
    {

    }

    @Override public int getFanout (Settings settings, String path)
    {
        float sparseness = settings.getSparseness(path);
        return (int)(Math.random() * settings.getEntitySize().size() * sparseness);
    }

    public List<JSONObject> getExisting (Settings settings, String path, List<JSONObject> entitiesToChooseFrom)
    {
        assert(entitiesToChooseFrom != null && entitiesToChooseFrom.size() > 0);

        int fanOut = getFanout(settings, path);
        final int[] ints = new Random().ints(1, entitiesToChooseFrom.size()).distinct().limit(fanOut).toArray();

        List<JSONObject> result = new ArrayList<>();
        for(int i = 0; i < ints.length; i++) {
            result.add(entitiesToChooseFrom.get(ints[i]));
        }

        return result;
    }

    @Override public boolean isApplicableToCollectionElement ()
    {
        return false;
    }

    protected JSONObject getComponent(StateGraph.ObjectGenerationVisitor visitor, JSONObject current, String tokenName) {
        if(current == null) {
            return null;
        }

        if(tokenName.equals(PARENT_LINK)) {
            return current.getJSONObject(Constants.XOR.GEN_PARENT);
        } else if(tokenName.equals(ROOT_LINK)) {
            return visitor.getRoot();
        } else if(current.has(tokenName) && current.get(tokenName) instanceof JSONObject) {
            return current.getJSONObject(tokenName);
        }

        return null;
    }

    protected int getMaxCollectionElements() {
        return new Integer(new Double(getValues()[1]).intValue());
    }

    protected String getDependencyValue(StateGraph.ObjectGenerationVisitor visitor) {

        JSONObject parent = visitor.getParent();
        if(parent == null) {
            return null;
        }

        String path = getValues()[0];

        // For each path, loop through each component and see if the value is available
        while (path != null) {
            // Extract the next attribute in the path
            String component = QueryView.getTopAttribute(path);

            // Get the path ready for the next round if applicable
            path = QueryView.getNext(path);

            JSONObject current = parent;
            parent = getComponent(visitor, current, component);
            if (parent == null) {
                // Maybe we are at the last component
                if (path == null) {

                    // Unable to find the dependency, probably accessed through another path
                    if(!current.has(component)) {
                        return null;
                    }

                    //perform conversion if necessary
                    Object value = current.get(component);
                    if (value != null && value instanceof Number) {
                        return ((Number)value).toString();
                    }

                    return current.getString(component);
                }
                else {
                    // If the object has not yet been created, then try the next path
                    continue;
                }
            }
        }

        return null;
    }
}
