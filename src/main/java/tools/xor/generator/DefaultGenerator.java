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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

public class DefaultGenerator implements Generator
{
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
    public char getCharValue ()
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
    public int getIntValue ()
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

    protected int getPosition() {
        if(getValues().length == 0) {
            throw new RuntimeException("Choices generator needs to have a minimum of 1 value.");
        }

        int result =  (int) (Math.random() * (getValues().length+1));
        if(result == getValues().length) {
            result--;
        }

        return result;
    }

    @Override
    public String getStringValue ()
    {
        int pos = getPosition();
        return getValues()[pos];
    }
}
