Setting up authentication for application to access the drive content
=====================================================================

To Tun the project you will need to authorize the application to access drive which you want to ftp files to/from.
Please follow below steps to generate the authorization config file:

1. go to https://console.developers.google.com/apis/library
2. From dropdown at top right click on create a project or select one if already there.
3. Click on credential on left menu
4. from the new credentials dropdown select OAuthClientID
5. On next screen select "Other" and click create, click OK on pop up
6. From the list under "OAuth 2.0 client IDs", click the download button corresponding to name, you just created
7. save the file and copy the content of the file to \ftp2google\src\main\resources\client_secrets.json file.
8. When running the project, will be asked first time to allow application to access the drive

Run ftp server from command line
================================

Ex: java -jar path/to/jar/ftp2google.jar path/to/properties/file/configuration.properties


FTP Client
==========

1) Upload file:

    			FTPClient ftpClient = new FTPClient();
    			ftpClient.connect("server/localhost", port);
    			ftpClient.login("username", "password");
    			ftpClient.enterLocalPassiveMode();
    			File downloadFile = new File("filepath");
    			ftpClient.appendFile("file-name-to-upload", FileUtils.openInputStream(downloadFile));


2) Download file

    			FTPClient ftpClient = new FTPClient();
    			ftpClient.connect("server/localhost", port);
    			ftpClient.login("username", "password");
    			ftpClient.enterLocalPassiveMode();
    			File downloadFile = new File("filepath");
    			OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile));
    			boolean success = ftpClient.retrieveFile("file-name-on-drive", outputStream);
                outputStream1.flush();
    			outputStream1.close();
