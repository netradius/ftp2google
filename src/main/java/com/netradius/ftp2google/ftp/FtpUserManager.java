package com.netradius.ftp2google.ftp;

import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.PasswordEncryptor;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.AbstractUserManager;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * @author Dilip Sisodia
 * @author Erik Jensen
 */
public class FtpUserManager extends AbstractUserManager{

	private BaseUser defaultUser;
	private BaseUser anonUser;

	public FtpUserManager(Properties configuration, String adminName, PasswordEncryptor passwordEncryptor) {
		super(adminName, passwordEncryptor);

		defaultUser = new BaseUser();
		defaultUser.setAuthorities(Arrays.asList(new Authority[] { new ConcurrentLoginPermission(1, 1) }));
		defaultUser.setEnabled(Boolean.valueOf(configuration.getProperty("user-enabled", "true")));
		defaultUser.setHomeDirectory(configuration.getProperty("home","c:\\temp"));
		defaultUser.setMaxIdleTime(Integer.valueOf(configuration.getProperty("idle-timeout", "300")));
		defaultUser.setName(configuration.getProperty("username","user"));
		defaultUser.setPassword(configuration.getProperty("password","user"));
		List<Authority> authorities = new ArrayList<Authority>();
		authorities.add(new WritePermission());
		authorities.add(new ConcurrentLoginPermission(10, 5));
		defaultUser.setAuthorities(authorities);

		anonUser = new BaseUser(defaultUser);
		anonUser.setName("anonymous");
		anonUser.setEnabled(Boolean.valueOf(configuration.getProperty("anonymous-enabled", "false")));
	}


	public User getUserByName(String username) throws FtpException {
		if (defaultUser.getName().equals(username)) {
			return defaultUser;
		} else if (anonUser.getName().equals(username)) {
			return anonUser;
		}

		return null;
	}


	public String[] getAllUserNames() throws FtpException {
		return new String[] { defaultUser.getName(), anonUser.getName() };
	}


	public void delete(String username) throws FtpException {
		// no opt
	}


	public void save(User user) throws FtpException {
	}


	public boolean doesExist(String username) throws FtpException {
		return ((defaultUser.getEnabled() && defaultUser.getName().equals(username)) || (anonUser.getEnabled() && anonUser.getName()
				.equals(username))) ? true : false;
	}


	public User authenticate(Authentication authentication) throws AuthenticationFailedException {
		if (UsernamePasswordAuthentication.class.isAssignableFrom(authentication.getClass())) {
			UsernamePasswordAuthentication upAuth = (UsernamePasswordAuthentication) authentication;

			if (defaultUser.getEnabled() && defaultUser.getName().equals(upAuth.getUsername())
					&& defaultUser.getPassword().equals(upAuth.getPassword())) {
				return defaultUser;
			}

			if (anonUser.getEnabled() && anonUser.getName().equals(upAuth.getUsername())) {
				return anonUser;
			}
		} else if (AnonymousAuthentication.class.isAssignableFrom(authentication.getClass())) {
			return anonUser.getEnabled() ? anonUser : null;
		}

		return null;
	}
}
