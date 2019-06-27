package br.net.carloseugenio.gradle.ftp

import br.net.carloseugenio.gradle.ftp.security.CryptoUtil
import groovy.transform.CompileStatic
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPSClient
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.inject.Inject

@CompileStatic
class FtpDownloadDirectoryTask extends DefaultTask {

    Server serverInfo
    ResourceSpec downloadInfo
	Logger log


	@TaskAction
    void executeTask() {
		log = project.getLogger()
		try {
			// Load the password from secured file
			serverInfo.password = CryptoUtil.loadPassword(serverInfo.username)

			FtpClientHelper helper = new FtpClientHelper(serverInfo: serverInfo, useTls: serverInfo.useTls, project: project)

			log.info "FTP Download task..."
			log.info "DownloadInfo:\n $downloadInfo"

			// Remove the project path
			//extension.sourcesDir = extension.sourcesDir - extension.projectPath

			// Ask the FTP Client to connect and login
			helper.connect()
			helper.login()


			//helper.downloadAll()
			log.info "Remote file specified?: ${downloadInfo.remoteFile}"
			if (downloadInfo.remoteFile) {
				downloadSingleFile(helper.ftpClient, downloadInfo.remoteFile, downloadInfo.localDir)
			} else {
				// Change to the remote source directory
				helper.cd(downloadInfo.remoteDir)
				downloadDirectory(helper.ftpClient, downloadInfo.remoteDir, "", downloadInfo.localDir)
			}

			helper.logout()

			helper.disconnect()
		} catch (Exception ex) {
			throw new GradleException("Exception on ${this} task:\n ${ex}", ex)
		}
    }

    /**
     * Download a whole directory from a FTP server.
     * @param ftpClient an instance of org.apache.commons.net.ftp.FTPClient class.
     * @param parentDir Path of the parent directory of the current directory being
     * downloaded.
     * @param currentDir Path of the current directory being downloaded.
     * @param saveDir path of directory where the whole remote directory will be
     * downloaded and saved.
     * @throws IOException if any network or IO error occurred.
     */
    void downloadDirectory(FTPClient ftpClient, String parentDir,
                                         String currentDir, String saveDir) throws IOException {
        String dirToList = parentDir
        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir
        }
        log.info "Current Dir:  $currentDir"
        FTPFile[] subFiles = ftpClient.listFiles(dirToList)

        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile aFile : subFiles) {
                String currentFileName = aFile.getName()
                log.info "Processin file: $currentFileName"
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    // skip parent directory and the directory itself
                    log.info "Found . or .. SKIP"
                    continue
                }
                String remoteFilePath = parentDir + "/" + currentDir + "/" + currentFileName
                if (currentDir.equals("")) {
                    remoteFilePath = parentDir + "/" + currentFileName
                }
                log.info "New filePath: $remoteFilePath"
                if (aFile.isDirectory()) {
                    log.info "The file [$aFile] is a directory!"
                    // download the sub directory
                    downloadDirectory(ftpClient, dirToList, currentFileName, saveDir)
                } else {
                    // download the file
					boolean success = downloadSingleFile(ftpClient, remoteFilePath, saveDir)
                    if (success) {
                        log.info("DOWNLOADED the file: " + remoteFilePath)
                    } else {
                        log.info("COULD NOT download the file: " + remoteFilePath)
                    }
                }
            }
        }
    }

    /**
     * ResourceSpec a single file from the FTP server
     * @param ftpClient an instance of org.apache.commons.net.ftp.FTPClient class.
     * @param remoteFilePath path of the file on the server
     * @param savePath path of directory where the file will be stored
     * @return true if the file was downloaded successfully, false otherwise
     * @throws IOException if any network or IO error occurred.
     */
    boolean downloadSingleFile(FTPClient ftpClient,
							   String remoteFilePath, String savePath) throws IOException {
        log.info "Downloading a single file: $remoteFilePath to $savePath"
        File downloadDir = new File(savePath)
		downloadDir.mkdirs()
		File localDownloadedFile = new File(downloadDir, remoteFilePath)
		localDownloadedFile.parentFile.mkdirs()
		log.info "Final File location: ${localDownloadedFile.getAbsolutePath()} "

        OutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(localDownloadedFile))
        try {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            return ftpClient.retrieveFile(remoteFilePath, outputStream)
        } catch (IOException ex) {
            throw ex
        } finally {
            if (outputStream != null) {
                outputStream.close()
            }
        }
    }

}
