/* ShiftCellAction.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		2013/7/25, Created by Dennis.Chen
}}IS_NOTE

Copyright (C) 2013 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
*/
package io.keikai.ui.impl.undo;


import io.keikai.api.CellOperationUtil;
import io.keikai.api.Range;
import io.keikai.api.Ranges;
import io.keikai.api.model.Sheet;

/**
 * 
 * @author dennis
 *
 */
public class ShiftCellAction extends Abstract2DCellDataStyleAction {
	private static final long serialVersionUID = -2778906215535785051L;
	
	private final int _rowNumber;
	private final int _columnNumber;
	
	public ShiftCellAction(String label, Sheet sheet,int row, int column, int lastRow,int lastColumn,int rowNumber, int columnNumber){
		super(label,sheet,row,column,lastRow,lastColumn,sheet,row+rowNumber,column+columnNumber,lastRow+rowNumber,lastColumn+columnNumber,RESERVE_ALL);
		this._rowNumber = rowNumber;
		this._columnNumber = columnNumber;
	}
	
	@Override
	protected void applyAction(){
		Range r = Ranges.range(_sheet,_row,_column,_lastRow,_lastColumn);
		CellOperationUtil.shift(r,_rowNumber, _columnNumber);
	}
}
