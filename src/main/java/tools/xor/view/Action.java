package tools.xor.view;

import tools.xor.Settings;
import tools.xor.service.PersistenceOrchestrator;

/**
 * Interface for any actions that need to be executed while processing an AggregateTree
 */
public interface Action
{
    void execute(QueryTreeInvocation qti, PersistenceOrchestrator po);
}
