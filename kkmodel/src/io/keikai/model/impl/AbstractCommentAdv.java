/*

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/12/01 , Created by dennis
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package io.keikai.model.impl;

import java.io.Serializable;

import io.keikai.model.SBook;
import io.keikai.model.SComment;

/**
 * 
 * @author dennis
 * @since 3.5.0
 */
public abstract class AbstractCommentAdv implements SComment, Serializable{

	private static final long serialVersionUID = 1L;
	
	public abstract AbstractCommentAdv clone();
	
	//ZSS-1183
	//@since 3.9.0
	/*package*/ abstract AbstractCommentAdv cloneComment(SBook book);
}
