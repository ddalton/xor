package tools.xor.util.xsd;

import tools.xor.Property;
import tools.xor.Type;

public abstract class AbstractGenerator {
	
	private Type type;

	public abstract void generate(XSDVisitor coordinator);
	
	public AbstractGenerator(Type type) {
		this.setType(type);
	}

	protected void processProperty(Property property, XSDVisitor coordinator) {
		if(property.getType().isDataType()) {
			if(property.isMany()) {
				(new XSDCollectionGenerator(property.getType())).generate(coordinator);
			}
		} else {
			(new XSDEntityGenerator(property.getType())).generate(coordinator);
		}
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}


	/**
	 * Overridden by subclasses
	 * @param coordinator
	 */
	public void generateBody(XSDVisitor coordinator) {
	}
}
