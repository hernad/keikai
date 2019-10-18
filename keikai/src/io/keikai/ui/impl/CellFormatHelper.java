/* CellFormatHelper.java

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Jan 29, 2008 11:14:44 AM     2008, Created by Dennis.Chen
}}IS_NOTE

Copyright (C) 2007 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under GPL Version 2.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
 */
package io.keikai.ui.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import io.keikai.model.CellRegion;
import io.keikai.model.SAutoFilter;
import io.keikai.model.SBorder;
import io.keikai.model.SCell;
import io.keikai.model.SCellStyle;
import io.keikai.model.SColor;
import io.keikai.model.SConditionalStyle;
import io.keikai.model.SDataBar;
import io.keikai.model.SFill;
import io.keikai.model.SFont;
import io.keikai.model.SHyperlink;
import io.keikai.model.SIconSet;
import io.keikai.model.SRichText;
import io.keikai.model.SSheet;
import io.keikai.model.STable;
import io.keikai.model.impl.AbstractCellStyleAdv;
import io.keikai.model.impl.AbstractSheetAdv;
import io.keikai.model.impl.AbstractTableAdv;
import io.keikai.model.impl.IconSetImpl;
import io.keikai.model.sys.EngineFactory;
import io.keikai.model.sys.format.FormatContext;
import io.keikai.model.sys.format.FormatEngine;
import io.keikai.model.sys.format.FormatResult;
import io.keikai.model.util.RichTextHelper;
import io.keikai.range.impl.StyleUtil;
import io.keikai.ui.Spreadsheet;
import org.zkoss.poi.ss.usermodel.ZssContext;
import org.zkoss.web.fn.ServletFns;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.WebApps;
import io.keikai.ui.impl.undo.ReserveUtil;

/**
 * @author Dennis.Chen
 * 
 */
public class CellFormatHelper implements Serializable{
	private static final long serialVersionUID = 8480097718738495064L;

	private static final int RESERVE_CELL_MARGIN = ReserveUtil.RESERVE_STYLE * 2; // double side 

	/**
	 * cell to get the format, could be null.
	 */
	private SCell _cell;
	
	/**
	 * cell style, could be null
	 */
	private SCellStyle _cellStyle;

	private SSheet _sheet;
	
	private int _row;

	private int _col;

	private boolean hasRightBorder_set = false;
	private boolean hasRightBorder = false;
	
	private MergeMatrixHelper _mmHelper;
	

	private FormatEngine _formatEngine;
	
	//ZSS-1142
	private SConditionalStyle _cdStyle;

	@Deprecated
	public CellFormatHelper(SSheet sheet, int row, int col, MergeMatrixHelper mmhelper) {
		this(sheet, row, col, mmhelper, null);
	}
	
	//ZSS-1142
	public CellFormatHelper(SSheet sheet, int row, int col, MergeMatrixHelper mmhelper, SConditionalStyle cdStyle) {
		_sheet = sheet;
		_row = row;
		_col = col;
		_cell = sheet.getCell(row, col);
		_cellStyle =_cell.getCellStyle();
		_mmHelper = mmhelper;
		_formatEngine = EngineFactory.getInstance().createFormatEngine();
		_cdStyle = cdStyle; //ZSS-1142
	}

	public String getHtmlStyle(StringBuffer doubleBorder, STable table, SCellStyle tbStyle) { //ZSS-977
		StringBuffer sb = new StringBuffer();

		//ZSS-34 cell background color does not show in excel
		//20110819, henrichen: if fill pattern is NONE, shall not show the cell background color
		//ZSS-857, ZSS-977: consider Table background
		SCellStyle fillStyle = StyleUtil
				.getFillStyle(_cellStyle, tbStyle, _cdStyle); //ZSS-1142
		
		String backColor = fillStyle != null ? 
			fillStyle.getBackColor().getHtmlColor() : null;
		if (backColor != null) {
			sb.append("background-color:").append(backColor).append(";");
		}

		//ZSS-841, ZSS-977:consider table style
		if (fillStyle != null)
			sb.append(((AbstractCellStyleAdv)fillStyle).getFillPatternHtml());
		
		//ZSS-568: double border is composed by this and adjacent cells
		//ZSS-977: consider table style
		//ZSS-1119: merged border could occur both on top and left sides 
		Map<String, String> mergedBorder = new HashMap<String, String>(4);
		processTopBorder(sb, doubleBorder, fillStyle, tbStyle, mergedBorder);
		processLeftBorder(sb, doubleBorder, fillStyle, tbStyle, mergedBorder);
		if (!mergedBorder.isEmpty()) {
			appendMergedBorder(sb, mergedBorder);
		}
		processBottomBorder(sb, doubleBorder, fillStyle, tbStyle);
		processRightBorder(sb, doubleBorder, fillStyle, tbStyle);

		return sb.toString();
	}
	
