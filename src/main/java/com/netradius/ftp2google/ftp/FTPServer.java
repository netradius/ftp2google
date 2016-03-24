package com.netradius.ftp2google.ftp;

import com.netradius.ftp2google.google.GoogleDrive;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;

import java.util.Properties;

/**
 * @author Dilip Sisodia
 * @author Erik Jensen
 */
public class FTPServer extends FtpServerFactory {

	protected GoogleDrive googleDrive;
	protected Properties configuration;

	public FTPServer(GoogleDrive gDrive, Properties configuration) {
		this.googleDrive = gDrive;
		this.configuration = configuration;
	}

	public void startServer(Properties configuration) throws FtpException {
		initFTPServer(configuration);
	}


	public void initFTPServer(Properties configuration) throws FtpException {

		setFileSystem(new FtpFileSystemView(googleDrive));
		ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
		boolean anonymousEnabled = Boolean.valueOf(configuration.getProperty("anonymous-enabled", "true"));
		connectionConfigFactory.setAnonymousLoginEnabled(anonymousEnabled);
		int maxLogins = Integer.valueOf(configuration.getProperty("max-logins", "10"));
		connectionConfigFactory.setMaxLogins(maxLogins);
		setConnectionConfig(connectionConfigFactory.createConnectionConfig());

		FtpUserManagerFactory ftpUserManagerFactory = new FtpUserManagerFactory(configuration);
		setUserManager(ftpUserManagerFactory.createUserManager());

		ListenerFactory listenerFactory = new ListenerFactory();
		int port = Integer.valueOf(configuration.getProperty("port", "2221"));
		listenerFactory.setPort(port);

		addListener("default", listenerFactory.createListener());
	}

}
