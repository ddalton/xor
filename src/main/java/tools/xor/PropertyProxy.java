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
    private AtomicReference<ExtendedProperty> atomicReference = new AtomicReference<>();
    private EntityType parentType;

    public ExtendedProperty bind(ExtendedProperty delegate, EntityType parentType) {
        this.atomicReference.set(delegate);
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
            if (atomicReference.get() == oldDelegate) {
                // Are we successful in creating a copy
                if (atomicReference.compareAndSet(oldDelegate, atomicReference.get().copy(parentType))) {

                    // Do the copy-on-write here
                    parentType.getShape().addProperty(parentType, atomicReference.get());
                }
                // someone else was successful, so update the delegate to the copy created by another thread
                else {
                    atomicReference.set((ExtendedProperty) parentType.getShape().getProperty(this.parentType, atomicReference.get().getName()));
                }
            }
        }
        
        return method.invoke(this.atomicReference.get(), args);
    }

    public ExtendedProperty getDelegate() {
        return this.atomicReference.get();
    }
    
    public ExtendedProperty getOldDelegate() {
        return this.oldDelegate;
    }
}
