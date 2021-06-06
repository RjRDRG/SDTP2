package tp1.kafka.event;

public class DeleteSpreadsheetEvent {

    private String sheetId, password;

    public DeleteSpreadsheetEvent(String sheetId, String password) {
        this.sheetId = sheetId;
        this.password = password;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
