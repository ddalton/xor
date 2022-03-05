package tools.xor.db.base;

import java.io.Serializable;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

@Entity
public class SimpleDefinitionInfo implements Serializable
{
  private static final long serialVersionUID = 1L;

  public  SimpleDefinitionInfo()
  {    
  }

  public SimpleDefinitionLink getId ()
  {
    return id;
  }

  public void setId (SimpleDefinitionLink id)
  {
    this.id = id;
  }

  public SimpleDefinition getSimpleDefinition ()
  {
    return simpleDefinition;
  }

  public void setSimpleDefinition (SimpleDefinition value)
  {
    this.simpleDefinition = value;
  }

  public  SimpleDefinitionInfo(String definitionId, Integer sourceId)
  {    
    this.id = new SimpleDefinitionLink(definitionId, sourceId);
  }

  @EmbeddedId
  private SimpleDefinitionLink id;

  @ManyToOne
  @MapsId("simpleDefinitionId")
  private SimpleDefinition simpleDefinition;
}
