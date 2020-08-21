package tools.xor.logic;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.Settings;
import tools.xor.db.base.Technician;
import tools.xor.db.vo.base.TechnicianVO;
import tools.xor.service.AggregateManager;
import tools.xor.util.ClassUtil;
import tools.xor.view.AggregateViewFactory;

/**
 * Unit test the TopoSort algorithm.
 */

public class DefaultVersion {
	@Autowired
	protected AggregateManager aggregateManager;

	@BeforeAll
	public static void executeOnceBeforeAll() {
		ClassUtil.setParallelDispatch(false);
	}

	@AfterAll
	public static void executeOnceAfterAll() {
		ClassUtil.setParallelDispatch(true);
	}

	final String NAME = "GEORGE_HADE";
	final String DISPLAY_NAME = "George Hade";
	final String DESCRIPTION = "Technician to fix HVAC issues";
	final String USER_NAME = "ghade";
	final String SKILL = "HVAC";	
	final String OBJECT_ID = "ObjectId";

    /**
     * Test for versioned Basic info
     */
	@Test
	public void version1() {

		// create person
		Technician technician = new Technician();
		technician.setName(NAME);
		technician.setDisplayName(DISPLAY_NAME);
		technician.setDescription(DESCRIPTION);
		technician.setUserName(USER_NAME);
		technician.setObjectId(OBJECT_ID);
		technician.setSkill(SKILL);
		technician = (Technician) aggregateManager.create(technician, new Settings());

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateManager.getView("BASICINFO"));		
		List<?> toList = aggregateManager.query(technician, settings);

		assert(toList.size() == 1);

		TechnicianVO result = null;
		if(TechnicianVO.class.isAssignableFrom(toList.get(0).getClass()))
				result = (TechnicianVO) toList.get(0);
		
		assert(result != null);
		assert(result.getName().equals(NAME));
		assert(result.getDisplayName().equals(DISPLAY_NAME));
		assert(result.getDescription().equals(DESCRIPTION));
		
		// Object id is not available in version 1
		assert(result.getObjectId() == null);
	}

	public void version2() {

		int savedVersion = aggregateManager.getViewVersion();
		aggregateManager.setViewVersion(2);
		
		// Setup with version 2 views
		(new AggregateViewFactory()).load(aggregateManager);
		
		// create person
		Technician technician = new Technician();
		technician.setName(NAME);
		technician.setDisplayName(DISPLAY_NAME);
		technician.setDescription(DESCRIPTION);
		technician.setUserName(USER_NAME);
		technician.setObjectId(OBJECT_ID);
		technician.setSkill(SKILL);
		technician = (Technician) aggregateManager.create(technician, new Settings());

		// read the person object using a DataObject
		Settings settings = new Settings();
		settings.setView(aggregateManager.getView("BASICINFO"));		
		List<?> toList = aggregateManager.query(technician, settings);

		assert(toList.size() == 1);

		TechnicianVO result = null;
		if(TechnicianVO.class.isAssignableFrom(toList.get(0).getClass()))
				result = (TechnicianVO) toList.get(0);
		
		assert(result != null);
		assert(result.getName().equals(NAME));
		assert(result.getDisplayName().equals(DISPLAY_NAME));
		assert(result.getDescription().equals(DESCRIPTION));
		
		// Object id is not available in version 1
		assert(result.getObjectId().equals(OBJECT_ID));
		
		// reset to default version
		aggregateManager.setViewVersion(savedVersion);
		(new AggregateViewFactory()).load(aggregateManager);		
	}	
}
