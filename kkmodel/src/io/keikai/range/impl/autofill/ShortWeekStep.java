/* FullWeekStep.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Mar 29, 2011 2:37:53 PM, Created by henrichen
}}IS_NOTE

Copyright (C) 2011 Potix Corporation. All Rights Reserved.
*/


package io.keikai.range.impl.autofill;

import java.io.Serializable;
import java.util.Locale;

import io.keikai.model.SCell;

/**
 * Full week Step.
 * @author henrichen
 *
 */
/*package*/ class ShortWeekStep implements Step, Serializable {
	private static final long serialVersionUID = 2717185483097352450L;
	
	private final CircularStep _innerStep;
	private final int _type;
	public ShortWeekStep(int initial, int step, int type, int datatype, Locale locale) { //ZSS-69
		_innerStep = new CircularStep(initial, step, ShortWeekData.getInstance(type, locale));
		_type = datatype;
	}
	@Override
	public int getDataType() {
		return _type;
	}
	@Override
	public Object next(SCell srcCell) {
		return _innerStep.next(srcCell);
	}
}
