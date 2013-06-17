/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.napoli.gecko.core.util;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link ConcurrentHashMap}-backed {@link Set}.
 * 
 * @author The Apache MINA Project (dev@mina.apache.org)
 * @version $Rev: 166684 $, $Date: 2012-04-24 13:08:27 +0800 (星期二, 24 四月 2012) $
 */
public class ConcurrentHashSet<E> extends MapBackedSet<E> {

	private static final long serialVersionUID = 8518578988740277828L;

	public ConcurrentHashSet() {
		super(new ConcurrentHashMap<E, Boolean>());
	}

	public ConcurrentHashSet(Collection<E> c) {
		super(new ConcurrentHashMap<E, Boolean>(), c);
	}

	@Override
	public boolean add(E o) {
		Boolean answer = ((ConcurrentMap<E, Boolean>) map).putIfAbsent(o,
				Boolean.TRUE);
		return answer == null;
	}
}