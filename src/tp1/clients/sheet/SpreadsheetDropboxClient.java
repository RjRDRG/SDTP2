package tp1.clients.sheet;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import org.pac4j.scribe.builder.api.DropboxApi20;
import tp1.api.Spreadsheet;
import tp1.api.service.util.Result;

import java.util.Optional;

public class SpreadsheetDropboxClient implements SpreadsheetRepositoryClient{

    @Override
    public Result<String> uploadSpreadsheet(String path, Spreadsheet spreadsheet) {
        try {
            UploadSpreadsheet.UploadSpreadsheetReply reply = UploadSpreadsheet.execute(new UploadSpreadsheet.UploadSpreadsheetArgs(path),spreadsheet);
            return Result.ok(path);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage(),e);
        }
    }

    @Override
    public Result<Void> delete(String path) {
        try {
            DeleteFile.execute(new DeleteFile.DeleteFileArgs(path));
            return Result.ok();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage(),e);
        }
    }

    @Override
    public Result<Spreadsheet> getSpreadsheet(String path) {
        try {
            return Result.ok(GetSpreadsheet.execute(new GetSpreadsheet.GetSpreadsheetArgs(path)));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage(),e);
        }
    }



    static class DropboxRequest {
        protected static final String apiKey = "56fv9o6uz9l0au2";
        protected static final String apiSecret = "kgbcni2nvolr8y2";
        protected static final String accessTokenStr = "70Di9fHgPoEAAAAAAAAAAaBY6243Nc5eHAENewpb1sRVNnlViNDFKjvLCq7Py-6J";
        protected static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
        protected static final String STREAM_CONTENT_TYPE = "application/octet-stream";
        protected static final String TEXT_CONTENT_TYPE = "text/plain; charset=dropbox-cors-hack";
        protected static final OAuth20Service service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
        protected static final OAuth2AccessToken accessToken  = new OAuth2AccessToken(accessTokenStr);
        protected static final Gson json = new Gson();
    }



    static class UploadSpreadsheet extends DropboxRequest{

        static class UploadSpreadsheetArgs {
            public final String path;
            public final String mode;
            public final boolean autorename;
            public final boolean mute;
            public final boolean strict_conflict;

            public UploadSpreadsheetArgs(String path) {
                this.path = path;
                mode = "overwrite";
                autorename = false;
                mute = true;
                strict_conflict = false;
            }
        }

        static class UploadSpreadsheetReply {
            private String name;
            private String id;
            private String client_modified;
            private String server_modified;
            private String rev;
            private int size;
            private boolean is_downloadable;

            public UploadSpreadsheetReply() {
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getClient_modified() {
                return client_modified;
            }

            public void setClient_modified(String client_modified) {
                this.client_modified = client_modified;
            }

            public String getServer_modified() {
                return server_modified;
            }

            public void setServer_modified(String server_modified) {
                this.server_modified = server_modified;
            }

            public String getRev() {
                return rev;
            }

            public void setRev(String rev) {
                this.rev = rev;
            }

            public int getSize() {
                return size;
            }

            public void setSize(int size) {
                this.size = size;
            }

            public boolean isIs_downloadable() {
                return is_downloadable;
            }

            public void setIs_downloadable(boolean is_downloadable) {
                this.is_downloadable = is_downloadable;
            }
        }

        static final String CREATE_FILE_V2_URL = "https://content.dropboxapi.com/2/files/upload";

        public static UploadSpreadsheetReply execute(UploadSpreadsheetArgs args, Spreadsheet spreadsheet) throws Exception {
            OAuthRequest request = new OAuthRequest(Verb.POST, CREATE_FILE_V2_URL);
            request.addHeader("Dropbox-API-Arg", json.toJson(args));
            request.addHeader("Content-Type", TEXT_CONTENT_TYPE);
            request.setPayload(json.toJson(spreadsheet));

            service.signRequest(accessToken, request);

            Response r = null;
            String exceptionMsg = "";
            try {
                r = service.execute(request);
            } catch (Exception e) {
                exceptionMsg = e.getMessage();
            }

            if(r == null) {
                throw new Exception("Invalid request\n" + exceptionMsg);
            }
            else if(r.getCode() != 200) {
                throw new Exception(
                        "status: " + r.getCode() +
                        "\nmessage: " + r.getMessage() +
                        "\nbody: " + Optional.ofNullable(r.getBody()).orElse("") +
                        "\n" + exceptionMsg
                );
            } else {
                return json.fromJson(r.getBody(), UploadSpreadsheetReply.class);
            }
        }
    }



    static class DeleteFile extends DropboxRequest{

        static class DeleteFileArgs {
            public final String path;

            public DeleteFileArgs(String path) {
                this.path = path;
            }
        }

        static final String DELETE_FILE_V2_URL =
                "https://api.dropboxapi.com/2/files/delete_v2";

        public static void execute(DeleteFileArgs args) throws Exception {
            OAuthRequest request = new OAuthRequest(Verb.POST, DELETE_FILE_V2_URL);
            request.addHeader("Content-Type", JSON_CONTENT_TYPE);
            request.setPayload(json.toJson(args));

            service.signRequest(accessToken, request);

            Response r = service.execute(request);

            if(r == null) {
                throw new Exception("Invalid request");
            }
            else if(r.getCode() != 200) {
                throw new Exception(
                        "status: " + r.getCode() +
                        "\nmessage: " + r.getMessage() +
                        "\nbody: " + Optional.ofNullable(r.getBody()).orElse("")
                );
            }
        }
    }



    static class GetSpreadsheet extends DropboxRequest{

        static class GetSpreadsheetArgs {
            public final String path;

            public GetSpreadsheetArgs(String path) {
                this.path = path;
            }
        }

        static final String GET_SPREADSHEET_V2_URL = "https://content.dropboxapi.com/2/files/download";

        public static Spreadsheet execute(GetSpreadsheetArgs args) throws Exception {
            OAuthRequest request = new OAuthRequest(Verb.POST, GET_SPREADSHEET_V2_URL);
            request.addHeader("Dropbox-API-Arg", json.toJson(args));
            request.addHeader("Content-Type", STREAM_CONTENT_TYPE);

            service.signRequest(accessToken, request);

            Response r = service.execute(request);

            if(r == null) {
                throw new Exception("Dropbox Reply Invalid request");
            }
            else if(r.getCode() != 200) {
                throw new Exception(
                        "Dropbox Reply:" +
                        "\tstatus: " + r.getCode() +
                        "\n\tmessage: " + r.getMessage() +
                        "\n\tbody: " + Optional.ofNullable(r.getBody()).orElse("")
                );
            } else {
                return json.fromJson(r.getBody(), Spreadsheet.class);
            }
        }
    }
}
