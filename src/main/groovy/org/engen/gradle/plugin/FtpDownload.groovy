package org.engen.gradle.plugin

class FtpDownload {

    def download() {
        println "Using fileCollection: ${fileCollection}"
        if (fileCollection == null) {
            throw new IllegalArgumentException("File collection can't be null!")
        }

        fileCollection.each { File f ->
            // Do not upload these files
            if (f.name.endsWith(".jrxml")) {
                return
            }

            println "Going to root dir: ${remoteDir}"
            boolean changed = ftpClient.changeWorkingDirectory(remoteDir)
            if (!changed) {
                throw new RuntimeException("Couldn't go to root remote dir: " + remoteDir)
            }

            def localFilename = "$f"
            println "Uploading file: $localFilename"

            println "Current projectPath: ${projectPath}"
            def remotePath = localFilename - projectPath
            remotePath = remotePath - localDir
            remotePath = remotePath - sourcesDir
            println "RemotePath final: $remotePath"

            def remoteFilename = "${remoteDir}$remotePath"
            println "Remote Destination :${remoteFilename}"


            // Make sure the path exists
            buildDirs(ftpClient, remotePath - f.name)

            // Store file on server
            InputStream is = new FileInputStream(localFilename)
            if (ftpClient.storeFile(remoteFilename, is)) {
                is.close()
            } else {
                println("Could not store file: " + localFilename)
                showServerReply(ftpClient)
                is.close()
                throw new RuntimeException("Server reported error. See above messages")
            }
        }

    }
}