	private boolean processBottomBorder(StringBuffer sb, StringBuffer db, SCellStyle fillStyle, SCellStyle tbStyle) { //ZSS-977, ZSS-1142

		boolean hitBottom = false;
		MergedRect rect = null;
		boolean hitMerge = false;

		
		// ZSS-259: should apply the bottom border from the cell of merged range's bottom
		// as processRightBorder() does.
		rect = _mmHelper.getMergeRange(_row, _col);
		int bottom = _row;
		if(rect != null) {
			hitMerge = true;
			bottom = rect.getLastRow();
		}
		SCellStyle nextStyle = StyleUtil.getBottomStyle(_sheet.getCell(bottom,_col).getCellStyle(), tbStyle, _cdStyle); //ZSS-977, ZSS-1142 
		SBorder.BorderType bb = null;
		if (nextStyle != null){
			bb = nextStyle.getBorderBottom();
			String color = nextStyle.getBorderBottomColor().getHtmlColor();
			hitBottom = appendBorderStyle(sb, "bottom", bb, color);
		}

		// ZSS-259: should check and apply the top border from the bottom cell
		// of merged range's bottom as processRightBorder() does.
		SCellStyle nextFillStyle = null; //ZSS-977
		if (!hitBottom) {
			bottom = hitMerge ? rect.getLastRow() + 1 : _row + 1; 
			/*if(next == null){ // don't search into merge ranges
				//check is _row+1,_col in merge range
				MergedRect rect = _mmHelper.getMergeRange(_row+1, _col);
				if(rect !=null){
					next = _sheet.getCell(rect.getTop(),rect.getLeft());
				}
			}*/
			//ZSS-919: merge more than 2 columns; must use top border of bottom cell
			if (!hitMerge || rect.getColumn() == rect.getLastColumn()) {
				nextFillStyle = nextStyle = _sheet.getCell(bottom,_col).getCellStyle();
				SCellStyle nextTbStyle = null; //ZSS-977
				SConditionalStyle nextCdStyle = null; //ZSS-1142
				if (nextStyle.getBorderTop() == SBorder.BorderType.NONE) {
					nextCdStyle = ((AbstractSheetAdv)_sheet).getConditionalFormattingStyle(bottom, _col); //ZSS-1142
					final STable table0 = ((AbstractSheetAdv)_sheet).getTableByRowCol(bottom, _col);
					nextTbStyle = table0 == null ? null : 
						((AbstractTableAdv)table0).getCellStyle(bottom, _col);
					nextStyle = StyleUtil.getTopStyle(nextStyle, nextTbStyle, nextCdStyle); //ZSS-1142
				}

				if (nextStyle != null){
					bb = nextStyle.getBorderTop();// get top border of
					String color = nextStyle.getBorderTopColor().getHtmlColor();
					// set next row top border as cell's bottom border;
					hitBottom = appendBorderStyle(sb, "bottom", bb, color);
				}
				
				//ZSS-977
				if (!hitBottom) {
					nextFillStyle = StyleUtil.getFillStyle(nextFillStyle, nextTbStyle, nextCdStyle); //ZSS-1142
				}
			}
		}
		
		//border depends on next cell's fill color if solid pattern
		if(!hitBottom && nextFillStyle !=null){
			//String bgColor = BookHelper.indexToRGB(_book, style.getFillForegroundColor());
			//ZSS-34 cell background color does not show in excel
			String bgColor = nextFillStyle.getFillPattern() == SFill.FillPattern.SOLID ?
					nextFillStyle.getBackColor().getHtmlColor() : null; //ZSS-857
			if (bgColor != null) {
				hitBottom = appendBorderStyle(sb, "bottom", SBorder.BorderType.THIN, bgColor);
			} else if (nextFillStyle.getFillPattern() != SFill.FillPattern.NONE) { //ZSS-841
				sb.append("border-bottom:none;"); // no grid line either
				hitBottom = true;
			}
		}
		
		//border depends on current cell's background color
		if(!hitBottom && fillStyle !=null){
			//String bgColor = BookHelper.indexToRGB(_book, style.getFillForegroundColor());
			//ZSS-34 cell background color does not show in excel
			String bgColor = fillStyle.getFillPattern() == SFill.FillPattern.SOLID ?
					fillStyle.getBackColor().getHtmlColor() : null;
			if (bgColor != null) {
				hitBottom = appendBorderStyle(sb, "bottom", SBorder.BorderType.THIN, bgColor);
			} else if (fillStyle.getFillPattern() != SFill.FillPattern.NONE) { //ZSS-841
				sb.append("border-bottom:none;"); // no grid line either
				hitBottom = true;
			}
		}
		db.append(hitBottom && bb == SBorder.BorderType.DOUBLE ? "b" : "_");
		
		return hitBottom;
	}

