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
class FtpListDirectoryTask extends DefaultTask {

    Server serverInfo
    ResourceSpec listInfo
    Logger log
    FtpClientHelper helper

    @TaskAction
    void executeTask() {
        log = project.getLogger()
        try {
            // Load the password from secured file
            serverInfo.password = CryptoUtil.loadPassword(serverInfo.username)

            helper = new FtpClientHelper(serverInfo: serverInfo, useTls: serverInfo.useTls, project: project)

            println "FTP list task..."
            println "listInfo:\n $listInfo"

            // Remove the project path
            //extension.sourcesDir = extension.sourcesDir - extension.projectPath

            // Ask the FTP Client to connect and login
            helper.connect()
            helper.login()

            // Change to the remote source directory
            helper.cd(listInfo.remoteDir)

            //helper.downloadAll()
            //uploadDirectory(helper.ftpClient, listInfo.remoteDir, "", listInfo.localDir)
            listDirectory(helper.ftpClient, listInfo.remoteDir, listInfo.localDir, "")

            helper.logout()

            helper.disconnect()
        } catch (Exception ex) {
            throw new GradleException("Exception on ${this} task:\n ${ex}", ex)
        }

    }

/**
 * List a whole directory (including its nested sub directories and files)
 * in a FTP server.
 *
 * @param ftpClient
 *            an instance of org.apache.commons.net.ftp.FTPClient class.
 * @param remoteDirPath
 *            Path of the destination directory on the server.
 * @param localDirPath
 *            Path of the local directory being listed.
 * @param remoteParentDir
 *            Path of the parent directory of the current directory on the
 *            server (used by recursive calls).
 * @throws IOException
 *             if any network or IO error occurred.
 */
    void listDirectory(FTPClient ftpClient, String remoteDirPath, String localDirPath, String remoteParentDir)
            throws IOException {

        println "Received command -> list directory"
        println "RemoteDirPath: $remoteDirPath"
        println "LocalParentDir: $localDirPath"
        println "RemoteParentDir: $remoteParentDir"

        println "LISTING directory: " + localDirPath

        File localDir = new File(localDirPath)
        println "LocalDir: $localDir"
        println "localDir.name: $localDir.name"
        File[] subFiles = localDir.listFiles()
        println "SubFiles of localDir: $subFiles.length"
        if (subFiles != null && subFiles.length > 0) {
            for (File item : subFiles) {
                println "Current SubFile: $item"
                if (item.isFile()) {
                    println "The item is a file!"
                    // current file
                    String localFilePath = item.getPath()
                    //println "Listing local file: $localFilePath to remote $remoteFilePath"
                    println "Echo local file: $localFilePath to remote dir $remoteDirPath"
                    new File(remoteDirPath) << localFilePath
                } else {
                    println "The item is a directory!"
                    String newLocalDirPath = localDirPath + File.separator + item.name
                    println "Trying to list the sub directory to server ..."
                    listDirectory(ftpClient, remoteDirPath, newLocalDirPath, "")
                }
            }
        }
    }


}
