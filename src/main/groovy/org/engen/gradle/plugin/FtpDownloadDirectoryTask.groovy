package org.engen.gradle.plugin

import groovy.transform.CompileStatic
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPSClient
import org.engen.gradle.plugin.security.CryptoUtil
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

@CompileStatic
class FtpDownloadDirectoryTask extends DefaultTask {

    Server serverInfo
    ResourceSpec downloadInfo

    @TaskAction
    void executeTask() {

        // Load the password from secured file
        serverInfo.password = CryptoUtil.loadPassword(serverInfo.username)

        FtpClientHelper helper = new FtpClientHelper(serverInfo: serverInfo)

        println "FTP Download task..."
        println "DownloadInfo:\n $downloadInfo"

        // Remove the project path
        //extension.sourcesDir = extension.sourcesDir - extension.projectPath

        // Ask the FTP Client to connect and login
        helper.connect()
        helper.login()

        // Change to the remote source directory
        helper.cd(downloadInfo.remoteDir)

        //helper.downloadAll()
        println "Remote file specified?: ${downloadInfo.remoteFile}"
        if (downloadInfo.remoteFile) {
            downloadSingleFile(helper.ftpClient, downloadInfo.remoteFile, downloadInfo.localDir)
        } else {
            downloadDirectory(helper.ftpClient, downloadInfo.remoteDir, "", downloadInfo.localDir)
        }

        helper.logout()

        helper.disconnect()

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
    void downloadDirectory(FTPSClient ftpClient, String parentDir,
                                         String currentDir, String saveDir) throws IOException {
        String dirToList = parentDir
        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir
        }
        println "Current Dir:  $currentDir"
        FTPFile[] subFiles = ftpClient.listFiles(dirToList)

        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile aFile : subFiles) {
                String currentFileName = aFile.getName()
                println "Processin file: $currentFileName"
                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    // skip parent directory and the directory itself
                    println "Found . or .. SKIP"
                    continue
                }
                String filePath = parentDir + "/" + currentDir + "/" + currentFileName
                if (currentDir.equals("")) {
                    filePath = parentDir + "/" + currentFileName
                }
                println "New filePath: $filePath"
                String newDirPath = saveDir + parentDir + File.separator + currentDir + File.separator + currentFileName
                if (currentDir.equals("")) {
                    newDirPath = saveDir + parentDir + File.separator + currentFileName
                }
                println "New DirPath: $newDirPath"
                if (aFile.isDirectory()) {
                    println "The file [$aFile] is a directory!"
                    // create the directory in saveDir
                    File newDir = new File(newDirPath)
                    boolean created = newDir.mkdirs()
                    if (created) {
                        System.out.println("CREATED the directory: " + newDirPath)
                    } else {
                        System.out.println("COULD NOT create the directory: " + newDirPath)
                    }

                    // download the sub directory
                    downloadDirectory(ftpClient, dirToList, currentFileName, saveDir)
                } else {
                    // download the file
                    boolean success = downloadSingleFile(ftpClient, filePath,
                            newDirPath)
                    if (success) {
                        System.out.println("DOWNLOADED the file: " + filePath)
                    } else {
                        System.out.println("COULD NOT download the file: "
                                + filePath)
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
    boolean downloadSingleFile(FTPSClient ftpClient,
                                             String remoteFilePath, String savePath) throws IOException {
        println "Downloading a single file: $remoteFilePath to $savePath"
        File downloadFile = new File(savePath)

        File parentDir = downloadFile.getParentFile()
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }

        OutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(downloadFile))
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
