package tp1.kafka.event;

public class ShareSpreadsheetEvent {

    private String sheetId, userId, password;

    public ShareSpreadsheetEvent(String sheetId, String userId, String password) {
        this.sheetId = sheetId;
        this.userId = userId;
        this.password = password;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
