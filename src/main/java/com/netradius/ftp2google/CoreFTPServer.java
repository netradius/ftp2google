/**
 * @author Dilip Sisodia
 */

package com.netradius.ftp2google;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.*;
import org.apache.ftpserver.usermanager.impl.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class CoreFTPServer extends FtpServerFactory {

	protected GoogleDrive googleDrive;
	protected Properties configuration;

	public CoreFTPServer(GoogleDrive gDrive, Properties configuration) {
		this.googleDrive = gDrive;
		this.configuration = configuration;
	}
	public void startServer(Properties configuration) throws FtpException {
		initFTPServer(configuration);
	}


	public void initFTPServer(Properties configuration) throws FtpException {

		setFileSystem(new FtpFileSystemView());
		ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
		boolean anonymousEnabled = Boolean.valueOf(configuration.getProperty("anonymous-enabled", "true"));
		connectionConfigFactory.setAnonymousLoginEnabled(anonymousEnabled);
		int maxLogins = Integer.valueOf(configuration.getProperty("max-logins", "10"));
		connectionConfigFactory.setMaxLogins(maxLogins);
		setConnectionConfig(connectionConfigFactory.createConnectionConfig());

		FtpUserManagerFactory ftpUserManagerFactory = new FtpUserManagerFactory();
		setUserManager(ftpUserManagerFactory.createUserManager());

		ListenerFactory listenerFactory = new ListenerFactory();
		int port = Integer.valueOf(configuration.getProperty("port", "2221"));
		listenerFactory.setPort(port);

		addListener("default", listenerFactory.createListener());
	}

	class FtpUserManagerFactory implements UserManagerFactory {

		public UserManager createUserManager() {
			return new FtpUserManager("admin", new ClearTextPasswordEncryptor());
		}
	}

	class FtpUserManager extends AbstractUserManager {

		private BaseUser defaultUser;
		private BaseUser anonUser;

		public FtpUserManager(String adminName, PasswordEncryptor passwordEncryptor) {
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

	class FtpFileSystemView implements FileSystemFactory, FileSystemView {

		class FtpFileWrapper implements FtpFile {

			private final FtpFileWrapper parent;
			private final GoogleFile gfile;

			private String virtualName;
			private FtpFileWrapper home;

			public FtpFileWrapper(FtpFileWrapper parent, GoogleFile ftpGFile, String virtualName) {
				this.parent = parent;
				this.gfile = ftpGFile;
				this.virtualName = virtualName;
			}

			public String getId() {
				return gfile.getId();
			}

			public String getAbsolutePath() {
				return isRoot() ? virtualName : parent.isRoot() ? FILE_SEPARATOR + virtualName : parent.getAbsolutePath() + FILE_SEPARATOR
						+ virtualName;
			}


			public boolean isHidden() {
				return false;
			}


			public boolean isFile() {
				return !isDirectory();
			}


			public boolean doesExist() {
				return gfile.isExists();
			}


			public boolean isReadable() {
				return true;
			}


			public boolean isWritable() {
				return true;
			}


			public boolean isRemovable() {
				return gfile.isRemovable();
			}


			public String getOwnerName() {
				return gfile.getOwnerName();
			}


			public String getGroupName() {
				return "netradius";
			}


			public int getLinkCount() {
				return gfile.getParents() != null ? gfile.getParents().size() : 0;
			}


			public long getSize() {
				return gfile.getSize();
			}


			public boolean delete() {
				if (!doesExist()) {
					throw new RuntimeException("File does not exists");
				}
				return false;

			}


			public long getLastModified() {
				return gfile.getLastModified();
			}


			public String getName() {
				return virtualName;
			}


			public boolean isDirectory() {
				return gfile.isDirectory();
			}

			public GoogleFile unwrap() {
				return gfile;
			}


			public boolean move(FtpFile destination) {
			return false;
			}


			public OutputStream createOutputStream(long offset) throws IOException {
				return googleDrive.createOutputStream(this.unwrap());
			}


			public InputStream createInputStream(long offset) throws IOException {
				return googleDrive.createInputStream(this.unwrap());
			}


			public boolean mkdir() {
				if (isRoot()) {
					throw new IllegalArgumentException("Cannot create root folder");
				}
				return false;
			}


			public boolean setLastModified(long arg0) {
				return true;
			}


			public List<FtpFile> listFiles() {
				return  null;
			}


			public String toString() {
				return "FtpFileWrapper [absolutePath=" + getAbsolutePath() + "]";
			}


			public boolean isRoot() {
				return parent == null;
			}

			public FtpFileWrapper getParentFile() {
				return parent;
			}

			public void setVirtualName(String virtualName) {
				this.virtualName = virtualName;
			}
		}

		public static final String FILE_SEPARATOR = "/";


		public static final String FILE_SELF = ".";

		private final User user;

		private FtpFileWrapper home;

		private FtpFileWrapper currentDir;

		public FtpFileSystemView() {
			this.user = null;
		}

		public FtpFileSystemView(User user) {
			this.user = user;
		}


		public FileSystemView createFileSystemView(User user) throws FtpException {
			return new FtpFileSystemView(user);
		}


		public boolean isRandomAccessible() throws FtpException {
			return true;
		}


		public FtpFile getHomeDirectory() throws FtpException {
			return home;
		}


		public FtpFile getWorkingDirectory() throws FtpException {

			initWorkingDirectory();

			return currentDir;
		}

		private void initWorkingDirectory() {
			if (currentDir == null) {
				GoogleFile root = new GoogleFile();
				root.setId("root");
				this.home = new FtpFileWrapper(null, root, "/");
				this.currentDir = this.home;
			}
		}


		public boolean changeWorkingDirectory(String path) throws FtpException {
			try {
				initWorkingDirectory();
				if (FILE_SEPARATOR.equals(path)) {
					currentDir = home;
					return true;
				}
				if (FILE_SELF.equals(path)) {
					return true;
				}

				FtpFileWrapper file = null;
				if (path.startsWith(FILE_SEPARATOR)) {
					file = getFileByAbsolutePath(path);
				} else {
					file = getFileByRelativePath(currentDir, path);
				}

				if (file != null && file.isDirectory()) {
					currentDir = file;
					return true;
				}
				return false;
			} catch (Exception e) {
				throw new FtpException(e.getMessage(), e);
			}
		}


		public void dispose() {
			currentDir = null;
		}


		public FtpFile getFile(String fileName) throws FtpException {

			initWorkingDirectory();

			try {
				if ("./".equals(fileName)) {
					return currentDir;
				}

				if (fileName.length() == 0) {
					return currentDir;
				}

				return fileName.startsWith(FILE_SEPARATOR) ? getFileByAbsolutePath(fileName) : getFileByName(currentDir, fileName);
			} catch (IllegalArgumentException e) {
				throw new FtpException(e.getMessage(), e);
			} catch (Exception e) {
				throw new FtpException(e.getMessage(), e);
			}
		}

		private FtpFileWrapper getFileByAbsolutePath(String path) {
			if (!path.startsWith(FILE_SEPARATOR)) {
				throw new IllegalArgumentException("Path '" + path + "' should start with '" + FILE_SEPARATOR + "'");
			}
			if (currentDir.getAbsolutePath().equals(path)) {
				return currentDir;
			}

			FtpFileWrapper folder;
			if (path.startsWith(currentDir.isRoot() ? currentDir.getAbsolutePath() : currentDir.getAbsolutePath() + FILE_SEPARATOR)) {
				folder = currentDir;
				path = path.substring(folder.getAbsolutePath().length() + 1);
			} else {
				folder = home;
				path = path.substring(1);
			}

			return getFileByRelativePath(folder, path);
		}

		private FtpFileWrapper getFileByRelativePath(FtpFileWrapper folder, String path) {
			FtpFileWrapper file = null;
			if (!path.contains(FILE_SEPARATOR)) {
				file = getFileByName(folder, path);
				return file;
			}

			for (String part : path.split(FtpFileSystemView.FILE_SEPARATOR)) {
				file = getFileByName(folder, part);
				folder = file;
			}
			return file;
		}

		private FtpFileWrapper getFileByName(FtpFileWrapper folder, String fileName) {
			String absolutePath = folder.getAbsolutePath() + (folder.isRoot() ? "" : FILE_SEPARATOR) + fileName;

			try {
				com.google.api.services.drive.model.File fileByName1 = googleDrive.getGDriveFileByName(fileName);

				if (fileByName1 != null) {
					GoogleFile fileByName = GoogleFile.create(fileByName1);
					return createFtpFileWrapper(folder, fileByName, fileName, true);
				}

				return createFtpFileWrapper(folder, new GoogleFile(Collections.singleton(folder.getId()), fileName), fileName, false);
			} catch (Exception e) {
				return createFtpFileWrapper(folder, new GoogleFile(Collections.singleton(folder.getId()), fileName), fileName, false);
			}
		}

		private FtpFileWrapper createFtpFileWrapper(FtpFileWrapper folder, GoogleFile gFile, String filename, boolean exists) {

			String absolutePath = folder == null ? filename : folder.isRoot() ? FILE_SEPARATOR + filename : folder.getAbsolutePath()
					+ FILE_SEPARATOR + filename;
			return new FtpFileWrapper(folder, gFile, filename);
		}

	}


}
