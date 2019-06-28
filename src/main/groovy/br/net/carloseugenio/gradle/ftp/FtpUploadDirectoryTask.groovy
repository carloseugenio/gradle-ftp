package br.net.carloseugenio.gradle.ftp

import br.net.carloseugenio.gradle.ftp.security.CryptoUtil
import groovy.transform.CompileStatic
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger

@CompileStatic
class FtpUploadDirectoryTask extends DefaultTask {

    Server serverInfo
    ResourceSpec uploadInfo
    Logger log
    FtpClientHelper helper

    @TaskAction
    void executeTask() {
        log = project.getLogger()
        try {
            // Load the password from secured file
            serverInfo.password = CryptoUtil.loadPassword(serverInfo.username)

            helper = new FtpClientHelper(serverInfo: serverInfo, useTls: serverInfo.useTls, project: project)

            println "FTP upload task..."
            println "uploadInfo:\n $uploadInfo"

            // Remove the project path
            //extension.sourcesDir = extension.sourcesDir - extension.projectPath

            // Ask the FTP Client to connect and login
            helper.connect()
            helper.login()

            // Change to the remote source directory
            helper.cd(uploadInfo.remoteDir)

            //helper.downloadAll()
            //uploadDirectory(helper.ftpClient, uploadInfo.remoteDir, "", uploadInfo.localDir)
            if (uploadInfo.localFile) {
                uploadSingleFile(helper.ftpClient, uploadInfo.remoteDir, uploadInfo.localFile)
            } else {
                uploadDirectory(helper.ftpClient, uploadInfo.remoteDir, uploadInfo.localDir, "")
            }

            helper.logout()

            helper.disconnect()
        } catch (Exception ex) {
            throw new GradleException("Exception on ${this} task:\n ${ex}", ex)
        }

    }

/**
 * Upload a whole directory (including its nested sub directories and files)
 * to a FTP server.
 *
 * @param ftpClient
 *            an instance of org.apache.commons.net.ftp.FTPClient class.
 * @param remoteDirPath
 *            Path of the destination directory on the server.
 * @param localDirPath
 *            Path of the local directory being uploaded.
 * @param remoteParentDir
 *            Path of the parent directory of the current directory on the
 *            server (used by recursive calls).
 * @throws IOException
 *             if any network or IO error occurred.
 */
    void uploadDirectory(FTPClient ftpClient, String remoteDirPath, String localDirPath, String remoteParentDir)
            throws IOException {

        println "Received command -> upload directory"
        println "RemoteDirPath: $remoteDirPath"
        println "LocalParentDir: $localDirPath"
        println "RemoteParentDir: $remoteParentDir"

        println "LISTING directory: " + localDirPath

        File localDir = new File(localDirPath)
        println "LocalDir: $localDir"
        println "localDir.name: $localDir.name"
        // Create the remote directory to upload if it does not exists
        // and change the current work directory
        String remoteFolder = localDir.name
        createRemoteDir(ftpClient, remoteFolder)
        // The new remote directory will be the old plus the new folder
        remoteDirPath = remoteDirPath + File.separator + remoteFolder
        println "New RemoteDirPath: $remoteDirPath"
        File[] subFiles = localDir.listFiles()
        println "SubFiles of localDir: $subFiles.length"
        if (subFiles != null && subFiles.length > 0) {
            for (File item : subFiles) {
                println "Current SubFile: $item"
                /*String remoteFilePath = remoteDirPath + "/" + remoteParentDir + "/" + item.getName()
                println "Considering remoteFilePath: $remoteFilePath"
                if (remoteParentDir == "") {
                    remoteDirPath + "/" + remoteFolder + "/" + item.getName()
                }
                println "Final remoteFilePath: $remoteFilePath"
                 */
                if (item.isFile()) {
                    println "The item is a file!"
                    // upload the file
                    String localFilePath = item.getPath()
                    //println "Uploading local file: $localFilePath to remote $remoteFilePath"
                    println "Uploading local file: $localFilePath to remote dir $remoteDirPath"
                    boolean uploaded = uploadSingleFile(ftpClient, remoteDirPath, localFilePath)
                    if (uploaded) {
                        println "File successfully UPLOADED to: " + remoteDirPath
                    } else {
                        println "COULD NOT upload the file: " + localFilePath
                    }
                } else {
                    println "The item is a directory!"
                    remoteFolder = item.name
                    String newLocalDirPath = localDirPath + File.separator + remoteFolder
                    println "Trying to upload the sub directory to server ..."
                    /*
                    String parent = remoteParentDir + "/" + item.getName()
                    println "Considering parent dir: $parent"
                    if (remoteParentDir == "") {
                        parent = item.getName()
                    }
                    println "Final parent dir: $parent"
                    localDirPath = item.getAbsolutePath()
                    println "Local parent Dir: $localDirPath"
                     */
                    //uploadDirectory(ftpClient, remoteDirPath, localDirPath, parent)
                    uploadDirectory(ftpClient, remoteDirPath, newLocalDirPath, "")
                }
            }
        }
    }

    private void createRemoteDir(FTPClient ftpClient, String remoteFilePath) {
        // create directory on the server
        println "Creating a directory named: $remoteFilePath on server"
        println "Working directory: " + ftpClient.printWorkingDirectory()
        boolean created = ftpClient.makeDirectory(remoteFilePath)
        helper.showServerReply()
        if (created) {
            println "CREATED the directory: " + remoteFilePath
        } else {
            println "COULD NOT create the directory: " + remoteFilePath
        }
        println "Changing to directory: $remoteFilePath"
        ftpClient.changeWorkingDirectory(remoteFilePath)
        helper.showServerReply()
    }

    /**
     * upload a single file to the FTP server
     * @param ftpClient an instance of org.apache.commons.net.ftp.FTPClient class.
     * @param remoteDirPath directory path where the file will be stored on server
     * @param localPath path of directory where the file is stored
     * @return true if the file was uploaded successfully, false otherwise
     * @throws IOException if any network or IO error occurred.
     */
    boolean uploadSingleFile(FTPClient ftpClient,
                             String remoteDirPath, String localPath) throws IOException {
        println "Uploading a single file: $localPath to remote path $remoteDirPath ..."
        File uploadFile = new File(localPath)

        if (!uploadFile.exists()) {
            throw new IllegalArgumentException("The provided file $uploadFile does not exists!")
        }

        String fileName = uploadFile.toPath().getFileName()

        InputStream inputStream = new BufferedInputStream(
                new FileInputStream(uploadFile))
        try {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            String remoteFile = remoteDirPath + File.separator + fileName
            println "Uploading to remote file: $remoteFile"
            boolean success =  ftpClient.storeFile(remoteFile, inputStream)
            helper.showServerReply()
            if (success) {
                log.info "Uploaded single file ${fileName} to remote dir: ${remoteDirPath}"
            } else {
                log.info "Failed to upload ${fileName} to remote dir: ${remoteDirPath}"
            }
            return success
        } catch (IOException ex) {
            throw ex
        } finally {
            if (inputStream != null) {
                inputStream.close()
            }
        }
    }

}
