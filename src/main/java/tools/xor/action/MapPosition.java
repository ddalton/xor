package tools.xor.action;

import java.util.HashSet;
import java.util.Set;

import tools.xor.BusinessObject;

public class MapPosition implements Position {

	private BusinessObject collectionElement;
	private Set<Object> oldPs = new HashSet<Object>();
	private Set<Object> newPs = new HashSet<Object>();
	
	public MapPosition(BusinessObject element) {
		this.collectionElement = element;
	}

	@Override
	public Set<Object> getAdded() {
		Set<Object> result = (new HashSet(newPs));
		result.removeAll(oldPs);
		
		return result;
	}

	@Override
	public void addToNew(Object position) {
		newPs.add(position);
	}

	@Override
	public Set getObsolete() {
		Set<Object> result = (new HashSet(oldPs));
		result.removeAll(newPs);
		
		return result;		
	}

	@Override
	public void addToOld(Object position) {
		oldPs.add(position);
	}

	@Override
	public BusinessObject getCollectionElement() {
		return this.collectionElement;
	}

	@Override
	public boolean isNew() {
		return (newPs.size() > 0 && oldPs.size() == 0);
	}

}
