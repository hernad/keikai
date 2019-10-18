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
package io.keikai.model;
/**
 * The text in the footer.
 * @author Dennis
 * @since 3.5.0
 */
public interface SFooter {

	public String getLeftText();
	public void setLeftText(String text);
	public String getCenterText();
	public void setCenterText(String text);
	public String getRightText();
	public void setRightText(String text);
	
}