	private boolean processRightBorder(StringBuffer sb, StringBuffer db, SCellStyle fillStyle, SCellStyle tbStyle) { //ZSS-977, ZSS-1142
		boolean hitRight = false;
		MergedRect rect=null;
		boolean hitMerge = false;
		//find right border of target cell 
		rect = _mmHelper.getMergeRange(_row, _col);
		int right = _col;
		if(rect!=null){
			hitMerge = true;
			right = rect.getLastColumn();
		}
		SBorder.BorderType bb = null;
		SCellStyle nextStyle = StyleUtil.getRightStyle(_sheet.getCell(_row, right).getCellStyle(), tbStyle, _cdStyle); //ZSS-977, ZSS-1142
		if (nextStyle != null){
			bb = nextStyle.getBorderRight();
			String color = nextStyle.getBorderRightColor().getHtmlColor();
			hitRight = appendBorderStyle(sb, "right", bb, color);
		}
		
		//if no border for target cell,then check is this cell in a merge range
		//if(true) then try to get next cell after this merge range
		//else get next cell of this cell
		SCellStyle nextFillStyle = null; //ZSS-977
		if(!hitRight){
			right = hitMerge?rect.getLastColumn()+1:_col+1;
			//ZSS-919: merge more than 2 rows; must use left border of right cell
			if (!hitMerge || rect.getRow() == rect.getLastRow()) {  
				nextFillStyle = nextStyle = _sheet.getCell(_row,right).getCellStyle();
				//ZSS-977
				SCellStyle nextTbStyle = null;
				SConditionalStyle nextCdStyle = null; //ZSS-1142
				if (nextStyle.getBorderLeft() == SBorder.BorderType.NONE) {
					nextCdStyle = ((AbstractSheetAdv)_sheet).getConditionalFormattingStyle(_row, right); //ZSS-1142
					final STable table0 = ((AbstractSheetAdv)_sheet).getTableByRowCol(_row, right);
					nextTbStyle = table0 == null ? null : 
						((AbstractTableAdv)table0).getCellStyle(_row, right);
					nextStyle = StyleUtil.getLeftStyle(nextStyle, nextTbStyle, nextCdStyle); //ZSS-1142
				}
				
				if (nextStyle != null){
					bb = nextStyle.getBorderLeft();//get left here
					//String color = BookHelper.indexToRGB(_book, style.getLeftBorderColor());
					// ZSS-34 cell background color does not show in excel
					String color = nextStyle.getBorderLeftColor().getHtmlColor();
					hitRight = appendBorderStyle(sb, "right", bb, color);
				}
				
				//ZSS-977
				if (!hitRight) {
					nextFillStyle = StyleUtil.getFillStyle(nextFillStyle, nextTbStyle, nextCdStyle); //ZSS-1142
				}
			}
		}

		//border depends on next cell's background color (why? dennis, 20131118)
		if(!hitRight && nextFillStyle !=null){
			//String bgColor = BookHelper.indexToRGB(_book, style.getFillForegroundColor());
			//ZSS-34 cell background color does not show in excel
			String bgColor = nextFillStyle.getFillPattern() == SFill.FillPattern.SOLID ?
					nextFillStyle.getBackColor().getHtmlColor() : null;
			if (bgColor != null) {
				hitRight = appendBorderStyle(sb, "right", SBorder.BorderType.THIN, bgColor);
			} else if (nextFillStyle.getFillPattern() != SFill.FillPattern.NONE) { //ZSS-841
				sb.append("border-right:none;"); // no grid line either
				hitRight = true;
			}
		}
		//border depends on current cell's background color
		if(!hitRight && fillStyle !=null){
			//String bgColor = BookHelper.indexToRGB(_book, style.getFillForegroundColor());
			//ZSS-34 cell background color does not show in excel
			String bgColor = fillStyle.getFillPattern() == SFill.FillPattern.SOLID ?
					fillStyle.getBackColor().getHtmlColor() : null;
			if (bgColor != null) {
				hitRight = appendBorderStyle(sb, "right", SBorder.BorderType.THIN, bgColor);
			} else if (fillStyle.getFillPattern() != SFill.FillPattern.NONE) { //ZSS-841
				sb.append("border-right:none;"); // no grid line either
				hitRight = true;
			}
		}

		db.append(hitRight && bb == SBorder.BorderType.DOUBLE ? "r" : "_");
		
		return hitRight;
	}

	private boolean appendBorderStyle(StringBuffer sb, String locate, SBorder.BorderType bs, String color) {
		if (bs == SBorder.BorderType.NONE)
			return false;
		
		sb.append("border-").append(locate).append(":");
		switch(bs) {
		case DASHED:
		case DOTTED:
			sb.append("dashed");
			break;
		case HAIR:
			sb.append("dotted");
			break;
		case DOUBLE:
			sb.append("none");
			break;
		default:
			sb.append("solid");
		}
		sb.append(" 1px");

		if (color != null) {
			sb.append(" ");
			sb.append(color);
		}

		sb.append(";");
		return true;
	}
	
	public static String getFontCSSStyle(SCell cell, SFont font) {
		final StringBuilder sb = new StringBuilder();
		
		String fontName = font.getName();
		if (fontName != null) {
			sb.append("font-family:").append(fontName).append(";");
		}
		
		String textColor = font.getColor().getHtmlColor();

		if (textColor != null) {
			sb.append("color:").append(textColor).append(";");
		}

		final SFont.Underline fontUnderline = font.getUnderline();
		final boolean strikeThrough = font.isStrikeout();
		boolean isUnderline = fontUnderline == SFont.Underline.SINGLE || fontUnderline == SFont.Underline.SINGLE_ACCOUNTING;
		if (strikeThrough || isUnderline) {
			sb.append("text-decoration:");
			if (strikeThrough)
				sb.append(" line-through");
			if (isUnderline)	
				sb.append(" underline");
			sb.append(";");
		} else {
			sb.append("text-decoration:none;"); //ZSS-1018
		}

		final SFont.Boldweight weight = font.getBoldweight();
		final boolean italic = font.isItalic();
		sb.append("font-weight:").append(weight).append(";");
		if (italic)
			sb.append("font-style:").append("italic;");

		//ZSS-748
		int fontSize = font.getHeightPoints();
		if (font.getTypeOffset() != SFont.TypeOffset.NONE) {
			fontSize = (int) (0.7 * fontSize + 0.5) ;
		}
		sb.append("font-size:").append(fontSize).append("pt;");
		
		//ZSS-748
		if (font.getTypeOffset() == SFont.TypeOffset.SUPER)
			sb.append("vertical-align:").append("super;");
		else if (font.getTypeOffset() == SFont.TypeOffset.SUB)
			sb.append("vertical-align:").append("sub;");
		
		//ZSS-1150:, 20151117, henrichen: in IE9, the "_" will be clipped 
		//  and not seen because in cell-cave, the style.display is set to
		//  table-cell and IE9 will offset cell-cave 2px down for unknown 
		//  reason. We are forced to use another way to do bottom 
		//  vertical-align here for IE9 case.
		SCellStyle cellStyle = cell.getCellStyle();
		if (cellStyle != null) {
			SCellStyle.VerticalAlignment verticalAlignment = cellStyle.getVerticalAlignment();
			if (isIE9VerticalAligment(verticalAlignment)
				&& (verticalAlignment == null || verticalAlignment == SCellStyle.VerticalAlignment.BOTTOM)) {
				sb.append("position:absolute;bottom:0px;padding-right:4px;box-sizing:border-box;");
			}
		}
		
		return sb.toString();
	}

