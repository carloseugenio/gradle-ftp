package br.net.carloseugenio.gradle.ftp


import groovy.transform.CompileStatic
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

@CompileStatic
class FtpUploadDirectoryTask extends DefaultTask {

    Server serverInfo
    ResourceSpec uploadInfo

    @TaskAction
    void executeTask() {

        // Load the password from secured file
        serverInfo.password = br.net.carloseugenio.gradle.ftp.security.CryptoUtil.loadPassword(serverInfo.username)

        FtpClientHelper helper = new FtpClientHelper(serverInfo: serverInfo)

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
        if (uploadInfo.remoteFile) {
            uploadSingleFile(helper.ftpClient, uploadInfo.remoteDir, uploadInfo.localFile)
        } else {
            uploadDirectory(helper.ftpClient, uploadInfo.remoteDir, uploadInfo.localDir, "")
        }

        helper.logout()

        helper.disconnect()

    }

/**
 * Upload a whole directory (including its nested sub directories and files)
 * to a FTP server.
 *
 * @param ftpClient
 *            an instance of org.apache.commons.net.ftp.FTPClient class.
 * @param remoteDirPath
 *            Path of the destination directory on the server.
 * @param localParentDir
 *            Path of the local directory being uploaded.
 * @param remoteParentDir
 *            Path of the parent directory of the current directory on the
 *            server (used by recursive calls).
 * @throws IOException
 *             if any network or IO error occurred.
 */
    void uploadDirectory(FTPClient ftpClient,
                                       String remoteDirPath, String localParentDir, String remoteParentDir)
            throws IOException {

        println "Received command upload directory for: "
        println "RemoteDirPath: $remoteDirPath"
        println "LocalParentDir: $localParentDir"
        println "RemoteParentDir: $remoteParentDir"

        println "LISTING directory: " + localParentDir

        File localDir = new File(localParentDir)
        println "LocalDir: $localDir"
        println "localDir.name: $localDir.name"
        // Create the remote directory to upload if it does not exists
        String remoteFolder = localDir.name
        createRemoteDir(ftpClient, remoteFolder)

        File[] subFiles = localDir.listFiles()
        println "SubFiles of localDir: $subFiles.length"
        if (subFiles != null && subFiles.length > 0) {
            for (File item : subFiles) {
                println "Current SubFile: $item"
                String remoteFilePath = remoteDirPath + "/" + remoteParentDir + "/" + item.getName()
                println "Considering remoteFilePath: $remoteFilePath"
                if (remoteParentDir == "") {
                    //remoteFilePath = remoteDirPath + "/" + item.getName()
                    remoteDirPath + "/" + remoteFolder + "/" + item.getName()
                }
                println "Final remoteFilePath: $remoteFilePath"
                if (item.isFile()) {
                    println "The item is a file!"
                    // upload the file
                    String localFilePath = item.getAbsolutePath()
                    println "Uploading local file: $localFilePath to remote $remoteFilePath"
                    println "About to upload the file: " + localFilePath
                    boolean uploaded = uploadSingleFile(ftpClient,
                            remoteFilePath, localFilePath)
                    if (uploaded) {
                        println "UPLOADED a file to: " + remoteFilePath
                    } else {
                        println "COULD NOT upload the file: " + localFilePath
                    }
                } else {
                    println "The item is a directory!"
                    createRemoteDir(ftpClient, remoteFilePath)
                    println "Trying to upload the sub directory..."
                    String parent = remoteParentDir + "/" + item.getName()
                    println "Considering parent dir: $parent"
                    if (remoteParentDir == "") {
                        parent = item.getName()
                    }
                    println "Final parent dir: $parent"
                    localParentDir = item.getAbsolutePath()
                    println "Local parent Dir: $localParentDir"
                    uploadDirectory(ftpClient, remoteDirPath, localParentDir,
                            parent)
                }
            }
        }
    }

    private void createRemoteDir(FTPClient ftpClient, String remoteFilePath) {
        // create directory on the server
        println "Creating a directory named: $remoteFilePath on server"
        println "Working directory: " + ftpClient.printWorkingDirectory()
        boolean created = ftpClient.makeDirectory(remoteFilePath)
        if (created) {
            println "CREATED the directory: " + remoteFilePath
        } else {
            println "COULD NOT create the directory: " + remoteFilePath
        }
    }

    /**
     * upload a single file to the FTP server
     * @param ftpClient an instance of org.apache.commons.net.ftp.FTPClient class.
     * @param remoteFilePath path of the file on the server
     * @param localPath path of directory where the file is stored
     * @return true if the file was uploaded successfully, false otherwise
     * @throws IOException if any network or IO error occurred.
     */
    boolean uploadSingleFile(FTPClient ftpClient,
                             String remoteFilePath, String localPath) throws IOException {
        println "Uploading a single file: $localPath to remote path $remoteFilePath ..."
        File uploadFile = new File(localPath)

        if (!uploadFile.exists()) {
            throw new IllegalArgumentException("The provided file $uploadFile does not exists!")
        }

        String fileName = uploadFile.toPath().getFileName()

        InputStream inputStream = new BufferedInputStream(
                new FileInputStream(uploadFile))
        try {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            String remoteFile = remoteFilePath + fileName
            println "Uploading to remote file: $remoteFile"
            return ftpClient.storeFile(remoteFile, inputStream)
        } catch (IOException ex) {
            throw ex
        } finally {
            if (inputStream != null) {
                inputStream.close()
            }
        }
    }

}
