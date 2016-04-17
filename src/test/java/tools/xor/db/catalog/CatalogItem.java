package tools.xor.db.catalog;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import tools.xor.db.base.Id;
import tools.xor.db.base.MetaEntity;

@Entity
public class CatalogItem extends Id{
	private Catalog catalog;

	public void setCatalog(Catalog value) {
		this.catalog = value;
	}

	@ManyToOne(optional=false)
	public Catalog getCatalog() {
		return this.catalog;
	}

	private MetaEntity context;

	@ManyToOne(optional=false)
	public MetaEntity getContext() {
		return this.context;
	}

	public void setContext(MetaEntity context) {
		this.context = context;
	}
}
