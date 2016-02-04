package com.netradius.ftp2google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import com.google.api.services.drive.model.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * @author Dilip Sisodia
 */

public class GoogleDrive {
	private static final Log LOG = LogFactory.getLog(GFile.class);

	private JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
	private HttpTransport httpTransport;
	private Credential credential;
	private Drive drive;
	private DataStoreFactory dataStoreFactory;

	public static class GFile implements Serializable, Cloneable {

		public static enum MIME_TYPE {

			GOOGLE_AUDIO("application/vnd.google-apps.audio", "audio"), GOOGLE_DOC("application/vnd.google-apps.document", "Google Docs"), GOOGLE_DRAW(
					"application/vnd.google-apps.drawing", "Google Drawing"), GOOGLE_FILE("application/vnd.google-apps.file",
					"Google  Drive file"), GOOGLE_FOLDER("application/vnd.google-apps.folder", "Google  Drive folder"), GOOGLE_FORM(
					"application/vnd.google-apps.form", "Google  Forms"), GOOGLE_FUSION("application/vnd.google-apps.fusiontable",
					"Google  Fusion Tables"), GOOGLE_PHOTO("application/vnd.google-apps.photo", "photo"), GOOGLE_SLIDE(
					"application/vnd.google-apps.presentation", "Google  Slides"), GOOGLE_PPT("application/vnd.google-apps.script",
					"Google  Apps Scripts"), GOOGLE_SITE("application/vnd.google-apps.sites", "Google  Sites"), GOOGLE_SHEET(
					"application/vnd.google-apps.spreadsheet", "Google  Sheets"), GOOGLE_UNKNOWN("application/vnd.google-apps.unknown",
					"unknown"), GOOGLE_VIDEO("application/vnd.google-apps.video", "video");

			private final String value;
			private final String desc;
			static Map<String, String> list = new HashMap<String,String>();

			MIME_TYPE(String value, String desc) {
				this.value = value;
				this.desc = desc;
			}

			public String getValue() {
				return value;
			}

			public String getDesc() {
				return desc;
			}

			public static MIME_TYPE parse(String mimeType) {
				for (MIME_TYPE a : MIME_TYPE.values()) {
					if (a.getValue().equals(mimeType)) {
						return a;
					}
				}
				return null;
			}
		};

		private static final long serialVersionUID = 1L;

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

		private transient GFile currentParent;

		private boolean exists;

		public GFile() {
			this("");
		}

		public GFile(String name) {
			this.name = name;
		}

		public Set<String> getParents() {
			return parents;
		}

		public void setParents(Set<String> parents) {
			this.parents = parents;
		}

