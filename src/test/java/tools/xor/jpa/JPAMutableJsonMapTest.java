package tools.xor.jpa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Resource;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import tools.xor.AbstractDBTest;
import tools.xor.Settings;
import tools.xor.db.pm.Project;
import tools.xor.service.AggregateManager;
import tools.xor.util.ClassUtil;

/**
 * Query a map collection into mutable JSON.
 *
 * Creating a Project from a POJO with this AggregateManager leaves state behind that breaks
 * subsequent operations (see JPAMutableJsonTest.checkReferenceSemantics), and is affected by
 * the state left by other tests, so this test uses its own Spring context.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:/spring-mutable-JSON-jpa-test.xml" })
// A unique property gives this class its own Spring context instead of the shared cached one
@TestPropertySource(properties = "xor.test.context=JPAMutableJsonMapTest")
@Transactional
public class JPAMutableJsonMapTest extends AbstractDBTest {

	@Resource(name = "aggregateManager")
	protected AggregateManager aggregateService;

	// The parallel dispatcher queries using other connections that do not see the test data
	@BeforeAll
	public static void executeOnceBeforeAll() {
		ClassUtil.setParallelDispatch(false);
	}

	@AfterAll
	public static void executeOnceAfterAll() {
		ClassUtil.setParallelDispatch(true);
	}

	private Project createProjects(Map<String, Project> subProjects) {
		Project master = new Project();
		master.setName("INFRASTRUCTURE");
		master.setDisplayName("Infrastructure");
		master.setDescription("Project to setup the new infrastructure");
		master = (Project) aggregateService.create(master, new Settings());

		for(String name: new String[] {"SETUP_NETWORK", "SETUP_TELEPHONE"}) {
			Project sub = new Project();
			sub.setName(name);
			sub.setDisplayName(name);
			sub.setDescription(name);
			sub = (Project) aggregateService.create(sub, new Settings());
			subProjects.put(name, sub);
		}
		master.setSubProjects(subProjects);

		return master;
	}

	private void checkMap(String viewName) {
		Map<String, Project> subProjects = new HashMap<>();
		Project master = createProjects(subProjects);

		Settings settings = getSettings();
		settings.setEntityClass(Project.class);
		settings.setView(aggregateService.getView(viewName));
		settings.setPreFlush(true);
		List<?> result = aggregateService.query(master, settings);

		assert(result.size() == 1);
		JSONObject root = (JSONObject) result.get(0);
		assert(root.getString("name").equals("INFRASTRUCTURE"));

		// The JSON object representing the map only contains the map entries
		JSONObject jsonSubProjects = root.getJSONObject("subProjects");
		assert(jsonSubProjects.keySet().equals(subProjects.keySet())) : "Unexpected map: " + jsonSubProjects;
		for(String name: subProjects.keySet()) {
			assert(jsonSubProjects.getJSONObject(name).getString("name").equals(name)) : "Unexpected map: " + jsonSubProjects;
		}
	}

	@Test
	public void queryMap() {
		checkMap("SUBPROJECTS");
	}

	/**
	 * The map key is provided by the subProjects.KEY_ column of a native query
	 */
	@Test
	public void queryMapNative() {
		checkMap("SUBPROJECTS_NATIVE");
	}
}
