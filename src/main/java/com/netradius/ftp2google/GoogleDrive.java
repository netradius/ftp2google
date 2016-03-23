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
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * @author Dilip Sisodia
 * @author Erik Jensen
 */
@Slf4j
public class GoogleDrive {

	private GoogleClientSecrets secrets;
	private JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
	private HttpTransport httpTransport;
	private Credential credential;
	private Drive drive;
	private DataStoreFactory dataStoreFactory;

	public GoogleDrive(GoogleClientSecrets secrets, Properties configuration) {

		this.secrets = secrets;

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
		Set<String> scopes = new HashSet<String>();
		scopes.add(DriveScopes.DRIVE);
		scopes.add(DriveScopes.DRIVE_METADATA);

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory, secrets, scopes)
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


	void getFileDownloadURL(GoogleFile file) {
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

	java.io.File downloadFile(GoogleFile file) {
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


	public File uploadFile(GoogleFile fileToUpload ) {
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


	public InputStream createInputStream(GoogleFile file) {
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

	public OutputStream createOutputStream(final GoogleFile fTPGoogleFileW) {
		final GoogleFile fTPGoogleFile = fTPGoogleFileW;
		OutputStream transferFileOutputStream;
		try {
			final java.io.File transferFile = java.io.File.createTempFile("gdrive-synch-", ".upload." + fTPGoogleFile.getName());
			fTPGoogleFile.setTransferFile(transferFile);
			transferFileOutputStream = new FileOutputStream(transferFile) {
				@Override
				public void close() throws IOException {
					com.google.api.services.drive.model.File updatedGoogleFile = null;
					super.close();
					try {
							updatedGoogleFile = uploadFile(fTPGoogleFile);
					} finally {
						FileUtils.deleteQuietly(fTPGoogleFile.getTransferFile());
					}
				}
			};
			return transferFileOutputStream;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


}
