package tools.xor.view;

import tools.xor.service.DataStore;

/**
 * Interface for any actions that need to be executed while processing an AggregateTree
 */
public interface Action
{
    /**
     * Execute any actions associated with the query tree
     * @param dispatcher some actions are only valid in a SerialDispatcher
     * @param qti needed to get the invocation id and any parent ids
     * @param po used to populate the temp join table
     */
    void execute(AbstractDispatcher dispatcher, QueryTreeInvocation qti, DataStore po);

    Action copy(Object context);
}
