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

package tools.xor;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import tools.xor.generator.Generator;
import tools.xor.util.ClassUtil;
import tools.xor.util.graph.StateGraph;

/**
 * @author Dilip Dalton
 * 
 */
public class DateType extends SimpleType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private long min = 0;
	private long max = (new Date()).getTime() + (1000*3600*24*365*2); // 2 years from current time

	public long getMin() {
		return min;
	}

	public void setMin(Date min) {
		this.min = min.getTime();
	}

	public long getMax() {
		return max;
	}

	public void setMax(Date max) {
		this.max = max.getTime();
	}

	public DateType(Class<?> clazz) {
		super(clazz);
	}

	@Override
	public Object newInstance(Object instance) {

		// Date
		if (instance instanceof Date) {
			Date date = (Date) instance;
			return new Date(date.getTime());
		} else if (instance instanceof Timestamp) {
			Timestamp ts = (Timestamp) instance;
			return new Timestamp(ts.getTime());
		} else if(instance != null && Date.class.isAssignableFrom(instance.getClass())) {
			// Date subclass, return unchanged as we don't know how to construct it
			return instance;
		}
		
		return null;
	}

	@Override
	public Object generate(Settings settings, Property property, JSONObject rootedAt, List<JSONObject> entitiesToChooseFrom,
						   StateGraph.ObjectGenerationVisitor visitor) {

		Generator gen = ((ExtendedProperty)property).getGenerator(visitor.getRelationshipName());
		if(gen != null) {
			return gen.getDateValue(visitor);
		}

		long  range = getMax() - getMin();
		return new Date((long) (getMin() + (range == 0 ? 0 : ClassUtil.nextDouble()*range)));
	}	
	
    @Override
    public String getJsonType() {
        return MutableJsonType.JSONSCHEMA_STRING_TYPE;
    }	
    
    @Override
    public String getJsonFormat() {
        return MutableJsonType.JSONSCHEMA_FORMAT_DATE_TIME;
    }      
}
