/*

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package io.keikai.model.sys.formula;

import io.keikai.model.SBook;
import org.zkoss.xel.FunctionMapper;
import org.zkoss.xel.VariableResolver;

/**
 * 
 * @author Dennis
 * @since 3.5.0
 */
public interface EvaluationContributor {

	public FunctionMapper getFunctionMaper(SBook book);
	
	public VariableResolver getVariableResolver(SBook book);
}
