package tp1.kafka.event;

import tp1.api.Spreadsheet;

public class CreateSpreadsheetEvent {

    private Spreadsheet sheet;
    private String password;

    public CreateSpreadsheetEvent(Spreadsheet sheet, String password) {
        this.sheet = sheet;
        this.password = password;
    }

    public Spreadsheet getSheet() {
        return sheet;
    }

    public void setSheet(Spreadsheet sheet) {
        this.sheet = sheet;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
