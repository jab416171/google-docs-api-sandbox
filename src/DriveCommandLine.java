import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.*;
import java.util.Arrays;

/**
 * User: Joe
 * Date: 9/16/12
 * Time: 10:23 AM
 */
public class DriveCommandLine {

    private static final String CLIENT_ID = getClientId();
    private static final String CLIENT_SECRET = getClientSecret();

    private static String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";

    public static void main(String[] args) throws IOException {
        if (CLIENT_SECRET == null || CLIENT_ID == null) {
            throw new RuntimeException("Get client secret and client id from https://code.google.com/apis/console");
        }
        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET, Arrays.asList(DriveScopes.DRIVE))
                .setAccessType("online")
                .setApprovalPrompt("auto").build();

        String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
        System.out.println("Please open the following URL in your browser then type the authorization code:");
        System.out.println(url);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String code = br.readLine();

        GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
        GoogleCredential credential = new GoogleCredential().setFromTokenResponse(response);

        //Create a new authorized API client
        Drive service = new Drive.Builder(httpTransport, jsonFactory, credential).build();

        //Insert a file
        File body = new File();
        body.setTitle("My document");
        body.setDescription("A test document");
        body.setMimeType("text/plain");

        java.io.File fileContent = new java.io.File("document.txt");
        FileContent mediaContent = new FileContent("text/plain", fileContent);

        File file = service.files().insert(body, mediaContent).execute();
        System.out.println("File ID: " + file.getId());
    }

    public static String getClientSecret() {
	    String contents = getContents(new java.io.File("clientsecret.txt")).trim();
	    return contents;
    }

    public static String getClientId() {
	    String contents = getContents(new java.io.File("clientid.txt")).trim();
	    return contents;
    }

	public static String getContents(java.io.File aFile) {
		//...checks on aFile are elided
		StringBuilder contents = new StringBuilder();

		try {
			//use buffering, reading one line at a time
			//FileReader always assumes default encoding is OK!
			BufferedReader input = new BufferedReader(new FileReader(aFile));
			try {
				String line = null; //not declared within while loop
				/*
								* readLine is a bit quirky :
								* it returns the content of a line MINUS the newline.
								* it returns null only for the END of the stream.
								* it returns an empty String if two newlines appear in a row.
								*/
				while ((line = input.readLine()) != null) {
					contents.append(line);
					contents.append(System.getProperty("line.separator"));
				}
			} finally {
				input.close();
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return contents.toString();
	}
}