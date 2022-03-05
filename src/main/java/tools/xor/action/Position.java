package tools.xor.action;

import java.util.Set;

import tools.xor.BusinessObject;

public interface Position {
	
	public Set<Object> getAdded();
	
	public boolean isNew();	
	
	public void addToNew(Object position);
	
	public Set<Object> getObsolete();
	
	public void addToOld(Object position);

	public BusinessObject getCollectionElement();

}
