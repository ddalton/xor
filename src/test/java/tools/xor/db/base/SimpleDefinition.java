package tools.xor.db.base;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

@Entity
public class SimpleDefinition extends tools.xor.db.base.Id
{

    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy="simpleDefinition", fetch= FetchType.LAZY, cascade= CascadeType.ALL)
    public Set<SimpleDefinitionInfo> getDefinitionInfoList ()
    {
        return definitionInfoList;
    }

    public void setDefinitionInfoList (Set<SimpleDefinitionInfo> generationInfoList)
    {
        this.definitionInfoList = generationInfoList;
    }


    private Set<SimpleDefinitionInfo> definitionInfoList;

}