		public GFile(Set<String> parents, String name) {
			this(name);
			this.parents = parents;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setRevision(long largestChangeId) {
			this.revision = largestChangeId;
		}

		public void setLength(long length) {
			this.setSize(length);
		}

		public void setDirectory(boolean isDirectory) {
			this.isDirectory = isDirectory;
		}

		public boolean isDirectory() {
			return isDirectory;
		}

		public String getMd5Checksum() {
			return md5Checksum;
		}

		public void setMd5Checksum(String md5Checksum) {
			this.md5Checksum = md5Checksum;
		}

		public Set<String> getLabels() {
			return labels;
		}

		public void setLabels(Set<String> labels) {
			this.labels = labels;
		}

		public void setLastModified(long time) {
			this.lastModified = time;
		}

		public String getName() {
			return name;
		}

		public long getLength() {
			return getSize();
		}

		public long getLastModified() {
			return lastModified;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		public long getRevision() {
			return revision;
		}

		public String toString() {
			return getName() + "(" + getId() + ")";
		}

		public void setDownloadUrl(URL downloadUrl) {
			this.downloadUrl = downloadUrl;
		}

		public URL getDownloadUrl() {
			return downloadUrl;
		}

		public java.io.File getTransferFile() {
			return transferFile;
		}

		public void setTransferFile(java.io.File transferFile) {
			this.transferFile = transferFile;
		}

		public long getLastViewedByMeDate() {
			return lastViewedByMeDate;
		}

		public void setLastViewedByMeDate(long lastViewedByMeDate) {
			this.lastViewedByMeDate = lastViewedByMeDate;
		}

		@Override
		public Object clone() {
			GFile ret = new GFile(getName());
			ret.setId(getId());
			ret.setName(getName());
			ret.setDirectory(isDirectory());
			ret.setLength(getLength());
			ret.setLastModified(getLastModified());
			ret.setMd5Checksum(getMd5Checksum());
			ret.setRevision(getRevision());
			ret.setParents(getParents());

			ret.setMimeType(mimeType);
			ret.setExists(isExists());
			ret.setLastViewedByMeDate(getLastViewedByMeDate());
			return ret;
		}

		public GFile getCurrentParent() {
			return currentParent;
		}

		public void setCurrentParent(GFile currentParent) {
			this.currentParent = currentParent;
		}

		public boolean isExists() {
			return exists;
		}

		public void setExists(boolean exists) {
			this.exists = exists;
		}

		public long getSize() {
			return size;
		}

		public void setSize(long size) {
			this.size = size;
		}

		public static GFile create(File googleFile) {
			if (googleFile == null)
				return null;
			GFile newFile = new GFile(getFilename(googleFile));
			newFile.setId(googleFile.getId());
			newFile.setLastModified(getLastModified(googleFile));
			newFile.setLength(getFileSize(googleFile));
			newFile.setDirectory(isDirectory(googleFile));
			newFile.setMd5Checksum(googleFile.getMd5Checksum());
			newFile.setParents(new HashSet<String>());
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

		public static List<GFile> create(List<File> googleFiles, long revision) {
			List<GFile> ret = new ArrayList<GFile>(googleFiles.size());
			for (File child : googleFiles) {
				GFile localFile = create(child);
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

	public GoogleDrive(Properties configuration) {

		java.io.File DATA_STORE_DIR = new java.io.File("data/google/" + configuration.getProperty("account", "default"));

		try {
			dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();

		} catch (Exception e) {
			throw new RuntimeException("Could not initialize Google API.");
		}
		init();
	}

	private void init() {
		try {
			credential = authorize();
			drive = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName("ftp2google").build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Credential authorize() throws Exception {
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory,
				new InputStreamReader(File.class.getResourceAsStream("/client_secrets.json")));
		if (clientSecrets.getDetails().getClientId().startsWith("Enter")
				|| clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
			System.exit(1);
		}
		Set<String> scopes = new HashSet<String>();
		scopes.add(DriveScopes.DRIVE);
		scopes.add(DriveScopes.DRIVE_METADATA);

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, clientSecrets, scopes)
				.setDataStoreFactory(dataStoreFactory).build();
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	}

	public File getFile(String fileId) {
		try {
			File file = drive.files().get(fileId).execute();
			return file;
		} catch (GoogleJsonResponseException e) {
			if (e.getStatusCode() == 404) {
				return null;
			}
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	void getFileDownloadURL(GFile file) {
		try {
			File googleFile = getFile(file.getId());
			switch (googleFile.getMimeType()) {
				case "application/vnd.google-apps.spreadsheet":
					file.setDownloadUrl(new URL(googleFile.getExportLinks().get(
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
					// file.getExportLinks().get("application/pdf")
					break;
				case "application/vnd.google-apps.document":
					file.setDownloadUrl(new URL(googleFile.getExportLinks().get(
							"application/vnd.openxmlformats-officedocument.wordprocessingml.document")));
					break;
				default:
					if (googleFile != null && googleFile.getDownloadUrl() != null && googleFile.getDownloadUrl().length() > 0) {
						file.setDownloadUrl(new URL(googleFile.getDownloadUrl()));
					} else {
						throw new RuntimeException("");
					}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	java.io.File downloadFile(GFile file) {
		java.io.File ret = null;
		InputStream is = null;
		java.io.File tmpFile = null;
		FileOutputStream tempFos = null;
		try {
			getFileDownloadURL(file);

			if (file.getDownloadUrl() == null) {
				return null;
			}

			HttpResponse resp = drive.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();

			tmpFile = java.io.File.createTempFile("gdrive-", ".download");
			is = resp.getContent();
			tempFos = new FileOutputStream(tmpFile);
			IOUtils.copy(is, tempFos);
			tempFos.flush();
			ret = tmpFile;
			is.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(tempFos);
		}
		return ret;
	}

	File getGDriveFileByName(String searchFileName) {
		try {
			List<File> files = drive.files().list().execute().getItems();
			for (File file: files) {
				if(file.getTitle().equals(searchFileName)) {
					return file;
				}
			}
		}
		catch (Exception ex) {
		}
		return null;
	}


	public File uploadFile(GFile fileToUpload ) {
		try {
			File file = null;

			FileContent mediaContent = null;
			if (!fileToUpload.isExists()) {
				file = new File();
				if (fileToUpload.isDirectory()) {
					file.setMimeType("application/vnd.google-apps.folder");
				}
				mediaContent = new FileContent(java.nio.file.Files.probeContentType(fileToUpload.getTransferFile().toPath()),
						fileToUpload.getTransferFile());
				file.setTitle(fileToUpload.getName());
				file.setModifiedDate(new DateTime(System.currentTimeMillis()));

				List<ParentReference> newParents = new ArrayList<ParentReference>(1);
				if (fileToUpload.getParents() != null) {
					for (String parent : fileToUpload.getParents()) {
						newParents.add(new ParentReference().setId(parent));
					}

				} else {
					newParents = Collections.singletonList(new ParentReference().setId(fileToUpload.getCurrentParent().getId()));
				}
				file.setParents(newParents);

				if (mediaContent == null) {
					file = drive.files().insert(file).execute();
				} else {
					file = drive.files().insert(file, mediaContent).execute();
				}
			}

			return file;
		} catch (IOException e) {

			throw new RuntimeException("File Upload faile.", e);
		}
	}


	public InputStream createInputStream(GFile file) {
		java.io.File transferFile = downloadFile(file);
		if (transferFile == null) {
			throw new IllegalStateException("File does not exists.");
		}

		try {
			InputStream transferFileInputStream = FileUtils.openInputStream(transferFile);
			return transferFileInputStream;
		} catch (IOException ex) {
			return null;
		}

	}

	public OutputStream createOutputStream(final GFile fTPGFileW) {
		final GFile fTPGFile = fTPGFileW;
		OutputStream transferFileOutputStream;
		try {
			final java.io.File transferFile = java.io.File.createTempFile("gdrive-synch-", ".upload." + fTPGFile.getName());
			fTPGFile.setTransferFile(transferFile);
			transferFileOutputStream = new FileOutputStream(transferFile) {
				@Override
				public void close() throws IOException {
					com.google.api.services.drive.model.File updatedGoogleFile = null;
					super.close();
					try {
							updatedGoogleFile = uploadFile(fTPGFile);
					} finally {
						FileUtils.deleteQuietly(fTPGFile.getTransferFile());
					}
				}
			};
			return transferFileOutputStream;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


}
