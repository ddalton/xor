package tools.xor.util;

import tools.xor.BusinessObject;
import tools.xor.EntityKey;
import tools.xor.SurrogateEntityKey;

public class SurrogateKeyStrategy implements EntityKeyStrategy {

	private static EntityKeyStrategy instance = null;
	
	private SurrogateKeyStrategy() {
	}

	public static EntityKeyStrategy getInstance() {
		if(instance == null) {
			instance = new SurrogateKeyStrategy();
		}
		return instance;
	}	
	
	@Override
	public EntityKey execute(BusinessObject bo, String domainEntityName, String anchor) {
		Object keyValue = bo.getIdentifierValue();
		
		return new SurrogateEntityKey(keyValue, domainEntityName, anchor);
	}
}
