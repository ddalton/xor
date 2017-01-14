package tools.xor.view;

import javax.xml.bind.annotation.XmlAttribute;


public class OutputLocation {
	
	private String parameter;

	// Indexed from 1. Useful if the stored procedure is
	// returning multiple results and not all the results are needed
	// The position specifies which result this corresponds to
	// used for validation
	private int position;
	
	public OutputLocation copy() {
		OutputLocation result = new OutputLocation();
		result.parameter = parameter;
		result.position = position;

		return result;
	}
	
    @XmlAttribute(name = "parameter")
	public String getParameter() {
		return this.parameter;
	}

	public void setParameter(String parameter) {
		this.parameter = parameter;
	}

	@XmlAttribute(name = "position")
	public int getPosition ()
	{
		return position;
	}

	public void setPosition (int position)
	{
		this.position = position;
	}
}
