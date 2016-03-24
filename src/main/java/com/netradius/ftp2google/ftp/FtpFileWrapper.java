package com.netradius.ftp2google.ftp;

import com.netradius.ftp2google.google.GoogleDrive;
import com.netradius.ftp2google.google.GoogleFile;
import org.apache.ftpserver.ftplet.FtpFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * @author Dilip Sisodia
 * @author Erik R. Jensen
 */
public class FtpFileWrapper implements FtpFile {

	private final GoogleDrive gdrive;
	private final FtpFileWrapper parent;
	private final GoogleFile gfile;
	private String virtualName;

	public FtpFileWrapper(FtpFileWrapper parent, GoogleFile gfile, String virtualName, GoogleDrive gdrive) {
		this.parent = parent;
		this.gfile = gfile;
		this.virtualName = virtualName;
		this.gdrive = gdrive;
	}

	public String getId() {
		return gfile.getId();
	}

	public String getAbsolutePath() {
		if (isRoot()) {
			return virtualName;
		} if (parent.isRoot()) {
			return "/" + virtualName;
		} else {
			return parent.getAbsolutePath() + "/" + virtualName;
		}
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
		return "netradius"; // TODO what is this? it shouldn't be hard coded
	}

	public int getLinkCount() {
		return gfile.getParents() != null ? gfile.getParents().size() : 0;
	}

	public long getSize() {
		return gfile.getSize();
	}

	public boolean delete() {
		if (!doesExist()) {
			throw new RuntimeException("File does not exists"); // TODO Not sure this is the best thing to do
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

	public boolean move(FtpFile destination) {
		return false;
	}

	public OutputStream createOutputStream(long offset) throws IOException {
		return gdrive.createOutputStream(gfile);
	}

	public InputStream createInputStream(long offset) throws IOException {
		return gdrive.createInputStream(gfile);
	}

	public boolean mkdir() {
		if (isRoot()) {
			throw new IllegalArgumentException("Cannot create root folder"); // TODO Not sure this is the best thing to do
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
}