	//ZSS-1150: "_" not seen when bottom alignment
	private static boolean isIE9VerticalAligment(
			SCellStyle.VerticalAlignment verticalAlignment) {
		return Spreadsheet
				.isIE9() && verticalAlignment != SCellStyle.VerticalAlignment.CENTER;
	}
	
	//style in element with id="xxx-cave" or class=".zsscelltxt"
	public String getInnerHtmlStyle() { 
		if (!_cell.isNull()) {
			final StringBuffer sb = new StringBuffer();
			sb.append(getTextCSSStyle( _cell));
			
			//vertical alignment
			SCellStyle.VerticalAlignment verticalAlignment = _cellStyle.getVerticalAlignment();
			if (!isIE9VerticalAligment(verticalAlignment)) { //ZSS-1150
				sb.append("display: table-cell;");
				switch (verticalAlignment) {
				case TOP:
					sb.append("vertical-align: top;");
					break;
				case CENTER:
					sb.append("vertical-align: middle;");
					break;
				case BOTTOM:
				default:
					sb.append("vertical-align: bottom;");
					break;
				}
			}
			
			//ZSS-1142
			if (_cdStyle != null) {
				final SDataBar dataBar = _cdStyle.getDataBar();
				final SIconSet iconSet = _cdStyle.getIconSet();
				if (dataBar != null) {
					final Double barPercent = _cdStyle.getBarPercent();
					if (barPercent != null) {
						sb.append("background-repeat:no-repeat;")
//20160630, henrichen: Was using .dbar::before to draw the data bar with border;
//unfortunally, it will block/cover the cell text; give up the .dbar::before way. 
//							.append("background-position:-20000px -20000px;")
							.append("background-position:0px 1px;")
							.append("background-size: ").append(barPercent).append("% calc(100% - 4px);")
							.append("background-image: linear-gradient(to right, ");
						final String htmlColor = dataBar.getColor().getHtmlColor(); 
						sb.append(htmlColor).append(", #e5ecf5);");
//						final String borderColor = htmlColor;
//						sb.append("border: none 1px ").append(borderColor).append(";");
//						final double rightPercent = 100.0 - barPercent.doubleValue();
//						sb.append("right: ").append(rightPercent).append("%;");
					}
				} else if (iconSet != null) {
					final SIconSet.IconSetType type = iconSet.getType();
					final String iconSetName = type != null ? type.name : null;
					final Integer iconSetId = _cdStyle.getIconSetId();
					final WebApp app = WebApps.getCurrent();
					if (iconSetId != null && app != null && iconSetName != null) {
						final String name = IconSetImpl
								.getIconSetName(iconSetName, iconSetId, iconSet.isReverse());
						if (name != null) {
							String path;
							try {
								path = ServletFns.encodeURL("~./zss/img/"+name+".png");
								sb.append("background-repeat:no-repeat;")
								.append("background-position:0px 2px;")
								.append("background-image: url(").append(path).append(");");
							} catch (ServletException e) {
								// ignore.
							}
						}
					}
				}
			}

			return sb.toString();
		}
		return "";
	}
	
	// ZSS-725: separate inner and font style to avoid the conflict between
	// vertical alignment, subscript and superscript.
	public String getFontHtmlStyle() {
		if (!_cell.isNull()) {
			
			final StringBuffer sb = new StringBuffer();
			final SFont font = _cellStyle.getFont();
			
			//sb.append(BookHelper.getFontCSSStyle(_book, font));
			sb.append(getFontCSSStyle(_cell, font));

			//condition color
			final FormatResult ft = _formatEngine.format(_cell, new FormatContext(ZssContext.getCurrent().getLocale()));
			final boolean isRichText = ft.isRichText();
			if (!isRichText) {
				final SColor color = ft.getColor();
				if(color!=null){
					final String htmlColor = color.getHtmlColor();
					sb.append("color:").append(htmlColor).append(";");
				}
			}

			return sb.toString();
		}
		return "";
	}
	
