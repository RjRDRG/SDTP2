package tp1.clients.sheet;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import org.pac4j.scribe.builder.api.DropboxApi20;
import tp1.api.Spreadsheet;
import tp1.api.service.util.Result;

public class SpreadsheetDropboxClient {

    public Result<String> createFolder(String directory) {
        return null;
    }

    public Result<String> createSpreadsheet(Spreadsheet sheet, String password) {

        return null;
    }

    public Result<String> updateSpreadsheet(Spreadsheet sheet, String password) {
        return null;
    }

    public Result<Void> deleteSpreadsheet(String sheetId, String password) {
        return null;
    }


    public Result<Spreadsheet> getSpreadsheet(String sheetId, String userId, String password) {
        return null;
    }

    static class CreateDirectory {
        private static final String apiKey = "56fv9o6uz9l0au2";
        private static final String apiSecret = "kgbcni2nvolr8y2";
        private static final String accessTokenStr = "70Di9fHgPoEAAAAAAAAAAaBY6243Nc5eHAENewpb1sRVNnlViNDFKjvLCq7Py-6J";

        static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

        static final String CREATE_FOLDER_V2_URL =
                "https://api.dropboxapi.com/2/files/create_folder_v2";

        private OAuth20Service service;
        private OAuth2AccessToken accessToken;

        private Gson json;

        public CreateDirectory() {
            service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
            accessToken = new OAuth2AccessToken(accessTokenStr);

            json = new Gson();
        }
    }
}
