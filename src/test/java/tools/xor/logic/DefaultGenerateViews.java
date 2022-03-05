package tools.xor.logic;

import org.springframework.beans.factory.annotation.Autowired;

import tools.xor.service.AggregateManager;
import tools.xor.view.AggregateViewFactory;

public class DefaultGenerateViews {

	@Autowired
	protected AggregateManager aggregateManager;
	
    /**
     * Generate XML view files by package
     */
	public void generate() {
		(new AggregateViewFactory()).generateQueries(aggregateManager);
	}

	public void domRewrite() {
		(new AggregateViewFactory()).testDOMRewrite(aggregateManager);
	}
}