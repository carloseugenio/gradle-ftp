package br.net.carloseugenio.gradle.ftp

import groovy.transform.CompileStatic
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient

@CompileStatic
class FtpClientHelper {

    FTPSClient ftpClient = new FTPSClient(false)
    boolean connected = false
    boolean authenticated = false
    String currentDir
    Server serverInfo

    def connect() {
        println "Connecting to server/port: " + serverInfo.host + "/" + serverInfo.port

        // Connect to server
        ftpClient.connect(serverInfo.host, serverInfo.port)
        println "Connected to server!"

        int reply = ftpClient.getReplyCode()
        println "Reply code: " + reply

        if (FTPReply.isPositiveCompletion(reply)) {
            println "Reply is Ok. Connected!"
            connected = true
        } else {
            println "Could not connect!"
            showServerReply()
            connected = false
        }
    }

    def login() {
        if (!connected) {
            println "Can't login. Not connected!"
            return
        }
        int len = serverInfo.password.length()
        def maskedPass = serverInfo.password[0..1] + ('*' * len - 3) + serverInfo.password[len-2..len-1]
        println "Trying to login with credentials: $maskedPass"
        // Login
        if (ftpClient.login(serverInfo.username, serverInfo.password)) {
            println "Login success!"
            //showServerReply(ftpClient)
            // Set protection buffer size
            ftpClient.execPBSZ(0)
            // Set data channel protection to private
            ftpClient.execPROT("P")
            // Enter local passive mode
            ftpClient.enterLocalPassiveMode()
            authenticated = true
        } else {
            println "Could not login!"
            showServerReply()
            authenticated = false
        }
    }

    def cd(String path) {
        if (!authenticated) {
            println "Can't changed dir. Not authenticated!"
            return
        }
        boolean changed = ftpClient.changeWorkingDirectory(path)
        if (changed) {
            println "Dir is now: $path"
            currentDir = path
        } else {
            println "Couldn't change to dir: $path"
        }
        showServerReply()
    }

    def downloadAll() {
        if (!authenticated) {
            println "Can't download. Not authenticated!"
            return
        }
        FTPFile[] files = ftpClient.listFiles()
        files.each { FTPFile file ->
            println "File on ($currentDir) : ${file.name}"
        }
        files.each { FTPFile f ->
            if (f.isFile()) {
                new File(currentDir,f.name).withOutputStream { stream ->
                    println "Getting $f.name"
                    boolean success = ftpClient.retrieveFile(f.name, stream)
                    if (success) {
                        println "Rtr success!"
                    } else {
                        println "Rtr errr!"
                    }
                    showServerReply()
                }

                InputStream fin = ftpClient.retrieveFileStream(f.getName())

            }
        }
    }

    def logout() {
        // Logout
        ftpClient.logout()
        showServerReply()
        println "Logged out."
    }

    def disconnect() {
        ftpClient.disconnect()
        showServerReply()
        println "Disconnected."
    }

    def showServerReply() {
        String[] replies = ftpClient.getReplyStrings()
        if (replies != null && replies.length > 0) {
            replies.each {
                println "[Server-Reply]: " + it
            }
        }
    }

}
