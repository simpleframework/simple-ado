package net.simpleframework.ado.lucene;

import java.io.IOException;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.query.AbstractDataQuery;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class LuceneQuery<T> extends AbstractDataQuery<T> {

	protected Directory directory;

	protected Query query;

	public LuceneQuery(final Directory directory, final Query query) {
		this.directory = directory;
		this.query = query;
	}

	private DirectoryReader _reader;

	private IndexSearcher _searcher;

	protected IndexSearcher getIndexSearcher() {
		if (_searcher == null) {
			try {
				_searcher = new IndexSearcher(_reader = DirectoryReader.open(directory));
			} catch (final IOException e) {
				log.warn(e.getMessage());
			}
		}
		return _searcher;
	}

	private int fetchSize = 30;

	@Override
	public int getFetchSize() {
		return fetchSize;
	}

	@Override
	public LuceneQuery<T> setFetchSize(final int fetchSize) {
		this.fetchSize = fetchSize;
		return this;
	}

	@Override
	public void close() {
		try {
			if (_reader != null) {
				_reader.close();
			}
		} catch (final IOException e) {
			throw ADOException.of(e);
		}
	}

	@Override
	public void move(final int toIndex) {
		super.move(toIndex);
		topDocs = null;
	}

	private TopDocs topDocs;

	private int j = 0;

	@Override
	public T next() {
		if (query == null) {
			return null;
		}
		i++;
		if (i < 0 || i >= getCount()) {
			return null;
		}
		final IndexSearcher searcher = getIndexSearcher();
		if (searcher == null) {
			return null;
		}
		final int fetchSize = getFetchSize();
		try {
			if (topDocs == null || j >= fetchSize) {
				final int topNum = i + fetchSize;
				final TopScoreDocCollector collector = TopScoreDocCollector.create(topNum, false);
				searcher.search(query, collector);
				topDocs = collector.topDocs(i, topNum);
				j = 0;
			}
			final ScoreDoc[] docs = topDocs.scoreDocs;
			if (j < docs.length) {
				final ScoreDoc scoreDoc = topDocs.scoreDocs[j++];
				T t = toBean(new LuceneDocument(searcher.doc(scoreDoc.doc), scoreDoc.score));
				if (t == null) {
					t = next();
				}
				return t;
			} else {
				return null;
			}
		} catch (final Exception e) {
			throw ADOException.of(e);
		}
	}

	protected abstract T toBean(final LuceneDocument doc);

	@Override
	public int getCount() {
		if (query == null) {
			return 0;
		}
		final IndexSearcher searcher = getIndexSearcher();
		if (searcher == null) {
			return 0;
		}
		if (count < 0) {
			final TotalHitCountCollector collector = new TotalHitCountCollector();
			try {
				searcher.search(query, collector);
				count = collector.getTotalHits();
			} catch (final IOException e) {
				throw ADOException.of(e);
			}
		}
		return count;
	}
}