	/* given alignment and cell type, return real alignment */
	//Halignment determined by style alignment, text format and value type  
	public static SCellStyle.Alignment getRealAlignment(SCell cell) {
		final SCellStyle style = cell.getCellStyle();
		SCell.CellType type = cell.getType();
		SCellStyle.Alignment align = style.getAlignment();
		if (align == SCellStyle.Alignment.GENERAL) {
			//ZSS-918: vertical text default to horizontal center; no matter the type 
			final boolean vtxt = style.getRotation() == 255; 
			if (vtxt) return SCellStyle.Alignment.CENTER;
			
			//ZSS-1020: 90 degree text default to horizontal right; no matter the type
			final boolean deg90 = style.getRotation() == 90;
			if (deg90) return SCellStyle.Alignment.RIGHT;
			
			final String format = style.getDataFormat();
			if (format != null && format.startsWith("@")) //a text format
				type = SCell.CellType.STRING;
			else if (type == SCell.CellType.FORMULA)
				type = cell.getFormulaResultType();
			switch(type) {
			case BLANK:
				return align;
			case BOOLEAN:
				return SCellStyle.Alignment.CENTER;
			case ERROR:
				return SCellStyle.Alignment.CENTER;
			case NUMBER:
				return SCellStyle.Alignment.RIGHT;
			case STRING:
			default:
				return SCellStyle.Alignment.LEFT;
			}
		}
		return align;
	}
	
	public static String getTextCSSStyle(SCell cell) {
		final SCellStyle style = cell.getCellStyle();

		final StringBuffer sb = new StringBuffer();
		SCellStyle.Alignment textHAlign = getRealAlignment(cell);
		
		switch(textHAlign) {
		case RIGHT:
			sb.append("text-align:").append("right").append(";");
			break;
		case CENTER:
		case CENTER_SELECTION:
			sb.append("text-align:").append("center").append(";");
			break;
		default:
			break;
		}
		//ZSS-944: when rotate 90 degree, wrap must be false
		final int rotate90 = style.getRotation();
		boolean textWrap = style.isWrapText() && rotate90 != 90 && rotate90 != -90; //ZSS-1020
		if (textWrap) {
			sb.append("white-space:").append("pre-wrap").append(";"); //ZSS-1118
		}/*else{ sb.append("white-space:").append("nowrap").append(";"); }*/

		return sb.toString();
	}

	//ZSS-977
	@Deprecated
	public boolean hasRightBorder() {
		return hasRightBorder(null, null);
	}
	//ZSS-977
	//@since 3.8.0
	public boolean hasRightBorder(STable table, SCellStyle tbStyle) {
		if(hasRightBorder_set){
			return hasRightBorder;
		}else{
			SCellStyle fillStyle = StyleUtil.getFillStyle(_cellStyle, tbStyle, _cdStyle); //ZSS-977, ZSS-1142
			hasRightBorder = processRightBorder(new StringBuffer(), new StringBuffer(), fillStyle, tbStyle); //ZSS-1142
			hasRightBorder_set = true;
		}
		return hasRightBorder;
	}
	
	public String getCellFormattedText(){
		final FormatResult ft = _formatEngine.format(_cell, new FormatContext(ZssContext.getCurrent().getLocale()));
		return ft.getText();
	}
	
	public String getCellEditText(){
		return _formatEngine.getEditText(_cell, new FormatContext(ZssContext.getCurrent().getLocale()));
	}
	
	/**
	 * Gets Cell text by given row and column, it handling
	 */
	static public String getRichCellHtmlText(SSheet sheet, int row,int column){
		final SCell cell = sheet.getCell(row, column);
		String text = "";
		if (!cell.isNull()) {
			final SCellStyle style = cell.getCellStyle();
			boolean wrap = style.isWrapText();
			boolean vtxt = style.getRotation() == 255; //ZSS-918
			
			final FormatResult ft = EngineFactory.getInstance().createFormatEngine().format(cell, new FormatContext(ZssContext.getCurrent().getLocale()));
			if (ft.isRichText()) {
				final SRichText rstr = ft.getRichText();
				text = vtxt ? getVRichTextHtml(cell, rstr, wrap) : getRichTextHtml(cell, rstr, wrap); //ZSS-918, ZSS-1138
			} else {
				text = vtxt ? escapeVText(ft.getText(), wrap) : escapeText(ft.getText(), wrap, true); //ZSS-918
			}
			final SHyperlink hlink = cell.getHyperlink();
			if (hlink != null) {
				text = getHyperlinkHtml(text, hlink, sheet, cell, style, ft, null, null); //ZSS-1018, ZSS-1142
			}				
		}
		return text;
	}
	
	// ZSS-725
	static public String getRichTextEditCellHtml(SSheet sheet, int row,int column){
		final SCell cell = sheet.getCell(row, column);
		String text = "";
		if (!cell.isNull()) {
			boolean wrap = cell.getCellStyle().isWrapText();
			
			final FormatResult ft = EngineFactory.getInstance().createFormatEngine().format(cell, new FormatContext(ZssContext.getCurrent().getLocale()));
			if (ft.isRichText()) {
				final SRichText rstr = ft.getRichText();
				text = RichTextHelper.getCellRichTextHtml(cell, rstr, wrap); //ZSS-1138

			} else {
				text = RichTextHelper.getFontTextHtml(escapeText(ft.getText(), wrap, true), cell.getCellStyle().getFont());
			}
		}
		return text;
	}
	
