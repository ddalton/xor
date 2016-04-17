package tools.xor.view;

import javax.xml.bind.annotation.XmlAttribute;


public class OutputLocation {
	@XmlAttribute
	private String name;
	
	private String parameter;
	
	private OutputType type;
	
	// Indexed from 1. Useful if the stored procedure is 
	// returning multiple results and not all the results are neede
	// The position specifies which result this corresponds to
	private int position;
	
	// The mechanism used by the stored procedure to return the output
	public enum OutputType {
		RETURN,
		PARAMETER;
	}
	
	public OutputLocation copy() {
		OutputLocation result = new OutputLocation();
		result.name = name;
		result.parameter = parameter;
		result.type = type;

		return result;
	}

	public void setName(String name) {
		this.name = name;
	}
	
    @XmlAttribute(name = "parameter")
	public String getParameter() {
		return this.parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}
	
    @XmlAttribute(name = "type")
	public OutputType getType() {
		return this.type;
	}

	public void setType(OutputType type) {
		this.type = type;
	};	
}
