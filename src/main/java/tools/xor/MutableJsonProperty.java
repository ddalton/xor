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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tools.xor.JSONObjectProperty.Converter;

/**
 * This is designed to work with org.json.JSONObject
 * 
 * @author Dilip Dalton
 *
 */
public class MutableJsonProperty extends ExternalProperty {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());

	private final JSONObjectProperty jsonObjectProperty;
	private Converter converter;
	
	public MutableJsonProperty(ExtendedProperty domainProperty, Type type, ExternalType parentType, Type elementType) {
		super(domainProperty, type, parentType, elementType);
		jsonObjectProperty = new JSONObjectProperty(this);
	}

	public MutableJsonProperty(String name, ExtendedProperty domainProperty, Type type, ExternalType parentType, Type elementType) {
		super(name, domainProperty, type, parentType, elementType);
		jsonObjectProperty = new JSONObjectProperty(this);
	}
	
    public MutableJsonProperty(String name, Type type, EntityType parentType, Type elementType) {
        super(name, type, parentType, elementType);
        jsonObjectProperty = new JSONObjectProperty(this);
    }

	@Override
	public Class<?> getJavaType() {
		return this.jsonObjectProperty.getJavaType();
	}

	@Override
	public String getStringValue(BusinessObject dataObject)
	{
		return this.jsonObjectProperty.getStringValue(dataObject);
	}

	@Override
	public Object query(Object dataObject) {
		return this.jsonObjectProperty.query(dataObject);
	}
	
	@Override
	public Object getValue(BusinessObject dataObject)
	{
		return this.jsonObjectProperty.getValue(dataObject);
	}

	@Override
	public void setValue(Settings settings, Object dataObject, Object propertyValue)
	{
		this.jsonObjectProperty.setValue(settings, dataObject, propertyValue);
	}
	
	@Override
	public void addElement(BusinessObject dataObject, Object element) {

		this.jsonObjectProperty.addElement(dataObject, element);
	}

	@Override
	public void addMapEntry(Object dataObject, Object key, Object value) {
		this.jsonObjectProperty.addMapEntry(dataObject, key, value);
	}
	
	public void setConverter(Converter converter) {
	    this.converter = converter;
	}
	
	@Override
	public Converter getConverter() {
	    // Get the default converter for simple types
        if(this.converter == null && getType().isDataType() && !isMany()) {
            this.converter = super.getConverter();
        }	    
	    return this.converter;
	}
	
	public void setNullable(boolean value) {
	    this.isNullable = value;
	}
}
