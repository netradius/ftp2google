package com.netradius.ftp2google.ftp;

import com.netradius.ftp2google.google.GoogleDrive;
import com.netradius.ftp2google.google.GoogleFile;
import org.apache.ftpserver.ftplet.*;

import java.util.Collections;

/**
 * @author Erik R. Jensen
 */
public class FtpFileSystemView implements FileSystemFactory, FileSystemView {

	public static final String FILE_SEPARATOR = "/";

	public static final String FILE_SELF = ".";

	private final User user;

	private FtpFileWrapper home;

	private FtpFileWrapper currentDir;

	private GoogleDrive gdrive;

	public FtpFileSystemView(GoogleDrive gdrive) {
		this(gdrive, null);
	}

	public FtpFileSystemView(GoogleDrive gdrive, User user) {
		this.gdrive = gdrive;
		this.user = user;
	}

	public FileSystemView createFileSystemView(User user) throws FtpException {
		return new FtpFileSystemView(gdrive, user);
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
			this.home = new FtpFileWrapper(null, root, "/", gdrive);
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
			com.google.api.services.drive.model.File fileByName1 = gdrive.getGDriveFileByName(fileName);

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
		return new FtpFileWrapper(folder, gFile, filename, gdrive);
	}

}