	private static String getHyperlinkHtml(String label, SHyperlink link, 
			SSheet sheet, SCell cell, SCellStyle cellStyle, FormatResult ft, SCellStyle tbStyle, SConditionalStyle cdStyle) { //ZSS-1018, ZSS-1142
		String addr = escapeText(link.getAddress()==null?"":link.getAddress(), false, false); //TODO escape something?
		if (label == null) {
			label = escapeText(link.getLabel(), false, false);
		}
		if( label == null) {
			label = escapeText(addr, false, false);
		}
		final StringBuffer sb  = new StringBuffer();
		//ZSS-233, don't use href directly to avoid direct click on spreadsheet at the beginning.
		sb.append("<a zs.t=\"SHyperlink\" z.t=\"").append(link.getType().getValue()).append("\" href=\"javascript:\" z.href=\"")
			.append(addr)
			.append("\" style=\"") //ZSS-1018
			.append(getFontHtmlStyle(sheet, cell, cellStyle, ft, tbStyle, cdStyle)) //ZSS-1018, ZSS-1142
			.append("\">")
			.append(label==null?"":label)
			.append("</a>");
		return sb.toString();		
	}
	
	//ZSS-1138
	private static String getRichTextHtml(SCell cell, SRichText text, boolean wrap) {
		return RichTextHelper.getCellRichTextHtml(cell, text, wrap);
	}
	
	
	/**
	 * Gets Cell text by given row and column
	 */
	static public String getCellHtmlText(SSheet sheet, int row,int column){
		final SCell cell = sheet.getCell(row, column);
		String text = "";
		if (cell != null) {
			boolean wrap = cell.getCellStyle().isWrapText();
			
			final FormatResult ft = EngineFactory.getInstance().createFormatEngine().format(cell, new FormatContext(ZssContext.getCurrent().getLocale()));
			if (ft.isRichText()) {
				final SRichText rstr = ft.getRichText();
				text = rstr.getText();
			} else {
				text = ft.getText();
			}
			text = escapeText(text, wrap, true);
		}
		return text;
	}
	
	private static String escapeText(String text, boolean wrap, boolean multiline) {
		return RichTextHelper.escapeText(text, wrap, multiline);
	}

	//ZSS-568
	private boolean processTopBorder(StringBuffer sb, StringBuffer db, SCellStyle fillStyle, SCellStyle tbStyle, Map<String, String> mergedBorder) { //ZSS-977

		boolean hitTop = false;
		MergedRect rect = null;
		boolean hitMerge = false;

		
		// ZSS-259: should apply the top border from the cell of merged range's top
		// as processRightBorder() does.
		rect = _mmHelper.getMergeRange(_row, _col);
		int top = _row;
		if(rect != null) {
			hitMerge = true;
			top = rect.getRow();
		}
		SCellStyle nextStyle = StyleUtil.getTopStyle(_sheet.getCell(top,_col).getCellStyle(), tbStyle, _cdStyle); //ZSS-977, ZSS-1142
		
		if (nextStyle != null){
			SBorder.BorderType bb = nextStyle.getBorderTop();
			if (bb == SBorder.BorderType.DOUBLE) {
				String color = nextStyle.getBorderTopColor().getHtmlColor();
				hitTop = appendBorderStyle(sb, "top", bb, color);
			} else if (bb != SBorder.BorderType.NONE) {
				//ZSS-919: check if my top is a merged cell 
				top = hitMerge ? rect.getRow() - 1 : _row - 1;
				if (!hitMerge || _row == rect.getRow() && _col == rect.getColumn()) { //ZSS-1119, single cell or mareger cell only
					if (top >= 0) {
						final MergedRect rectT = _mmHelper.getMergeRange(top, _col);
						//my top merge more than 2 columns
						if (rectT != null && rectT.getColumn() < rectT.getLastColumn()) {
							db.append('_');
	
							String color = nextStyle.getBorderTopColor().getHtmlColor();
							
							//support only solid line but position correctly
							mergedBorder.put("top", color);
							return true;
						}
					}
				
					//ZSS-1119: a merger cell; check its right mergee top border
					if (hitMerge) {
						int right = _col + 1;
						SCellStyle rightStyle = StyleUtil.getTopStyle(_sheet.getCell(_row, right).getCellStyle(), tbStyle, _cdStyle); //ZSS-1142
						if (rightStyle != null && rightStyle.getBorderTop() != SBorder.BorderType.NONE) {
							db.append('_');
							
							String color = nextStyle.getBorderTopColor().getHtmlColor();
							//support only solid line but position correctly
							mergedBorder.put("top", color);
							return true;
						}
					}
				}
			}
		}
		

		// ZSS-259: should check and apply the bottom border from the top cell
		// of merged range's top as processRightBorder() does.
		if (!hitTop) {
			top = hitMerge ? rect.getRow() - 1 : _row - 1;
			if (top >= 0) {
				nextStyle = _sheet.getCell(top,_col).getCellStyle();
				//ZSS-977
				if (nextStyle.getBorderBottom() == SBorder.BorderType.NONE) {
					final STable table0 = ((AbstractSheetAdv)_sheet).getTableByRowCol(top, _col);
					final SCellStyle tbStyle0 = 
							table0 == null ? null : ((AbstractTableAdv)table0).getCellStyle(top, _col);
					final SConditionalStyle cdStyle0 = ((AbstractSheetAdv)_sheet).getConditionalFormattingStyle(top, _col); //ZSS-1142
					nextStyle = StyleUtil.getBottomStyle(nextStyle, tbStyle0, cdStyle0); //ZSS-1142
				}
				
				if (nextStyle != null){
					SBorder.BorderType bb = nextStyle.getBorderBottom();// get bottom border of
					if (bb == SBorder.BorderType.DOUBLE) {
						String color = nextStyle.getBorderBottomColor().getHtmlColor();
						// set next row top border as cell's top border;
						hitTop = appendBorderStyle(sb, "top", bb, color);
					}
				}
			}
		}
		
		db.append(hitTop ? "t" : "_");
		return hitTop;
	}

