/* JoinFunctionMapper.java

	Purpose:
		
	Description:
		
	History:
		Apr 7, 2010 12:54:13 PM, Created by henrichen

Copyright (C) 2010 Potix Corporation. All Rights Reserved.

*/

package io.keikai.model.impl.sys.formula;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.zkoss.xel.Function;
import org.zkoss.xel.FunctionMapper;
import org.zkoss.xel.XelException;

/**
 * Aggregate FunctionMapper that join serveral {@link FunctionMapper} together. The first
 * added get called first.
 * @author henrichen
 *
 */
public class JoinFunctionMapper implements FunctionMapper, Serializable {
	private static final long serialVersionUID = 1199148162868283007L;
	
	private LinkedHashSet<FunctionMapper> _mappers;
	
	public JoinFunctionMapper(FunctionMapper mapper) {
		_mappers = new LinkedHashSet<FunctionMapper>(4);
		if (mapper != null) {
			addFunctionMapper(mapper);
		}
	}
	
	/* Add a new Function Mapper.
	 * @see io.keikai.model.Book#addFunctionMapper(java.lang.String, io.keikai.model.FunctionMapper)
	 */
	/*package*/ void addFunctionMapper(FunctionMapper mapper) {
		_mappers.add(mapper);
	}

	/* Remove a specified Function Mapper.
	 * @see io.keikai.model.Book#removeFunctionMapper(java.lang.String, io.keikai.model.FunctionMapper)
	 */
	/*package*/ void removeFunctionMapper(FunctionMapper mapper) {
		_mappers.remove(mapper);
	}
	
	//--FunctionMapper--//
	public Collection<?> getClassNames() {
		return new ArrayList<String>(0);
	}


	public Class<?> resolveClass(String name) throws XelException {
		return null;
	}

	@Override
	public Function resolveFunction(String arg0, String arg1) throws XelException {
		for (FunctionMapper mapper : _mappers) {
			final Function fun = mapper.resolveFunction(arg0, arg1);
			if (fun != null) return fun;
		}
		return null;
	}
}
