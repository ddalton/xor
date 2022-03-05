package tools.xor.generator;

public class LinkedChoices extends Choices
{
    Lot lot;

    public LinkedChoices (String[] arguments)
    {
        super(arguments);
    }

    public void setLot(Lot lot) {
        this.lot = lot;
    }

    /**
     * Used to notify the generator that the next entity is being processed.
     * This will help the generator to get the next lot (random) number.
     * The lot number is shared by multiple columns that need to be grouped together.
     *
     * @return the lot value
     */
    public int castLot ()
    {
        return lot.pick();
    }

    @Override
    protected int getPosition() {
        return lot.getValue();
    }
}
