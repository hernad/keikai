/*

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2014/07/25, Created by henrichen
}}IS_NOTE

Copyright (C) 2014 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/
package io.keikai.model;

/**
 * Picture data in a picture.
 * @author henrichen
 * @since 3.6.0
 */
public interface SPictureData {
	public SPicture.Format getFormat();
	
	public byte[] getData();
	
	public int getIndex();
}
