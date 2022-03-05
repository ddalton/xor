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

package tools.xor.db.vo.base;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Chapter")
@XmlAccessorType(XmlAccessType.FIELD)
public class ChapterVO extends MetaEntityVO {

	//@XmlTransient  
	//@JsonIgnore
	private BookVO documentTemplate;

	public void setDocumentTemplate(BookVO value) {
		this.documentTemplate = value;
	}

	public BookVO getDocumentTemplate() {
		return this.documentTemplate;
	}

	private Set<ChapterVO> subChapters;

	public Set<ChapterVO> getSubChapters() {
		if (this.subChapters == null) {
			this.subChapters = new HashSet<ChapterVO>();
		}
		return this.subChapters;
	}

	//@XmlIDREF
	private ChapterVO parentChapter;

	public void setParentChapter(ChapterVO value) {
		this.parentChapter = value;
	}

	public ChapterVO getParentChapter() {
		return this.parentChapter;
	}

	private ChapterTypeVO chapterType;

	public void setChapterType(ChapterTypeVO value) {
		this.chapterType = value;
	}

	public ChapterTypeVO getChapterType() {
		return this.chapterType;
	}

	private String chapterUri;

	public void setChapterUri(String value) {
		this.chapterUri = value;
	}

	public String getChapterUri() {
		return this.chapterUri;
	}

	public void setSubChapters(Set <ChapterVO> value) {
		this.subChapters = value;
	}

	private boolean hidden;

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean value) {
		this.hidden = value;
	}	

}