	private boolean processLeftBorder(StringBuffer sb, StringBuffer db, SCellStyle fillStyle, SCellStyle tbStyle, Map<String, String> mergedBorder) { //ZSS-977,ZSS-1119, ZSS-1142
		boolean hitLeft = false;
		MergedRect rect=null;
		boolean hitMerge = false;
		//find left border of target cell 
		rect = _mmHelper.getMergeRange(_row, _col);
		int left = _col;
		if(rect!=null){
			hitMerge = true;
			left = rect.getColumn();
		}
		SCellStyle nextStyle = StyleUtil.getLeftStyle(_sheet.getCell(_row,left).getCellStyle(), tbStyle, _cdStyle); //ZSS-977, ZSS-1142
		if (nextStyle != null){
			SBorder.BorderType bb = nextStyle.getBorderLeft();
			if (bb == SBorder.BorderType.DOUBLE) {
				String color = nextStyle.getBorderLeftColor().getHtmlColor();
				hitLeft = appendBorderStyle(sb, "left", bb, color);
			} else if (bb != SBorder.BorderType.NONE) {
				//ZSS-919: check if my left is a merged cell 
				left = hitMerge?rect.getColumn()-1:_col-1;
				if (left >= 0) { //single cell, merger cell, or mergee cell
					final MergedRect rectT = _mmHelper.getMergeRange(_row, left);
					//my left merged more than 2 rows
					if (rectT != null && rectT.getRow() < rectT.getLastRow()) {
						db.append('_');

						String color = nextStyle.getBorderLeftColor().getHtmlColor();
						//support only solid line but position correctly
						mergedBorder.put("left", color); //ZSS-1119
						return true;
					}
				}
			}
		}

		
		//if no border for target cell,then check if this cell is in a merge range
		//if(true) then try to get next cell after this merge range
		//else get next cell of this cell
		if(!hitLeft){
			left = hitMerge?rect.getColumn()-1:_col-1;
			if (left >= 0) {
				nextStyle = _sheet.getCell(_row,left).getCellStyle();
				//ZSS-977
				if (nextStyle.getBorderRight() == SBorder.BorderType.NONE) {
					final SConditionalStyle cdStyle0 = ((AbstractSheetAdv)_sheet).getConditionalFormattingStyle(_row, left); //ZSS-1142
					final STable table0 = ((AbstractSheetAdv)_sheet).getTableByRowCol(_row, left);
					final SCellStyle tbStyle0 = 
							table0 == null ? null : ((AbstractTableAdv)table0).getCellStyle(_row, left);
					nextStyle = StyleUtil.getRightStyle(nextStyle, tbStyle0, cdStyle0); //ZSS-1142
				}
				if (nextStyle != null){
					SBorder.BorderType bb = nextStyle.getBorderRight();//get right here
					//String color = BookHelper.indexToRGB(_book, style.getLeftBorderColor());
					// ZSS-34 cell background color does not show in excel
					if (bb == SBorder.BorderType.DOUBLE) {
						String color = nextStyle.getBorderRightColor().getHtmlColor();
						hitLeft = appendBorderStyle(sb, "left", bb, color);
					}
				}
			}
		}
		
		db.append(hitLeft ? "l" : "_");
		return hitLeft;
	}

	//ZSS-918
	private static String escapeVText(String text, boolean wrap) {
		return RichTextHelper.escapeVText(text, wrap);
	}
	
	//ZSS-918, ZSS-1138
	private static String getVRichTextHtml(SCell cell, SRichText rstr, boolean wrap) {
		return RichTextHelper.getCellVRichTextHtml(cell, rstr, wrap);
	}

	//ZSS-919, ZSS-1119: handle when has to deal with both top and left borders 
	private boolean appendMergedBorder(StringBuffer sb, Map<String, String> mergedBorder) {
		String topColor = mergedBorder.get("top");
		String topPx = topColor != null ? "-1px" : "0px";
		String leftColor = mergedBorder.get("left");
		String leftPx = leftColor != null ? "-1px" : "0px";
		
		sb.append("box-shadow:").append(leftPx).append(" ").append(topPx).append(" ").append(topColor != null ? topColor : leftColor).append(";");
		return true;
	}
	
	//20150303, henrichen: The fix for ZSS-945 is super dirty!
	//ZSS-945
	//@since 3.8.0
	//@Internal
	public FormatResult getFormatResult() {
		return _cell == null ? null : _formatEngine.format(_cell, new FormatContext(ZssContext.getCurrent().getLocale()));
	}
	
	//ZSS-945
	//@since 3.8.0
	//@Internal
	public String getCellFormattedText(FormatResult ft) {
		return ft == null ? "" : ft.getText();
	}
	
