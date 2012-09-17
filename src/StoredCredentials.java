import java.io.Serializable;

/**
 * User: Joe
 * Date: 9/16/12
 * Time: 1:47 PM
 */
public class StoredCredentials implements Serializable {
	private String userId;
	private String accessToken;
	private String refreshToken;

	public StoredCredentials(String userId, String accessToken, String refreshToken) {
		this.userId = userId;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
}
