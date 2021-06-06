package tp1.kafka.event;

public class UpdateCellEvent {

    private String sheetId, cell, rawValue, userId, password;

    public UpdateCellEvent(String sheetId, String cell, String rawValue, String userId, String password) {
        this.sheetId = sheetId;
        this.cell = cell;
        this.rawValue = rawValue;
        this.userId = userId;
        this.password = password;
    }

    public String getSheetId() {
        return sheetId;
    }

    public void setSheetId(String sheetId) {
        this.sheetId = sheetId;
    }

    public String getCell() {
        return cell;
    }

    public void setCell(String cell) {
        this.cell = cell;
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
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