	//ZSS-945, ZSS-1018
	//@since 3.8.0
	//@Internal
	public static String getFontHtmlStyle(SSheet sheet, SCell cell, SCellStyle cellStyle, FormatResult ft, SCellStyle tbCellStyle, SConditionalStyle cdStyle) { //ZSS-977, ZSS-1142
		if (!cell.isNull()) {
			final StringBuffer sb = new StringBuffer();
			//ZSS-977
			SFont font = StyleUtil.getFontStyle(sheet.getBook(), cellStyle, tbCellStyle, cdStyle); //ZSS-1142
			sb.append(getFontCSSStyle(cell, font));

			//condition color
			final boolean isRichText = ft.isRichText();
			if (!isRichText) {
				final SColor color = ft.getColor();
				if(color!=null){
					final String htmlColor = color.getHtmlColor();
					sb.append("color:").append(htmlColor).append(";");
				}
			}

			return sb.toString();
		}
		return "";
	}
	
	//ZSS-945
	//@since 3.8.0
	//@Internal
	/**
	 * Gets Cell text by given row and column, it handling
	 */
	static public String getRichCellHtmlText(SSheet sheet, int row,int column, FormatResult ft, SCellStyle tbStyle, SConditionalStyle cdStyle){ //ZSS-1018, ZSS-1142
		final SCell cell = sheet.getCell(row, column);
		String text = "";
		if (!cell.isNull()) {
			final SCellStyle style = cell.getCellStyle(); 
			boolean wrap = style.isWrapText();
			boolean vtxt = style.getRotation() == 255; //ZSS-918
			
			if (ft.isRichText()) {
				final SRichText rstr = ft.getRichText();
				text = vtxt ? getVRichTextHtml(cell, rstr, wrap) : getRichTextHtml(cell, rstr, wrap); //ZSS-918, ZSS-1138
			} else {
				text = vtxt ? escapeVText(ft.getText(), wrap) : escapeText(ft.getText(), wrap, true); //ZSS-918
			}
			final SHyperlink hlink = cell.getHyperlink();
			if (hlink != null) {
				text = getHyperlinkHtml(text, hlink, sheet, cell, style, ft, tbStyle, cdStyle); //ZSS-1018, ZSS-1142
			}				
		}
		return text;
	}
	
	//ZSS-945
	//@since 3.8.0
	//@Internal
	/**
	 * Gets Cell text by given row and column
	 */
	static public String getCellHtmlText(SSheet sheet, int row,int column, FormatResult ft, SCellStyle tbStyle, SConditionalStyle cdStyle){ //ZSS-1018, ZSS-1142
		final SCell cell = sheet.getCell(row, column);
		String text = "";
		if (cell != null) {
			boolean wrap = cell.getCellStyle().isWrapText();
			
			if (ft.isRichText()) {
				final SRichText rstr = ft.getRichText();
				text = rstr.getText();
			} else {
				text = ft.getText();
			}
			text = escapeText(text, wrap, true);
		}
		return text;
	}
	
	//@since 3.8.0
	public String getRealHtmlStyle(FormatResult ft, SCellStyle tbCellStyle) { //ZSS-977
		if (!_cell.isNull()) {
			final StringBuffer sb = new StringBuffer();
			sb.append(getFontHtmlStyle(_sheet, _cell, _cell.getCellStyle(), ft, tbCellStyle, _cdStyle)); //ZSS-977, ZSS-1018, ZSS-1142
			sb.append(getIndentCSSStyle(_cell));
			sb.append(getMergedMaxHeightStyle(_cell)); //ZSS-1199
			return sb.toString();
		}
		
		return "";
	}
	
	private String getIndentCSSStyle(SCell cell) {
		final int indention = _cell.getCellStyle().getIndention();
		final boolean wrap = _cell.getCellStyle().isWrapText();
		if(indention > 0) {
			if(wrap) {
				//ZSS-1016
				return "float:right; width: " + 
					(_sheet.getColumn(_cell.getColumnIndex()).getWidth() - (indention * 8.5) - RESERVE_CELL_MARGIN) + "px;";
			} else 
				return "text-indent:" + (indention * 8.5) + "px;";
		}
		return "";
	}

	//@since 3.8.3
	//ZSS-1199
	private String getMergedMaxHeightStyle(SCell cell) {
		MergedRect rect = _mmHelper.getMergeRange(cell.getRowIndex(), cell.getColumnIndex());
		if(rect != null) {
			return "max-height: none;";
		}
		return "";
	}
	
	//ZSS-901
	public String getAutoFilterBorder() {

		StringBuffer sb = new StringBuffer();

		final SAutoFilter filter = _sheet.getAutoFilter();
		if (filter == null) return "____"; //empty
		
		// must check in top/left/bottom/right order
		final CellRegion rgn = filter.getRegion();
		final int t = rgn.getRow();
		final int l = rgn.getColumn();
		final int b = rgn.getLastRow();
		final int r = rgn.getLastColumn();
		
		final int r0 = _cell.getRowIndex();
		final int c0 = _cell.getColumnIndex(); 
		sb.append(r0 == t && l <= c0 && c0 <= r ? "t" : "_");
		sb.append(c0 == l && t <= r0 && r0 <= b? "l" : "_");
		sb.append(r0 == b && l <= c0 && c0 <= r ? "b" : "_");
		sb.append(c0 == r && t <= r0 && r0 <= b ? "r" : "_");
		return sb.toString();
	}
	
	//ZSS-1142
	public boolean withDataBarBorder() {
		if (_cdStyle != null) {
			final SDataBar dataBar = _cdStyle.getDataBar();
			if (dataBar != null) {
				final Double barPercent = _cdStyle.getBarPercent();
				if (barPercent != null && barPercent > 0.0) {
					return true;
				}
			}
		}
		return false;
	}
}
