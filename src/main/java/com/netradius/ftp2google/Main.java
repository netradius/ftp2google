package com.netradius.ftp2google;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.ftplet.FtpException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * Main entry point for ftp2google application.
 *
 * @author Dilip Sisodia
 * @author Erik Jensen
 */
@Slf4j
public class Main {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static FtpServer server = null;

	private static String getOption(CommandLine cmd, Options options, String option, boolean required) {
		if (cmd.hasOption(option)) {
			return cmd.getOptionValue(option);
		} else if (required) {
			String name = options.getOption(option).getLongOpt();
			System.err.println(name + " is required");
			System.exit(1);
		}
		return null;
	}

	private static Properties readProperties(String configLoc) {
		try (FileInputStream fin = new FileInputStream(configLoc)) {
			Properties properties = new Properties();
			properties.load(fin);
			return properties;
		} catch (IOException x) {
			System.err.println("Error reading configuration file " + configLoc + ": " + x.getMessage());
			System.exit(1);
		}
		return null; // will never be reached, needed by compiler
	}

	private static GoogleClientSecrets readSecrets(String secretsLoc) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(secretsLoc), UTF8))) {
			return GoogleClientSecrets.load(JacksonFactory.getDefaultInstance(), reader);
		} catch (IOException x) {
			System.err.println("Error reading secrets file " + secretsLoc + ": " + x.getMessage());
			System.exit(1);
		}
		return null; // will never be reached, needed by compiler
	}

	public static void main(String [] args) throws FtpException {

		Options options = new Options();
		options.addOption("h", "help", false, "prints the help menu");
		options.addOption("c", "config", true, "configuration file (required)");
		options.addOption("s", "secrets", true, "google secrets file (required)");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException x) {
			System.err.println("Error parsing command line options: " + x.getMessage());
			System.exit(1);
		}

		if (cmd.hasOption("h")) {
			new HelpFormatter().printHelp("ftp2google", options);
			System.exit(0);
		}

		String configLoc = getOption(cmd, options, "c", true);
		String secretsLoc = getOption(cmd, options, "s", true);

		Properties config = readProperties(configLoc);
		GoogleClientSecrets secrets = readSecrets(secretsLoc);


		try {
			GoogleDrive drive = new GoogleDrive(secrets, config);
			CoreFTPServer coreFTPServer = new CoreFTPServer(drive, config);
			coreFTPServer.initFTPServer(config);
			server = coreFTPServer.createServer();
			registerShutdownHook();
			server.start();
		} catch (Exception ex) {
			log.warn(ex.getMessage());
		}
	}

	private static void stop() {
		server.stop();
	}

	private static void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				log.info("Shuting down...");
				Main.stop();
				log.info("Good bye!");
			}
		});
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

}
