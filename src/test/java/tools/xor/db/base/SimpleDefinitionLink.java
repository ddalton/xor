package tools.xor.db.base;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class SimpleDefinitionLink implements Serializable
{

  private static final long serialVersionUID = 1L;

  public SimpleDefinitionLink() {
  }

  public SimpleDefinitionLink(String simpleDefinitionId, Integer sourceId) {
    this.simpleDefinitionId = simpleDefinitionId;
    this.sourceId = sourceId;
  }

  private String simpleDefinitionId;

  private Integer sourceId;

  public String getSimpleDefinitionId ()
  {
    return simpleDefinitionId;
  }

  public void setSimpleDefinitionId (String value)
  {
    this.simpleDefinitionId = value;
  }

  public Integer getSourceId ()
  {
    return sourceId;
  }

  public void setSourceId (Integer sourceId)
  {
    this.sourceId = sourceId;
  }
}
