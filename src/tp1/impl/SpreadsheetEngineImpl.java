package tp1.impl;


import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.gembox.spreadsheet.ExcelCell;
import com.gembox.spreadsheet.ExcelFile;
import com.gembox.spreadsheet.ExcelWorksheet;
import com.gembox.spreadsheet.SpreadsheetInfo;

import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.engine.SpreadsheetEngine;
import tp1.api.service.util.Result;
import tp1.util.CellRange;


/**
 Example of use:
 Spreadsheet sheet = ...
 String[][] values = SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(new AbstractSpreadsheet() {
@Override
public int rows() {
return sheet.getRows();
}
@Override
public int columns() {
return sheet.getColumns();
}
@Override
public String sheetId() {
return sheet.getSheetId();
}
@Override
public String cellRawValue(int row, int col) {
try {
return sheet.getRawValues()[row][col];
} catch (IndexOutOfBoundsException e) {
return "#ERROR?";
}
}
@Override
public String[][] getRangeValues(String sheetURL, String range) {
// get remote range values
});
 */


public class SpreadsheetEngineImpl implements SpreadsheetEngine {
	
	private static final String ERROR = "#ERROR?";
	private SpreadsheetEngineImpl() {		
	}

	static public SpreadsheetEngine getInstance() {
		return new SpreadsheetEngineImpl();
	}
	
	
	public Result<String[][]> computeSpreadsheetValues(Map<String,Long> versions, AbstractSpreadsheet sheet) {
		ExcelFile workbook = new ExcelFile();
		ExcelWorksheet worksheet = workbook.addWorksheet(sheet.sheetId());

		Map<String,String> serverVersions = new HashMap<>();

		for (int i = 0; i < sheet.rows(); i++)
			for (int j = 0; j < sheet.columns(); j++) {
				String rawVal = sheet.cellRawValue(i, j);
				ExcelCell cell = worksheet.getCell(i, j);
				var r = setCell(versions, sheet, worksheet, cell, rawVal);
				for (var entry : r.getOthers().entrySet()) {
					serverVersions.put(entry.getKey(),entry.getValue());
				}
			}
		worksheet.calculate();

		String[][] cells = new String[sheet.rows()][sheet.columns()];
		for (int row = 0; row < sheet.rows(); row++) {
			for (int col = 0; col < sheet.columns(); col++) {
				ExcelCell cell = worksheet.getCell(row, col);
				var value = cell.getValue();
				cells[row][col] = value != null ? value.toString() : ERROR;
			}
		}

		var result = Result.ok(cells);
		result.setOthers(serverVersions);

		return result;
	}
	
	enum CellType { EMPTY, BOOLEAN, NUMBER, IMPORTRANGE, TEXT, FORMULA };
	
	static Result<Void> setCell(Map<String,Long> versions, AbstractSpreadsheet sheet, ExcelWorksheet worksheet, ExcelCell cell, String rawVal ) {
		CellType type = parseRawValue( rawVal );

		switch (type) {
			case BOOLEAN -> cell.setValue(Boolean.parseBoolean(rawVal));
			case NUMBER -> cell.setValue(Double.parseDouble(rawVal));
			case FORMULA -> cell.setFormula(rawVal);
			case TEXT, EMPTY -> cell.setValue(rawVal);
			case IMPORTRANGE -> {
				var matcher = IMPORTRANGE_PATTERN.matcher(rawVal);
				if (matcher.matches()) {
					var sheetUrl = matcher.group(1);
					var range = matcher.group(2);
					Result<String[][]> values = sheet.rangeValues(versions, sheetUrl, range);
					if (values.isOK())
						applyRange(worksheet, cell, new CellRange(range), values.value());
					else
						cell.setValue(ERROR);

					Result<Void> result = Result.ok();
					result.setOthers(values.getOthers());
					return result;
				}
			}
		}
		return Result.ok();
	}
	
	
	private static void applyRange(ExcelWorksheet worksheet, ExcelCell cell0, CellRange range, String[][] values) {
		int row0 = cell0.getRow().getIndex(), col0 = cell0.getColumn().getIndex();

		for (int r = 0; r < range.rows(); r++)
			for (int c = 0; c < range.cols(); c++) {
				var cell = worksheet.getCell(row0 + r, col0 + c);
				setCell(null,null, worksheet, cell, values[r][c]);
			}
	}

	static CellType parseRawValue(String rawVal) {
		if (rawVal.length() == 0)
			return CellType.EMPTY;

		rawVal = rawVal.toLowerCase();

		if (rawVal.charAt(0) == '=')
			return rawVal.startsWith(IMPORTRANGE_FORMULA) ? CellType.IMPORTRANGE : CellType.FORMULA;

		if (rawVal.equals("true") || rawVal.equals("false"))
			return CellType.BOOLEAN;

		try {
			Double.parseDouble(rawVal);
			return CellType.NUMBER;
		} catch (Exception x) {
		}
		return CellType.TEXT;
	}
	
	static {
		SpreadsheetInfo.setLicense("FREE-LIMITED-KEY");
	}
	
	private static final String URL_REGEX = "(.+)";
	private static final String IMPORTRANGE_FORMULA = "=importrange";
	private static final Pattern IMPORTRANGE_PATTERN = Pattern.compile(String.format("=importrange\\(\"%s\",\"(%s)\"\\)", URL_REGEX, CellRange.RANGE_REGEX));
}
