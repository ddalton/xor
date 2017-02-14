package tools.xor.generator;

public class Lot
{
    int numLots;
    int value = 0;

    public Lot(int maxValue) {
        this.numLots = maxValue;

        if (maxValue == 0) {
            throw new RuntimeException("Lot needs to have a minimum of 1 value.");
        }
    }

    public int pick ()
    {
        value = (int)(Math.random() * (numLots + 1));
        if (value == numLots) {
            value--;
        }

        return value;
    }

    public int getValue ()
    {
        return this.value;
    }
}
