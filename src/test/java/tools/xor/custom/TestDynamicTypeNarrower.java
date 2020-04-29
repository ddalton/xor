/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations 
 * under the License.
 */

package tools.xor.custom;

import tools.xor.AbstractTypeNarrower;
import tools.xor.db.enums.base.MetaEntityTypeEnum;
import tools.xor.db.vo.base.CitationVO;
import tools.xor.db.vo.base.MetaEntityVO;
import tools.xor.db.vo.base.PatentVO;
import tools.xor.service.Shape;
import tools.xor.util.ClassUtil;
import tools.xor.view.View;

public class TestDynamicTypeNarrower extends AbstractTypeNarrower {

	@Override
	public String downcast(Shape shape, Object entity, View view) {
		Class<?> entityClass = ClassUtil.getUnEnhanced(entity.getClass());
		
		if(MetaEntityVO.class.isAssignableFrom(entityClass)) {
			MetaEntityVO entityVO = (MetaEntityVO) entity;
			String type = entityVO.getMetaEntityType().getName();
			
			if(MetaEntityTypeEnum.PATENT.name().equals(type))
				return PatentVO.class.getName();
			else if(MetaEntityTypeEnum.CITATION.name().equals(type))
				return CitationVO.class.getName();
		}
		
		return super.downcast(shape, entity, view);
	}
}
