/*
{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2014/8/5, Created by henri
}}IS_NOTE

Copyright (C) 2014 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
*/

package io.keikai.api.model.impl;

import java.io.Serializable;

import io.keikai.api.model.Font;
import io.keikai.api.model.RichText;

/**
 * @author henri
 * @since 3.6.0
 */
class SegmentImpl implements RichText.Segment, Serializable {
	private static final long serialVersionUID = 6796093491667252184L;
	
	final private String text;
	final private Font font;
	
	SegmentImpl(String text, Font font) {
		this.text = text;
		this.font = font;
	}
	
	@Override
	public String getText() {
		return text;
	}

	@Override
	public Font getFont() {
		return font;
	}
}