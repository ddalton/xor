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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An OpenType represents a custom type that is a composition of properties from other
 * persistence managed types.
 */
public class OpenType extends AbstractType {
	private static final Logger logger = LogManager.getLogger(new Exception().getStackTrace()[0].getClassName());	

	private String name;

	public static final String DELIM = "#";

	/**
	 * Open properties will be specified a set of strings in the form
	 * classname1#propertyname1
	 * classname1#propertyname2
	 * classname2#propertyname1
	 *
	 * The property name can be in dotted form, but it will be flattened in the actual object instance
	 *
 	 */
	private Set<String> openProperties;

	public OpenType(String name, Set<String> properties) {
		super();		

		this.name = name;
		this.openProperties = properties;
		
		// We do not call AbstractType#init because that requires an instance class and
		// open types do not have an instance class
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getEntityName() {
		return getName();
	}

	@Override
	public List<Type> getEmbeddableTypes() {
		return new ArrayList<Type>();
	}	

	@Override
	public String getURI() {
		return null;
	}

	@Override
	public Class<?> getInstanceClass() {
		return null;
	}

	@Override
	public boolean isInstance(Object object) {
		// TODO: Might have to handle JSONObject
		return false;
	}

	@Override
	public Property getProperty(String path) {
		return getShape().getProperty(this, path);
	}

	public void setProperty() {

		if(getShape().getProperties(this) == null) {
			for (String p : openProperties) {
				String[] tokens = p.split(DELIM);
				if (tokens.length != 2) {
					throw new RuntimeException(
						"The open property should be of the form <Class name>#<property name>");
				}
				Type t = getShape().getType(tokens[0]);
				Property property = t.getProperty(tokens[1]);
				getShape().addProperty(this, property);
			}
		}
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public boolean isSequenced() {
		return false;
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@Override
	public List<Property> getDeclaredProperties() {
		return getProperties();
	}

	@Override
	public List<?> getAliasNames() {
		return new ArrayList<String>();
	}

	@Override
	public List<?> getInstanceProperties() {
		return new ArrayList<Object>();
	}

	@Override
	public Object get(Property property) {
		return null;
	}

	@Override
	public AccessType getAccessType() {
		throw new UnsupportedOperationException("An open type does not define an access mechanism");
	}

	@Override
	public Property getIdentifierProperty() {
		return null;
	}

	@Override
	public boolean isEmbedded() {
		return false;
	}
	
	@Override
	public boolean isEntity() {
		return true;
	}	

	@Override
	public Property getVersionProperty() {
		return null;
	}

	@Override
	public boolean supportsDynamicUpdate() {
		return false;
	}

}
