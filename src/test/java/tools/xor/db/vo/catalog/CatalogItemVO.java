package tools.xor.db.vo.catalog;

import tools.xor.db.vo.base.IdVO;
import tools.xor.db.vo.base.MetaEntityVO;

public class CatalogItemVO extends IdVO{
	private CatalogVO catalog;

	public void setCatalog(CatalogVO value) {
		this.catalog = value;
	}

	public CatalogVO getCatalog() {
		return this.catalog;
	}

	private MetaEntityVO context;

	public MetaEntityVO getContext() {
		return this.context;
	}

	public void setContext(MetaEntityVO context) {
		this.context = context;
	}
}
