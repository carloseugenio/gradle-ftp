package br.net.carloseugenio.gradle.ftp

import org.apache.commons.net.ftp.FTPReply
import org.junit.After
import org.junit.Before
import org.mockftpserver.core.command.StaticReplyCommandHandler
import org.mockftpserver.fake.FakeFtpServer
import org.mockftpserver.fake.UserAccount
import org.mockftpserver.fake.filesystem.DirectoryEntry
import org.mockftpserver.fake.filesystem.FileEntry
import org.mockftpserver.fake.filesystem.FileSystem
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem
import org.mockftpserver.fake.filesystem.WindowsFakeFileSystem

class FtpServer {

	private FakeFtpServer fakeFtpServer

	String username = "user"
	String password = "password"

	private FileSystem createFileSystem(boolean linux) {
		if (linux) {
			return new UnixFakeFileSystem()
		} else {
			return new WindowsFakeFileSystem()
		}
	}

	private void makeDirectory(FileSystem fs, String rootPath) {
		fs.add(new DirectoryEntry(rootPath + "data"))
	}

	private void addFile(FileSystem fs, String rootPath, String fileName, String data) {
		String separator = (fs instanceof UnixFakeFileSystem) ? "/" : "\\"
		String filePath = rootPath + separator + fileName
		fs.add(new FileEntry(filePath, data))
	}

	void init() {
		fakeFtpServer = new FakeFtpServer()
		fakeFtpServer.setServerControlPort(8080)
		fakeFtpServer.addUserAccount(new UserAccount(username, password, "/"))

		FileSystem fs = createFileSystem(true)

		makeDirectory(fs, "/")
		addFile(fs, "/data", "file1.txt", "file 1 test data")
		addFile(fs, "/data", "file2.txt", "file 2 test data")
		addFile(fs, "/data/subdir", "file3-subdir.txt", "file 3 on sub dir data")
		fakeFtpServer.setFileSystem(fs)

		fakeFtpServer.start()
	}

	FileSystem getFileSystem() {
		return fakeFtpServer.getFileSystem()
	}

	def addAuth() {
		StaticReplyCommandHandler authHandler = new StaticReplyCommandHandler(FTPReply.SECURITY_MECHANISM_IS_OK, "Static AUTH OK")
		fakeFtpServer.setCommandHandler("AUTH", authHandler)
	}

	void stop() {
		fakeFtpServer.stop()
	}

}
