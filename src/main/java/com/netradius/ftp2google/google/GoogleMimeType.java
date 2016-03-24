package com.netradius.ftp2google.google;

/**
 * @author Erik R. Jensen
 */
public enum GoogleMimeType {

	GOOGLE_AUDIO("application/vnd.google-apps.audio", "audio"),
	GOOGLE_DOC("application/vnd.google-apps.document", "Google Docs"),
	GOOGLE_DRAW("application/vnd.google-apps.drawing", "Google Drawing"),
	GOOGLE_FILE("application/vnd.google-apps.file", "Google Drive file"),
	GOOGLE_FOLDER("application/vnd.google-apps.folder", "Google Drive folder"),
	GOOGLE_FORM("application/vnd.google-apps.form", "Google Forms"),
	GOOGLE_FUSION("application/vnd.google-apps.fusiontable", "Google Fusion Tables"),
	GOOGLE_PHOTO("application/vnd.google-apps.photo", "photo"),
	GOOGLE_SLIDE("application/vnd.google-apps.presentation", "Google Slides"),
	GOOGLE_PPT("application/vnd.google-apps.script", "Google Apps Scripts"),
	GOOGLE_SITE("application/vnd.google-apps.sites", "Google Sites"),
	GOOGLE_SHEET("application/vnd.google-apps.spreadsheet", "Google Sheets"),
	GOOGLE_UNKNOWN("application/vnd.google-apps.unknown", "unknown"),
	GOOGLE_VIDEO("application/vnd.google-apps.video", "video");

	private final String value;
	private final String desc;

	GoogleMimeType(String value, String desc) {
		this.value = value;
		this.desc = desc;
	}

	public String getValue() {
		return value;
	}

	public String getDesc() {
		return desc;
	}
}
