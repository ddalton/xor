package tools.xor.view;

import javax.persistence.ParameterMode;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

public class ParameterMapping {
	@XmlAttribute
	String name;         // Optional if attribute is specified, required for non-view parameters
	
	@XmlAttribute
	String attribute;    // view parameter
	
	@XmlAttribute
	Class<?> type = void.class;       // required for non-view parameter
	
	@XmlAttribute
	int scale;           // Required for NUMERIC/DECIMAL OUT parameters
	
	@XmlAttribute
	String defaultValue;
	
	@XmlAttribute
	ParameterMode mode = ParameterMode.IN;  // Default is IN
	
	@XmlTransient
	int position;

	public void setName(String name) {
		this.name = name;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void setMode(ParameterMode mode) {
		this.mode = mode;
	}

}
