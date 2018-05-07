package tools.xor.view;

import tools.xor.AggregateAction;
import tools.xor.EntityType;
import tools.xor.Type;
import tools.xor.service.Shape;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.util.graph.TypeGraph;
import tools.xor.util.graph.UnmodifiableTypeGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class UnmodifiableView implements View
{
    private View view;

    public UnmodifiableView(View view) {
        this.view = view;
    }

    private void raiseException() {
        throw new UnsupportedOperationException("Changes are not allowed on this view, make a copy of the view to make changes.");
    }

    @Override public boolean isExpanded ()
    {
        return view.isExpanded();
    }

    @Override public boolean isUnion ()
    {
        return view.isUnion();
    }

    @Override public int getVersion ()
    {
        return view.getVersion();
    }

    @Override public Join getJoin ()
    {
        return view.getJoin() != null ? new UnmodifiableJoin(view.getJoin()) : null;
    }

    @Override public List<Parameter> getParameter ()
    {
        if(view.getParameter() == null) {
            return null;
        }

        List result = new ArrayList(view.getParameter().size());
        for(Parameter param: view.getParameter()) {
            result.add(new UnmodifiableParam(param));
        }

        return result;
    }

    @Override public List<Filter> getFilter ()
    {
        if(view.getFilter() == null) {
            return null;
        }

        List result = new ArrayList(view.getFilter().size());
        for(Filter filter: view.getFilter()) {
            result.add(new UnmodifiableFilter(filter));
        }

        return result;
    }

    @Override public OQLQuery getUserOQLQuery ()
    {
        return view.getUserOQLQuery() == null ? null : new UnmodifiableOQLQuery(view.getUserOQLQuery());
    }

    @Override public void setUserOQLQuery (OQLQuery userOQLQuery)
    {
        raiseException();
    }

    @Override public NativeQuery getNativeQuery ()
    {
        return view.getNativeQuery() == null ? null : new UnmodifiableNativeQuery(view.getNativeQuery());
    }

    @Override public StoredProcedure getStoredProcedure (AggregateAction action)
    {
        StoredProcedure sp = view.getStoredProcedure(action);

        return sp == null ? null : new UnmodifiableSP(sp);
    }

    @Override public List<StoredProcedure> getStoredProcedure ()
    {
        if(view.getStoredProcedure() == null) {
            return null;
        }

        List result = new ArrayList(view.getStoredProcedure().size());
        for(StoredProcedure sp: view.getStoredProcedure()) {
            result.add(new UnmodifiableSP(sp));
        }
        return result;
    }

    @Override public void setStoredProcedure (List<StoredProcedure> storedProcedure)
    {
        raiseException();
    }

    @Override public Shape getShape ()
    {
        return view.getShape();
    }

    @Override public void setShape (Shape shape)
    {
        raiseException();
    }

    @Override public List<String> getAttributeList ()
    {
        return Collections.unmodifiableList(view.getAttributeList());
    }

    @Override public void setAttributeList (List<String> attributeList)
    {
        raiseException();
    }

    @Override
    public String getTypeName() {
        return view.getTypeName();
    }
    
	@Override
	public void setTypeName(String typeName) {
        raiseException();
    }    

    @Override public String getName ()
    {
        return view.getName();
    }

    @Override public void setName (String name)
    {
        raiseException();
    }

    @Override public QueryView getEntityView (Type type, boolean narrow)
    {
        return view.getEntityView(type, narrow);
    }

    @Override public Class inferDomainClass ()
    {
        return view.inferDomainClass();
    }

    @Override public View copy ()
    {
        return view.copy();
    }

    @Override public Set<String> getViewReferences ()
    {
        return view.getViewReferences();
    }

    @Override public boolean hasViewReference ()
    {
        return view.hasViewReference();
    }

    @Override public void expand ()
    {
        view.expand();
    }

    @Override public List<String> getExpandedList (List<String> input)
    {
        return view.getExpandedList(input);
    }

    @Override public Set<String> getExactAttributes ()
    {
        return view.getExactAttributes() != null ?
            Collections.unmodifiableSet(view.getExactAttributes()) :
            null;
    }

    @Override public Map<String, Pattern> getRegexAttributes ()
    {
        return view.getRegexAttributes() != null ?
            Collections.unmodifiableMap(view.getRegexAttributes()) :
            null;
    }

    @Override public boolean matches (String path)
    {
        return view.matches(path);
    }

    @Override public void addTypeGraph (EntityType type, TypeGraph<State, Edge<State>> value, boolean isExact)
    {
        raiseException();
    }

    @Override public TypeGraph<State, Edge<State>> getTypeGraph (EntityType entityType, boolean isExact)
    {
        return new UnmodifiableTypeGraph<>(view.getTypeGraph(entityType, isExact));
    }

    @Override public TypeGraph<State, Edge<State>> getTypeGraph (EntityType entityType)
    {
        return new UnmodifiableTypeGraph<>(view.getTypeGraph(entityType));
    }

    @Override public List<UnmodifiableView> getChildren ()
    {
        if(view.getChildren() == null) {
            return null;
        }

        List<UnmodifiableView> result = new ArrayList<>();
        for(View v: view.getChildren()) {
            if( !(view instanceof UnmodifiableView) ) {
                result.add(new UnmodifiableView(v));
            } else {
                result.add((UnmodifiableView)v);
            }
        }

        return result;
    }

	@Override
	public boolean isValid() {
		return view.isValid();
	}

}
