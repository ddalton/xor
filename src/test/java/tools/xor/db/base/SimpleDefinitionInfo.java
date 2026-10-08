package tools.xor.db.base;

import java.io.Serializable;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

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
