import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Joe
 * Date: 9/16/12
 * Time: 10:23 AM
 */
public class OAuthManager {

	private static final String CLIENTSECRETS_LOCATION = "client_secrets.json";

	public static final String REDIRECT_URI = "urn:ietf:wg:oauth:2.0:oob";
	private static final List<String> SCOPES = Arrays.asList(
			"https://www.googleapis.com/auth/drive.file",
			"https://www.googleapis.com/auth/userinfo.email",
			"https://www.googleapis.com/auth/userinfo.profile");


	private static GoogleAuthorizationCodeFlow flow = null;

	/**
	 * Exception thrown when an error occurred while retrieving credentials.
	 */
	public static class GetCredentialsException extends Exception {

		protected String authorizationUrl;

		/**
		 * Construct a GetCredentialsException.
		 *
		 * @param authorizationUrl The authorization URL to redirect the user to.
		 */
		public GetCredentialsException(String authorizationUrl) {
			this.authorizationUrl = authorizationUrl;
		}

		/**
		 * Set the authorization URL.
		 */
		public void setAuthorizationUrl(String authorizationUrl) {
			this.authorizationUrl = authorizationUrl;
		}

		/**
		 * @return the authorizationUrl
		 */
		public String getAuthorizationUrl() {
			return authorizationUrl;
		}
	}

	/**
	 * Exception thrown when a code exchange has failed.
	 */
	public static class CodeExchangeException extends GetCredentialsException {

		/**
		 * Construct a CodeExchangeException.
		 *
		 * @param authorizationUrl The authorization URL to redirect the user to.
		 */
		public CodeExchangeException(String authorizationUrl) {
			super(authorizationUrl);
		}

	}

	/**
	 * Exception thrown when no refresh token has been found.
	 */
	public static class NoRefreshTokenException extends GetCredentialsException {

		/**
		 * Construct a NoRefreshTokenException.
		 *
		 * @param authorizationUrl The authorization URL to redirect the user to.
		 */
		public NoRefreshTokenException(String authorizationUrl) {
			super(authorizationUrl);
		}

	}

	/**
	 * Exception thrown when no user ID could be retrieved.
	 */
	private static class NoUserIdException extends Exception {
	}

	/**
	 * Retrieved stored credentials for the provided user ID.
	 *
	 * @param userId User's ID.
	 * @return Stored Credential if found, {@code null} otherwise.
	 */
	static Credential getStoredCredentials(String userId) {
		// TODO: Implement this method to work with your database. Instantiate a new
		// Credential instance with stored accessToken and refreshToken.
		StoredCredentials storedCredentials = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream("StoredCredentials.dat"));

