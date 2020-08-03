package tools.xor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PropertyProxy to allow for copy-on-write semantics when the Shape typeInheritance is VALUE
 */
public class PropertyProxy implements InvocationHandler {
    
    private ExtendedProperty oldDelegate;
    private AtomicReference<ExtendedProperty> delegate = new AtomicReference<>();
    private EntityType parentType;

    public ExtendedProperty bind(ExtendedProperty delegate, EntityType parentType) {
        this.delegate.set(delegate);
        this.oldDelegate = delegate;
        this.parentType = parentType;
        
        ExtendedProperty proxy = (ExtendedProperty) Proxy.newProxyInstance(
                ExtendedProperty.class.getClassLoader(), new Class[] { ExtendedProperty.class }, this);    
        
        return proxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        
        // For now, copy-on-write semantics is supported only when setting a generator
        // This can be expanded to all mutable methods in the future
        if(method.getName().equals("setGenerator")) {
            // Make a copy against the correct type
            // delegate is no longer a proxy
            if (delegate.compareAndSet(oldDelegate, delegate.get().copy(parentType))) {

                // Do the copy-on-write here
                parentType.getShape().addProperty(delegate.get());
            }
        }
        
        return method.invoke(this.delegate.get(), args);
    }

    public ExtendedProperty getDelegate() {
        return this.delegate.get();
    }
    
    public ExtendedProperty getOldDelegate() {
        return this.oldDelegate;
    }
}
