package net.simpleframework.ado.lucene;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.AbstractADOManager;
import net.simpleframework.ado.bean.IIdBeanAware;
import net.simpleframework.ado.query.DataQueryUtils;
import net.simpleframework.ado.query.IDataQuery;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.Convert;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.object.ObjectFactory;
import net.simpleframework.common.web.html.HtmlUtils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class AbstractLuceneManager extends AbstractADOManager implements ILuceneManager {
	final Version version = Version.LUCENE_43;

	final List<String> queryFieldsCache = new ArrayList<String>();

	private FSDirectory directory;

	private Analyzer defaultAnalyzer;

	private final String[] queryFields;

	public AbstractLuceneManager(final File indexPath, final String[] queryFields) {
		try {
			this.directory = FSDirectory.open(indexPath);
		} catch (final IOException e) {
			throw ADOException.of(e);
		}
		this.queryFields = queryFields;
	}

	public AbstractLuceneManager(final File indexPath) {
		this(indexPath, null);
	}

	public File getIndexPath() {
		return directory.getDirectory();
	}

	protected String[] getQueryFields() {
		if (queryFields != null && queryFields.length > 0) {
			return queryFields;
		} else {
			return queryFieldsCache.toArray(new String[queryFieldsCache.size()]);
		}
	}

	@Override
	public boolean indexExists() {
		try {
			return DirectoryReader.indexExists(directory);
		} catch (final IOException e) {
			throw ADOException.of(e);
		}
	}

	protected Analyzer getDefaultAnalyzer() {
		if (defaultAnalyzer == null) {
			defaultAnalyzer = new SmartChineseAnalyzer(version);
		}
		return defaultAnalyzer;
	}

	protected IndexWriter createIndexWriter() throws IOException {
		final IndexWriterConfig iwConfig = new IndexWriterConfig(version, getDefaultAnalyzer());
		iwConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
		return new IndexWriter(directory, iwConfig);
	}

	protected String getId(final Object obj) {
		return obj instanceof IIdBeanAware ? Convert.toString(((IIdBeanAware) obj).getId()) : null;
	}

	protected boolean objectToDocument(final Object obj, final LuceneDocument doc)
			throws IOException {
		final String id = getId(obj);
		if (StringUtils.hasText(id)) {
			doc.addStringField("id", id, true);
		}
		return true;
	}

	protected IDataQuery<?> queryAll() {
		return null;
	}

	@Override
	public void rebuildIndex() {
		final IDataQuery<?> dq = queryAll();
		if (dq == null) {
			return;
		}
		IndexWriter iWriter = null;
		try {
			iWriter = createIndexWriter();
			if (indexExists()) {
				iWriter.deleteAll();
			}
			dq.setFetchSize(0);
			for (Object obj; (obj = dq.next()) != null;) {
				final LuceneDocument document = new LuceneDocument();
				if (objectToDocument(obj, document)) {
					iWriter.addDocument(document.doc);
				}
			}
			iWriter.commit();
		} catch (final IOException e) {
			throw ADOException.of(e);
		} finally {
			closeWriter(iWriter);
		}
	}

	@Override
	public void doAddIndex(final Object... objects) {
		IndexWriter iWriter = null;
		try {
			iWriter = createIndexWriter();
			for (final Object obj : objects) {
				final LuceneDocument document = new LuceneDocument();
				if (objectToDocument(obj, document)) {
					iWriter.addDocument(document.doc);
				}
			}
		} catch (final IOException e) {
			throw ADOException.of(e);
		} finally {
			closeWriter(iWriter);
		}
	}

	@Override
	public void doUpdateIndex(final Object... objects) {
		IndexWriter iWriter = null;
		try {
			iWriter = createIndexWriter();
			for (final Object obj : objects) {
				final String id = getId(obj);
				if (StringUtils.hasText(id)) {
					final LuceneDocument document = new LuceneDocument();
					if (objectToDocument(obj, document)) {
						iWriter.updateDocument(new Term("id", id), document.doc);
					}
				}
			}
		} catch (final IOException e) {
			throw ADOException.of(e);
		} finally {
			closeWriter(iWriter);
		}
	}

	@Override
	public void doDeleteIndex(final Object... objects) {
		IndexWriter iWriter = null;
		try {
			iWriter = createIndexWriter();
			for (final Object obj : objects) {
				final String id = getId(obj);
				if (StringUtils.hasText(id)) {
					iWriter.deleteDocuments(new Term("id", id));
				}
			}
		} catch (final IOException e) {
			throw ADOException.of(e);
		} finally {
			closeWriter(iWriter);
		}
	}

	@Override
	public String[] getQueryTokens(final String queryString) {
		try {
			final TokenStream tokenStream = getDefaultAnalyzer().tokenStream("QUERY_TOKENS",
					new StringReader(queryString));
			final ArrayList<String> al = new ArrayList<String>();
			while (tokenStream.incrementToken()) {
				final String term = tokenStream.getAttribute(CharTermAttribute.class).toString();
				if (term != null && term.length() > 1) {
					al.add(term);
				}
			}
			if (al.size() == 0) {
				al.add(queryString);
			}
			return al.toArray(new String[al.size()]);
		} catch (final IOException e) {
			throw ADOException.of(e);
		}
	}

	protected Object documentToObject(final LuceneDocument doc, final Class<?> beanClass) {
		Object obj;
		if (beanClass == null) {
			obj = new KVMap();
		} else {
			obj = ObjectFactory.newInstance(beanClass);
		}
		final String[] queryFields = getQueryFields();
		if (queryFields != null) {
			for (final String f : queryFields) {
				BeanUtils.setProperty(obj, f, doc.get(f));
			}
		}
		return obj;
	}

	private Query getQuery(final String queryString) {
		Query query = null;
		QueryParser qp;
		if (StringUtils.hasText(queryString)
				&& indexExists()
				&& (qp = new MultiFieldQueryParser(version, getQueryFields(), getDefaultAnalyzer())) != null) {
			try {
				query = qp.parse(queryString.trim());
			} catch (final ParseException e) {
				log.warn(e);
			}
		}
		return query;
	}

	@Override
	public <T> IDataQuery<T> query(final String queryString, final Class<T> beanClass) {
		final Query query = getQuery(queryString);
		if (query == null) {
			return DataQueryUtils.nullQuery();
		}
		return new LuceneQuery<T>(directory, query) {
			@SuppressWarnings("unchecked")
			@Override
			protected T toBean(final LuceneDocument doc) {
				return (T) documentToObject(doc, beanClass);
			}
		};
	}

	@Override
	public IDataQuery<Map<String, Object>> query(final String queryString) {
		final Query query = getQuery(queryString);
		if (query == null) {
			return DataQueryUtils.nullQuery();
		}
		return new LuceneQuery<Map<String, Object>>(directory, query) {
			@SuppressWarnings("unchecked")
			@Override
			protected Map<String, Object> toBean(final LuceneDocument doc) {
				return (Map<String, Object>) documentToObject(doc, null);
			}
		};
	}

	private void closeWriter(final IndexWriter indexWriter) {
		try {
			if (indexWriter != null) {
				indexWriter.close();
			}
		} catch (final Exception e) {
		}
	}

	/*---------------------------------utils----------------------------------*/

	protected String trimContent(final String contentHtml, final int length) {
		return StringUtils.substring(HtmlUtils.htmlToText(contentHtml), length);
	}

	protected String trimContent(final String contentHtml) {
		return trimContent(contentHtml, 250);
	}
}