			String newUserId = ois.readUTF(); // consume the user ID
			String accessToken = ois.readUTF();
			String refreshToken = ois.readUTF();
			Object jsonFactory = ois.readObject();
			Object transport = ois.readObject();
			if (newUserId == null || newUserId.isEmpty() || accessToken == null || accessToken.isEmpty() || refreshToken == null || refreshToken.isEmpty()) {
				java.io.File file = new java.io.File("StoredCredentials.dat");
				file.delete();
				return null;
			}
			storedCredentials = new StoredCredentials(userId, accessToken, refreshToken);
		} catch (IOException e) {
			return null;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return new GoogleCredential().setAccessToken(storedCredentials.getAccessToken()).setRefreshToken(storedCredentials.getRefreshToken());
	}

	/**
	 * Store OAuth 2.0 credentials in the application's database.
	 *
	 * @param userId      User's ID.
	 * @param credentials The OAuth 2.0 credentials to store.
	 */
	static void storeCredentials(String userId, Credential credentials) {
		// TODO: Implement this method to work with your database.
		// Store the credentials.getAccessToken() and credentials.getRefreshToken()
		// string values in your database.

		java.io.File file = new java.io.File("StoredCredentials.dat");
		file.delete();
		try {
			ObjectOutputStream ois = new ObjectOutputStream(new FileOutputStream("StoredCredentials.dat"));

			ois.writeUTF(userId);
			ois.writeUTF(credentials.getAccessToken());
			ois.writeUTF(credentials.getRefreshToken());
			ois.writeObject(credentials.getJsonFactory());
			ois.writeObject(credentials.getTransport());
			ois.flush();
			ois.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Build an authorization flow and store it as a static class attribute.
	 *
	 * @return GoogleAuthorizationCodeFlow instance.
	 * @throws IOException Unable to load client_secrets.json.
	 */
	static GoogleAuthorizationCodeFlow getFlow() throws IOException {
		if (flow == null) {
			HttpTransport httpTransport = new NetHttpTransport();
			JacksonFactory jsonFactory = new JacksonFactory();
			GoogleClientSecrets clientSecrets =
					GoogleClientSecrets.load(jsonFactory,
							new FileInputStream(CLIENTSECRETS_LOCATION));
			flow =
					new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets, SCOPES)
							.setAccessType("offline").setApprovalPrompt("force").build();
		}
		return flow;
	}

	/**
	 * Exchange an authorization code for OAuth 2.0 credentials.
	 *
	 * @param authorizationCode Authorization code to exchange for OAuth 2.0
	 *                          credentials.
	 * @return OAuth 2.0 credentials.
	 * @throws CodeExchangeException An error occurred.
	 */
	static Credential exchangeCode(String authorizationCode)
			throws CodeExchangeException {
		try {
			GoogleAuthorizationCodeFlow flow = getFlow();
			GoogleTokenResponse response =
					flow.newTokenRequest(authorizationCode).setRedirectUri(REDIRECT_URI).execute();
			return flow.createAndStoreCredential(response, null);
		} catch (IOException e) {
			System.err.println("An error occurred: " + e);
			throw new CodeExchangeException(null);
		}
	}

	/**
	 * Send a request to the UserInfo API to retrieve the user's information.
	 *
	 * @param credentials OAuth 2.0 credentials to authorize the request.
	 * @return User's information.
	 * @throws NoUserIdException An error occurred.
	 */
	static Userinfo getUserInfo(Credential credentials)
			throws NoUserIdException {
		Oauth2 userInfoService =
				new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), credentials).build();
		Userinfo userInfo = null;
		try {
			userInfo = userInfoService.userinfo().get().execute();
		} catch (IOException e) {
			System.err.println("An error occurred: " + e);
		}
		if (userInfo != null && userInfo.getId() != null) {
			return userInfo;
		} else {
			throw new NoUserIdException();
		}
	}

	/**
	 * Retrieve the authorization URL.
	 *
	 * @param emailAddress User's e-mail address.
	 * @param state        State for the authorization URL.
	 * @return Authorization URL to redirect the user to.
	 * @throws IOException Unable to load client_secrets.json.
	 */
	public static String getAuthorizationUrl(String emailAddress, String state) throws IOException {
		GoogleAuthorizationCodeRequestUrl urlBuilder =
				getFlow().newAuthorizationUrl().setRedirectUri(REDIRECT_URI).setState(state);
		urlBuilder.set("user_id", emailAddress);
		return urlBuilder.build();
	}

	/**
	 * Retrieve credentials using the provided authorization code.
	 * <p/>
	 * This function exchanges the authorization code for an access token and
	 * queries the UserInfo API to retrieve the user's e-mail address. If a
	 * refresh token has been retrieved along with an access token, it is stored
	 * in the application database using the user's e-mail address as key. If no
	 * refresh token has been retrieved, the function checks in the application
	 * database for one and returns it if found or throws a NoRefreshTokenException
	 * with the authorization URL to redirect the user to.
	 *
	 * @param authorizationCode Authorization code to use to retrieve an access
	 *                          token.
	 * @param state             State to set to the authorization URL in case of error.
	 * @return OAuth 2.0 credentials instance containing an access and refresh
	 *         token.
	 * @throws NoRefreshTokenException No refresh token could be retrieved from
	 *                                 the available sources.
	 * @throws IOException             Unable to load client_secrets.json.
	 */
	public static Credential getCredentials(String authorizationCode, String state)
			throws CodeExchangeException, NoRefreshTokenException, IOException {
		String emailAddress = "";
		try {
			Credential credentials = exchangeCode(authorizationCode);
			Userinfo userInfo = getUserInfo(credentials);
			String userId = userInfo.getId();
			emailAddress = userInfo.getEmail();
			if (credentials.getRefreshToken() != null) {
				storeCredentials(userId, credentials);
				return credentials;
			} else {
				credentials = getStoredCredentials(userId);
				if (credentials != null && credentials.getRefreshToken() != null) {
					return credentials;
				}
			}
		} catch (CodeExchangeException e) {
			e.printStackTrace();
			// Drive apps should try to retrieve the user and credentials for the current
			// session.
			// If none is available, redirect the user to the authorization URL.
			e.setAuthorizationUrl(getAuthorizationUrl(emailAddress, state));
			throw e;
		} catch (NoUserIdException e) {
			e.printStackTrace();
		}
		// No refresh token has been retrieved.
		String authorizationUrl = getAuthorizationUrl(emailAddress, state);
		throw new NoRefreshTokenException(authorizationUrl);
	}

	public static Drive buildService(Credential credentials) {
		HttpTransport httpTransport = new NetHttpTransport();
		JacksonFactory jsonFactory = new JacksonFactory();

		return new Drive.Builder(httpTransport, jsonFactory, credentials)
				.build();
	}

	public static void printFile(Drive service, String fileId) {
		try {
			File file = service.files().get(fileId).execute();

			System.out.println("Title: " + file.getTitle());
			System.out.println("Description: " + file.getDescription());
			System.out.println("MIME type: " + file.getMimeType());
		} catch (HttpResponseException e) {
			if (e.getStatusCode() == 401) {
				// Credentials have been revoked.
				// TODO: Redirect the user to the authorization URL.
				throw new UnsupportedOperationException();
			}
		} catch (IOException e) {
			System.out.println("An error occurred: " + e);
		}
	}
}