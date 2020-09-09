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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.JSONObjectProperty;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;

public class DateRange extends DefaultGenerator
{
    private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

    public DateRange (String[] arguments)
    {
        super(arguments);
    }

    @Override
    public Date getDateValue(StateGraph.ObjectGenerationVisitor visitor) {

        long minimum = 0;
        long maximum = (new Date()).getTime() + (1000*3600*24*365*2); // 2 years in future

        String dateFormat = JSONObjectProperty.ISO8601_FORMAT;
        if(getValues().length >= 3) {
            dateFormat = getValues()[2];
        }
        DateFormat df = new SimpleDateFormat(dateFormat);
        try {
            if(getValues().length >= 1 && !StringUtils.isBlank(getValues()[0])) {
                minimum = (df.parse(getValues()[0])).getTime();
            }
            if(getValues().length >= 2 && !StringUtils.isBlank(getValues()[1])) {
                maximum = (df.parse(getValues()[1])).getTime();
            }
        }
        catch (ParseException e) {
            logger.warn(
                "DateRange: problem parsing date string");
            e.printStackTrace();
        }

        if(maximum <= minimum) {
            logger.error("DateRange: max value is less than min value");
        }


        long  range = maximum - minimum;
        return new Date((long) (minimum + (range == 0 ? 0 : ClassUtil.nextDouble()*range)));
    }
}
