package net.simpleframework.ado;

import java.util.Collection;
import java.util.LinkedHashSet;

import net.simpleframework.common.object.ObjectEx;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class AbstractADOManager extends ObjectEx implements IADOManager {

	/**
	 * 监听器
	 */
	private Collection<IADOListener> listeners;

	protected Collection<IADOListener> getListeners() {
		if (listeners == null) {
			listeners = new LinkedHashSet<IADOListener>();
		}
		return listeners;
	}

	@Override
	public void addListener(final IADOListener listener) {
		getListeners().add(listener);
	}

	@Override
	public boolean removeListener(final IADOListener listener) {
		return getListeners().remove(listener);
	}

	@Override
	public void reset() {
	}
}
