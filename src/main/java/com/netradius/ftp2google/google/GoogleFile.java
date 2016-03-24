package com.netradius.ftp2google.google;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import lombok.Data;

import java.io.Serializable;
import java.net.URL;
import java.util.*;

/**
 * @author Erik R. Jensen
 */
@Data
public class GoogleFile implements Serializable, Cloneable {

	private String id;
	private long revision;
	private Set<String> labels;
	private String name;
	private boolean isDirectory;
	private long size;
	private String md5Checksum;
	private long lastModified;
	private String mimeType;
	private Set<String> parents;

	private transient java.io.File transferFile = null;

	private transient URL downloadUrl;
	private long lastViewedByMeDate;

	private transient GoogleFile currentParent;

	private boolean exists;

	public GoogleFile() {
		this("");
	}

	public GoogleFile(String name) {
		this.name = name;
	}

	public GoogleFile(Set<String> parents, String name) {
		this(name);
		this.parents = parents;
	}

	public long getLastViewedByMeDate() {
		return lastViewedByMeDate;
	}

	public void setLastViewedByMeDate(long lastViewedByMeDate) {
		this.lastViewedByMeDate = lastViewedByMeDate;
	}

	@Override
	public Object clone() {
		GoogleFile ret = new GoogleFile(getName());
		ret.setId(getId());
		ret.setName(getName());
		ret.setDirectory(isDirectory());
		ret.setSize(getSize());
		ret.setLastModified(getLastModified());
		ret.setMd5Checksum(getMd5Checksum());
		ret.setRevision(getRevision());
		ret.setParents(getParents());
		ret.setMimeType(mimeType);
		ret.setExists(isExists());
		ret.setLastViewedByMeDate(getLastViewedByMeDate());
		return ret;
	}

	public static GoogleFile create(File googleFile) {
		if (googleFile == null)
			return null;
		GoogleFile newFile = new GoogleFile(getFilename(googleFile));
		newFile.setId(googleFile.getId());
		newFile.setLastModified(getLastModified(googleFile));
		newFile.setSize(getFileSize(googleFile));
		newFile.setDirectory(isDirectory(googleFile));
		newFile.setMd5Checksum(googleFile.getMd5Checksum());
		newFile.setParents(new HashSet<>());
		newFile.setExists(true);
		for (ParentReference ref : googleFile.getParents()) {
			if (ref.getIsRoot()) {
				newFile.getParents().add("root");
			} else {
				newFile.getParents().add(ref.getId());
			}
		}
		if (googleFile.getLabels().getTrashed()) {
			newFile.setLabels(Collections.singleton("trashed"));
		} else {
			newFile.setLabels(Collections.<String> emptySet());
		}
		if (googleFile.getLastViewedByMeDate() != null) {
			newFile.setLastViewedByMeDate(googleFile.getLastViewedByMeDate().getValue());
		}
		return newFile;
	}

	public static List<GoogleFile> create(List<File> googleFiles, long revision) {
		List<GoogleFile> ret = new ArrayList<>(googleFiles.size());
		for (File child : googleFiles) {
			GoogleFile localFile = create(child);
			localFile.setRevision(revision);
			ret.add(localFile);
		}
		return ret;
	}

	private static String getFilename(File file) {
		String filename = file.getTitle() != null ? file.getTitle() : file.getOriginalFilename();
		return filename;
	}

	private static boolean isDirectory(File googleFile) {
		boolean isDirectory = "application/vnd.google-apps.folder".equals(googleFile.getMimeType());
		return isDirectory;
	}

	private static long getLastModified(File googleFile) {
		final boolean b = googleFile != null && googleFile.getModifiedDate() != null;
		if (b) {
			return googleFile.getModifiedDate().getValue();
		} else {
			return 0;
		}

	}

	private static long getFileSize(File googleFile) {
		return googleFile.getFileSize() == null ? 0 : googleFile.getFileSize();
	}

	public boolean isRemovable() {
		return !"root".equals(getId());
	}

	public String getOwnerName() {
		return "netradius";
	}

}
