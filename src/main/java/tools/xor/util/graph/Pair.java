package tools.xor.util.graph;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public final class Pair<V> {
	private final V start;
	private final V end;

	public Pair(V value1, V value2) {
		this.start = value1;
		this.end = value2;
	}

	public V getStart() {
		return this.start;
	}
	
	public V getEnd() {
		return this.end;
	}
	
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
            // if deriving: appendSuper(super.hashCode()).
            append(this.start).
            append(this.end).
            toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
       if (!(obj instanceof Pair))
            return false;
        if (obj == this)
            return true;

        Pair<V> rhs = (Pair<V>) obj;
        return new EqualsBuilder().
            append(this.start, rhs.start).
            append(this.end, rhs.end).
            isEquals();
    }
}
