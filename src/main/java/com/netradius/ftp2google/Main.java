package com.netradius.ftp2google;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.*;
import java.net.URL;
import java.util.Properties;

/**
 * @author Dilip Sisodia
 */
public class Main {

	private static final Logger LOG = Logger.getLogger(Main.class);
	private static FtpServer server = null;

	public static void main(String [] args) throws FtpException {

		configureLogging();

		Properties configuration = readProperties();
		if(args.length > 0){
			configuration.putAll(readCommandLinePropertiesFile(args[0]));
		}
		try {

			GoogleDrive drive = new GoogleDrive(configuration);
			CoreFTPServer coreFTPServer = new CoreFTPServer(drive, configuration);
			coreFTPServer.initFTPServer(configuration);
			server = coreFTPServer.createServer();
			registerShutdownHook();
			server.start();

		} catch (Exception ex) {
			LOG.warn(ex.getMessage());

		}
	}

	private static void stop() {
		server.stop();
	}

	private static void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				LOG.info("Shuting down...");
				Main.stop();
				LOG.info("Good bye!");
			}
		});
	}

	public static Properties readProperties() {
		Properties properties = new Properties();

		InputStream configurationStream = Main.class.getResourceAsStream("/configuration.properties");
		if (configurationStream == null) {
			return properties;
		}

		try {
			properties.load(configurationStream);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (configurationStream != null) {
				try {
					configurationStream.close();
				} catch (Exception ex) {
				}
			}
		}
		return properties;
	}

	static Properties readCommandLinePropertiesFile(String propertiesFilename) {
		Properties properties = new Properties();
		FileInputStream inStream = null;
		try {
			inStream = new FileInputStream(propertiesFilename);
			properties.load(inStream);
		} catch (Exception ex) {
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (Exception ex) {
				}
			}
		}
		return properties;
	}

	private static void configureLogging() {
		try {
			String log4jFilename = "classpath:/log4j.xml";

			if (log4jFilename.startsWith("classpath:")) {
				URL log4j_resource = Main.class.getResource("/log4j.xml");
				if (log4j_resource == null) {
					LOG.warn("Resource '/log4j.xml' not found on classpath. Logging to file system not enabled.");
				} else {
					DOMConfigurator.configure(log4j_resource);
				}
			} else {
				LOG.warn("Log4j couldn't be configured. Logs wont be written to file.");
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
