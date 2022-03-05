package tools.xor.view;

import javax.xml.bind.annotation.XmlAttribute;


public class OutputLocation {
	
	private String parameter;

	// Should match result position of the view
	// We use negative positions as 0 and positive numbers are used to
	// denote the position of the result sets returned from the stored procedure call
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
