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

package tools.xor.db.base;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class Chapter extends MetaEntity{	

	private Book book;

	@ManyToOne
	public Book getBook() {
		return this.book;
	}

	public void setBook(Book book) {
		this.book = book;
	}

	private Set<Chapter> subChapter;

	@OneToMany(mappedBy="parentChapter", cascade = CascadeType.ALL, orphanRemoval = true )
	public Set<Chapter> getSubChapters() {
		return this.subChapter;
	}

	public void setSubChapters(Set<Chapter> value) {
		this.subChapter = value;
	}

	private Chapter parentChapter;

	@ManyToOne
	public Chapter getParentChapter() {
		return this.parentChapter;
	}

	public void setParentChapter(Chapter value) {
		this.parentChapter = value;
	}

	private ChapterType chapterType;

	@ManyToOne(optional=false)
	public ChapterType getChapterType() {
		return this.chapterType;
	}

	public void setChapterType(ChapterType value) {
		this.chapterType = value;
	}

	private String chapterUri;

	public void setChapterUri(String value) {
		this.chapterUri = value;
	}
	public String getChapterUri() {
		return this.chapterUri;
	}

	private boolean hidden;

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean value) {
		this.hidden = value;
	}
}
