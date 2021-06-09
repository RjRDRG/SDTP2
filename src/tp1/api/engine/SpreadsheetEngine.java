package tp1.api.engine;

import java.util.Map;

/**
 * 
 * The SpreadsheeEngine class is used to compute the values of a spreadsheet from its raw values.
 * 
 * 
 * @author smd
 *
 */
public interface SpreadsheetEngine {

	/**
	 * 
	 * @param sheet - The spreadsheet whose cells will be used to compute the values
	 * @return the full "matrix" of cell values.
	 */
	String[][] computeSpreadsheetValues(Map<String,Long> versions, AbstractSpreadsheet sheet);

}
