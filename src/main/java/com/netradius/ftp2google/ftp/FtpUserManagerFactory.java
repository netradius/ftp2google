package com.netradius.ftp2google.ftp;

import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.UserManagerFactory;

import java.util.Properties;

/**
 * @author Dilip Sisodia
 * @author Erik R. Jensen
 */
public class FtpUserManagerFactory implements UserManagerFactory {

	private Properties configuration;

	public FtpUserManagerFactory(Properties configuration) {
		this.configuration = configuration;
	}

	public UserManager createUserManager() {
		return new FtpUserManager(configuration, "admin", new ClearTextPasswordEncryptor());
	}

}
