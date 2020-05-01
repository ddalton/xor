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

package tools.xor.util.xsd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import tools.xor.EntityType;
import tools.xor.ExtendedProperty;
import tools.xor.Property;
import tools.xor.Type;

public class XSDEntityGenerator extends AbstractGenerator {
	private static final Logger logger = LogManager.getLogger(new Exception()
	.getStackTrace()[0].getClassName());
	
	public XSDEntityGenerator(Type type) {
		super(type);
	}
	
	@Override
	public void generateBody(XSDVisitor coordinator) {
		Element indicator = coordinator.getDocument().createElement(XSDVisitor.XSD_PREFIX + "sequence");
		coordinator.push(indicator);
		
		// Save the type in the mapping before drill down
		coordinator.setType(getType(), getType().getName());

		if(EntityType.class.isAssignableFrom(getType().getClass())) {
			EntityType entityType = (EntityType) getType();
			for(Property property: entityType.getProperties()) {

				Element element = coordinator.getDocument().createElement(XSDVisitor.XSD_PREFIX + "element");
				coordinator.push(element);
				element.setAttribute("name", property.getName());
				if(property.isNullable()) {
					element.setAttribute("minOccurs", "0");
					if(property.isMany()) {
						element.setAttribute("nillable", "true");
					}
				}
				if(property.isMany()) {
					element.setAttribute("maxOccurs", "unbounded");
				}

				if(!coordinator.hasType(property.getType())) {
					coordinator.addToRoot(); // Add the new complex type to the root element
					processProperty(property, coordinator);
					coordinator.pop(); // pop out the root element
				}
				if(property.isMany()) {
					element.setAttribute("type", coordinator.getType( ((ExtendedProperty)property).getElementType() ));
				} else {
					element.setAttribute("type", coordinator.getType(property.getType()));
				}
				coordinator.pop(); // pop the element from the call stack
			}
		} 

		coordinator.pop(); // pop the indicator
	}
	
	@Override
	public void generate(XSDVisitor coordinator) {
		
		Element complexType = coordinator.getDocument().createElement(XSDVisitor.XSD_PREFIX + "complexType");
		coordinator.push(complexType);
		complexType.setAttribute("name", getType().getName());
		
		generateBody(coordinator);
		
		coordinator.pop(); // pop the complex type
	}

	
	
}
