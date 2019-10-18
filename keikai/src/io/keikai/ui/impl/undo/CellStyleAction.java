/* CellStyleAction.java

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
public class CellStyleAction extends AbstractCellDataStyleAction {
	private static final long serialVersionUID = 4837162823526309185L;
	
	private final CellOperationUtil.CellStyleApplier _styleApplier;
	
	
	public CellStyleAction(String label, Sheet sheet,int row, int column, int lastRow,int lastColumn,
			CellOperationUtil.CellStyleApplier styleApplier){
		super(label,sheet,row,column,lastRow,lastColumn,RESERVE_STYLE);
		this._styleApplier = styleApplier;
	}
	protected void applyAction(){
		Range r = Ranges.range(_sheet,_row,_column,_lastRow,_lastColumn);
		CellOperationUtil.applyCellStyle(r, _styleApplier);
	}
}
