package tools.xor.db.pm;

import tools.xor.db.base.Id;

import javax.persistence.Entity;

@Entity
public class PriorityTask extends Task
//public class PriorityTask extends Id
{
    private int priority;

    public int getPriority ()
    {
        return priority;
    }

    public void setPriority (int priority)
    {
        this.priority = priority;
    }
}
