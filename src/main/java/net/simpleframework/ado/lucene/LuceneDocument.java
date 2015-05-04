package net.simpleframework.ado.lucene;

import java.io.Reader;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class LuceneDocument {
	final Document doc;

	/* 当前查询的评分 */
	float score;

	public LuceneDocument(final Document document, final float score) {
		this.doc = document == null ? new Document() : document;
		this.score = score;
	}

	public LuceneDocument(final Document document) {
		this(document, 0f);
	}

	public LuceneDocument() {
		this(null);
	}

	public void addStoredField(final String name, final String value) {
		doc.add(new StoredField(name, value));
	}

	public void addStringFields(final String name, final String[] vals, final boolean stored) {
		if (vals != null && vals.length > 0) {
			for (final String val : vals) {
				addStringField(name, val.trim(), stored);
			}
		}
	}

	public void addStringField(final String name, final String value, final boolean stored) {
		if (value == null) {
			return;
		}
		doc.add(new StringField(name, value, stored ? Store.YES : Store.NO));
	}

	public void addTextField(final String name, final String value, final boolean stored) {
		if (value == null) {
			return;
		}
		doc.add(new TextField(name, value, stored ? Store.YES : Store.NO));
	}

	public void addTextField(final String name, final Reader reader) {
		if (reader == null) {
			return;
		}
		doc.add(new TextField(name, reader));
	}

	public String get(final String name) {
		return doc.get(name);
	}

	public String[] getValues(final String name) {
		return doc.getValues(name);
	}

	public void removeField(final String name) {
		doc.removeField(name);
	}

	public void removeFields(final String name) {
		doc.removeFields(name);
	}
}
