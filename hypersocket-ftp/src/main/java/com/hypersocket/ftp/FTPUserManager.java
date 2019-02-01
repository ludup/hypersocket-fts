package com.hypersocket.ftp;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hypersocket.auth.AuthenticationService;
import com.hypersocket.auth.AuthenticationState;
import com.hypersocket.auth.BrowserEnvironment;
import com.hypersocket.auth.FallbackAuthenticationRequired;
import com.hypersocket.config.ConfigurationService;
import com.hypersocket.fs.FileResourceService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.realm.PrincipalType;
import com.hypersocket.realm.RealmService;
import com.hypersocket.resource.ResourceNotFoundException;
import com.hypersocket.session.SessionService;

@Component
public class FTPUserManager implements UserManager {

	static Logger log = LoggerFactory.getLogger(FTPUserManager.class);
	
	@Autowired
	AuthenticationService authenticationService;

	@Autowired
	FileResourceService fileResourceService; 
	
	@Autowired
	RealmService realmService;

	@Autowired
	ConfigurationService configurationService;
	
	@Autowired
	SessionService sessionService;

	AuthenticationState createAuthenticationState(
			UsernamePasswordAuthentication auth) throws AccessDeniedException {

		try {
			Map<String, Object> environment = new HashMap<String, Object>();

			environment.put(BrowserEnvironment.USER_AGENT.toString(), "FTP Client");
			environment.put(HttpHeaders.AUTHORIZATION,  "basic "
									+ new String(Base64.encodeBase64((auth.getUsername()
											+ ":" + auth.getPassword())
											.getBytes("UTF-8")), "UTF-8"));
			AuthenticationState state = authenticationService
					.createAuthenticationState(FTPService.AUTHENTICATION_SCHEME_RESOURCE_KEY,
							auth.getUserMetadata()
							.getInetAddress().getHostAddress(), environment,
							null,
							Locale.getDefault());
			return state;
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Surely all environments support UTF-8?");
		}
	}

	public User authenticate(Authentication auth)
			throws AuthenticationFailedException {

		if (auth instanceof AnonymousAuthentication) {
			// Currently we do not support anonymous
			throw new AuthenticationFailedException();
		} else if (auth instanceof UsernamePasswordAuthentication) {
			UsernamePasswordAuthentication pwd = (UsernamePasswordAuthentication) auth;

			try {
				AuthenticationState state = createAuthenticationState(pwd);
				
				HashMap<String, String> params = new HashMap<String,String>();
				authenticationService.logon(state, params);

				if (state.isAuthenticationComplete()) {

					if(state.hasPostAuthenticationStep()) {
						throw new AuthenticationFailedException("User must change their password. Please login via the browser first to change your password.");
					}
					// We have authenticated!
					if(log.isDebugEnabled()) {
						log.debug(state.getSession().getCurrentPrincipal().getName() + " has authenticated via FTP authentication");
					}
					
					state.getSession().setTransient(true);
					sessionService.updateSession(state.getSession());
					
					return new FTPSessionUser(state.getSession(), pwd.getPassword(), fileResourceService.getPersonalResources());
				} 
			} catch (AccessDeniedException e) {
				throw new AuthenticationFailedException(e);
			} catch(FallbackAuthenticationRequired e) {
				throw new AuthenticationFailedException(e);
			}

		}
		
		throw new AuthenticationFailedException("Unsupported authentication type " + auth.getClass().getCanonicalName());

	}

	public void delete(String arg0) throws FtpException {

	}

	public boolean doesExist(String user) throws FtpException {
		return realmService.findUniquePrincipal(user);
	}

	public String getAdminName() throws FtpException {
		return "admin";
	}

	public String[] getAllUserNames() throws FtpException {
		return new String[] { "admin" };
	}

	public User getUserByName(String username) throws FtpException {
		try {
			return new FTPUser(realmService.getUniquePrincipal(username, PrincipalType.USER), "admin");
		} catch (ResourceNotFoundException e) {
			throw new FtpException(e);
		}
	}

	public boolean isAdmin(String user) throws FtpException {
		return user.equals("admin");
	}

	public void save(User arg0) throws FtpException {

	}

}
