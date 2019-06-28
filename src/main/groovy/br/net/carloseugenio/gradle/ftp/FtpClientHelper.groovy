package br.net.carloseugenio.gradle.ftp

import groovy.transform.CompileStatic
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import org.gradle.api.Project
import org.gradle.api.logging.Logger

@CompileStatic
class FtpClientHelper {

    Project project
    FTPClient ftpClient
    boolean connected = false
    boolean authenticated = false
    String currentDir
    Server serverInfo
    boolean useTls = false
    Logger log

    def connect() {
        log = project.getLogger()
        log.info "Connecting to server/port: " + serverInfo.host + ":" + serverInfo.port
        log.info "UseTLS: ${useTls}"
        if (useTls) {
            ftpClient = new FTPSClient(false)
        } else {
            ftpClient = new FTPClient()
        }
        // Connect to server
        ftpClient.connect(serverInfo.host, serverInfo.port)
        log.info "Connected to server!"

        int reply = ftpClient.getReplyCode()
        log.info "Reply code: " + reply

        if (FTPReply.isPositiveCompletion(reply)) {
            log.info "Reply is Ok. Connected!"
            connected = true
        } else {
            log.info "Could not connect!"
            showServerReply()
            connected = false
        }
    }

    def login() {
        if (!connected) {
            log.info "Can't login. Not connected!"
            return
        }
        int len = serverInfo.password.length()
        def maskedPass = serverInfo.password[0..1] + ('*' * len - 3) + serverInfo.password[len-2..len-1]
        log.info "Trying to login with credentials: $maskedPass"
        // Login
        if (ftpClient.login(serverInfo.username, serverInfo.password)) {
            log.info "Login success!"
            showServerReply()
            if (useTls) {
                // Set protection buffer size
                ((FTPSClient)ftpClient).execPBSZ(0)
                // Set data channel protection to private
                ((FTPSClient)ftpClient).execPROT("P")
            }
            // Enter local passive mode
            ftpClient.enterLocalPassiveMode()
            authenticated = true
        } else {
            log.info "Could not login!"
            showServerReply()
            authenticated = false
        }
    }

    def cd(String path) {
        if (!authenticated) {
            log.info "Can't changed dir. Not authenticated!"
            return
        }
        log.info "Changing to directory: ${path}"
        boolean changed = ftpClient.changeWorkingDirectory(path)
        if (changed) {
            log.info "Dir is now: $path"
            currentDir = path
        } else {
            log.info "Couldn't change to dir: $path"
        }
        showServerReply()
    }

    def downloadAll() {
        if (!authenticated) {
            log.info "Can't download. Not authenticated!"
            return
        }
        FTPFile[] files = ftpClient.listFiles()
        files.each { FTPFile file ->
            log.info "File on ($currentDir) : ${file.name}"
        }
        files.each { FTPFile f ->
            if (f.isFile()) {
                new File(currentDir,f.name).withOutputStream { stream ->
                    log.info "Getting $f.name"
                    boolean success = ftpClient.retrieveFile(f.name, stream)
                    if (success) {
                        log.info "Rtr success!"
                    } else {
                        log.info "Rtr errr!"
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
        log.info "Logged out."
    }

    def disconnect() {
        ftpClient.disconnect()
        showServerReply()
        log.info "Disconnected."
    }

    def showServerReply() {
        String[] replies = ftpClient.getReplyStrings()
        if (replies != null && replies.length > 0) {
            replies.each {
                log.info "[Server-Reply]: " + it
            }
        }
    }

}
