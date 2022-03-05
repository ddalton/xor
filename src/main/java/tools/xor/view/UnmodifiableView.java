package tools.xor.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONObject;

import tools.xor.AggregateAction;
import tools.xor.EntityType;
import tools.xor.Settings;
import tools.xor.Type;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.util.Edge;
import tools.xor.util.State;
import tools.xor.util.graph.StateGraph;
import tools.xor.util.graph.TypeGraph;
import tools.xor.util.graph.UnmodifiableTypeGraph;

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

    @Override public int getVersion ()
    {
        return view.getVersion();
    }

    @Override public List<Join> getJoin ()
    {
        List<Join> result = new LinkedList<>();

        if(view.getJoin() != null) {
            for (Join join : view.getJoin()) {
                result.add(new UnmodifiableJoin(join));
            }
        }

        return view.getJoin() != null ? result : null;
    }

    @Override public List<Function> getFunction ()
    {
        if(view.getFunction() == null) {
            return null;
        }

        List result = new ArrayList(view.getFunction().size());
        for(Function function : view.getFunction()) {
            result.add(new UnmodifiableFunction(function));
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

    @Override public List<String> getConsolidatedAttributes ()
    {
        return Collections.unmodifiableList(view.getConsolidatedAttributes());
    }

    @Override public List<String> getAttributeList ()
    {
        return Collections.unmodifiableList(view.getAttributeList());
    }

    @Override public JSONObject getJson() {
        if(view.getJson() != null) {
            return ClassUtil.copyJson(view.getJson());
        } else {
            return null;
        }
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

    @Override public String getAnchorPath ()
    {
        return view.getAnchorPath();
    }

    @Override public void setAnchorPath (String path)
    {
        raiseException();
    }

    @Override public AggregateTree getAggregateTree (Type type)
    {
        return view.getAggregateTree(type);
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

    @Override public boolean isCompositionView ()
    {
        return view.isCompositionView();
    }

    @Override public void expand ()
    {
        view.expand();
    }

    @Override public void expand(List<String> expanding) {
        view.expand(expanding);
    }

    @Override
    public List<String> getExpandedList(List<String> input, List<String> expanding)
    {
        return view.getExpandedList(input, expanding);
    }

    @Override
    public Set<String> getFunctionAttributes()
    {
        return view.getFunctionAttributes();
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

    @Override public void addTypeGraph (EntityType type, TypeGraph<State, Edge<State>> value, StateGraph.Scope scope)
    {
        raiseException();
    }

    @Override public boolean isTree (Settings settings)
    {
        return view.isTree(settings);
    }

    @Override public TypeGraph<State, Edge<State>> getTypeGraph (EntityType entityType, StateGraph.Scope scope)
    {
        return new UnmodifiableTypeGraph<>(view.getTypeGraph(entityType, scope));
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

    @Override public boolean isSplitToRoot ()
    {
        return view.isSplitToRoot();
    }

    @Override public void setSplitToRoot (boolean value)
    {
        raiseException();
    }

    @Override public boolean isCustom ()
    {
        return view.isCustom();
    }

    @Override public List<String> getPrimaryKeyAttribute ()
    {
        return view.getPrimaryKeyAttribute();
    }

    @Override public boolean isTempTablePopulated ()
    {
        return view.isTempTablePopulated();
    }

    @Override public void setTempTablePopulated (boolean tempTablePopulated)
    {
        raiseException();
    }

    @Override public Integer getResultPosition ()
    {
        return view.getResultPosition();
    }
}
