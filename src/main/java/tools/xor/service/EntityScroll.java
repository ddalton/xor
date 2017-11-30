package tools.xor.service;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

public interface EntityScroll<T> extends Closeable, Iterator
{
    void close () throws IOException;

    boolean hasNext ();

    T next ();
}
