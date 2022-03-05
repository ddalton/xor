package tools.xor.util;

import java.util.HashMap;
import java.util.Map;

import tools.xor.BusinessObject;
import tools.xor.EntityKey;
import tools.xor.EntityType;
import tools.xor.NaturalEntityKey;

/**
 * This strategy is typically used for entities that have a natural key and
 * for operations that do not involve a READ or a CLONE.
 * 
 * This has to be combined with a PrefetchCache implementation for performance
 * reasons.
 * 
 * @author Dilip Dalton
 *
 */
public class NaturalKeyStrategy implements EntityKeyStrategy {
	
	private static EntityKeyStrategy instance = null;
	
	private NaturalKeyStrategy() {
	}

	public static EntityKeyStrategy getInstance() {
		if(instance == null) {
			instance = new NaturalKeyStrategy();
		}
		return instance;
	}	

	@Override
	public EntityKey execute(BusinessObject bo, String domainEntityName, String anchor) {
		
		if( !(bo.getType() instanceof EntityType) ) {
			throw new RuntimeException("The type " + bo.getType().getName() + " is not an entity type");
		}
		
		if (((EntityType)bo.getType()).getNaturalKey() == null) {
			throw new RuntimeException("The type " + bo.getType().getName() + " does not have a natural key");
		}

		Map<String, Object> naturalKey = new HashMap<String, Object>();
		for(String key: ((EntityType)bo.getType()).getExpandedNaturalKey()) {
			Object keyValue = bo.get(key);
			if(keyValue == null) {
				continue;
			}
			naturalKey.put(key, keyValue);
		}	
	
		return new NaturalEntityKey(naturalKey, domainEntityName, anchor);
	}
}
