import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.Scanner;

/**
 * User: Joe
 * Date: 9/16/12
 * Time: 10:38 AM
 */
public class Main {
	public static void main(String[] args) throws Exception {
		GoogleAuthorizationCodeFlow flow = OAuthManager.getFlow();
		String clientId = flow.getClientId();
		System.out.println(OAuthManager.getAuthorizationUrl("jab416171@gmail.com", null));
		if(OAuthManager.getStoredCredentials(clientId) == null) {
			String code = new Scanner(System.in).nextLine();
			Credential credentials = OAuthManager.getCredentials(code, "");
			OAuthManager.storeCredentials(clientId, credentials);
		}
		Drive service = OAuthManager.buildService(OAuthManager.getStoredCredentials(clientId));
		String fileName = "myfile.xls";
		Drive.Files.Get get = service.files().get(fileName);
		File file = null;
		try {
			file = get.get("fileId") == null ? null : get.execute();
		} catch (GoogleJsonResponseException e) {
			if(e.getStatusCode() == 404)
				file = null;
		}
		if(file == null) {
			file = new File();
			file.setTitle(fileName);
			String mimeType = "application/vnd.google-apps.spreadsheet";
			file.setMimeType(mimeType);

			AbstractInputStreamContent mediaContent = new FileContent(mimeType, new java.io.File("document.xlsx"));
			file = service.files().insert(file, mediaContent).execute();
		}
		System.out.println(file.getId());
		System.out.println(file.getTitle());
		OAuthManager.printFile(service, file.getId());
	}
}
