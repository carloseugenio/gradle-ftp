package br.net.carloseugenio.gradle.ftp

import br.net.carloseugenio.gradle.ftp.security.CryptoUtil
import org.apache.commons.net.ftp.FTPReply
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockftpserver.fake.FakeFtpServer
import org.mockftpserver.fake.UserAccount
import org.mockftpserver.fake.filesystem.DirectoryEntry
import org.mockftpserver.fake.filesystem.FileEntry
import org.mockftpserver.fake.filesystem.FileSystem
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem
import org.mockftpserver.fake.filesystem.WindowsFakeFileSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.mockftpserver.core.command.StaticReplyCommandHandler

import static org.junit.Assert.*

class FtpDownloadTest {

	public @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
	private Logger log = LoggerFactory.getLogger(getClass())
	private FakeFtpServer fakeFtpServer
	File buildFile
	private String username = "user"
	private String password = "password"
	private String destination = "build/test-result/"
	private String debug = "-q"

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

	@Before
	void setup() {
		new File(destination).deleteDir()
		assert !new File(destination).exists()
		new File("user-secure").delete()
		assert !new File("user-secure").exists()
		fakeFtpServer = new FakeFtpServer()
		fakeFtpServer.setServerControlPort(8080)
		fakeFtpServer.addUserAccount(new UserAccount("user", "password", "/"))


		FileSystem fs = createFileSystem(true)

		makeDirectory(fs, "/")
		addFile(fs, "/data", "file1.txt", "file 1 test data")
		addFile(fs, "/data", "file2.txt", "file 2 test data")
		addFile(fs, "/data/subdir", "file3-subdir.txt", "file 3 on sub dir data")
		fakeFtpServer.setFileSystem(fs)

		fakeFtpServer.start()
		buildFile = testProjectDir.newFile('build.gradle')
		buildFile << """
            plugins {
                id 'br.net.carloseugenio.gradle.ftp.gradle-ftp'
            }
        """
	}

	@After
	void tearDown() {
		fakeFtpServer.stop()
		new File(destination).deleteDir()
		new File("user-secure").delete()
	}

	@Test
	void fTPServerAndFileSystemCreated () {
		log.info("Verify existence of file...")
		assert fakeFtpServer.getFileSystem().exists("/data/file1.txt")
	}

	@Test
	void testApplyPlugin() {
		Project project = ProjectBuilder.builder().build()
		project.pluginManager.apply "br.net.carloseugenio.gradle.ftp.gradle-ftp"
		assert project.plugins.size() == 1
	}

	@Test
	void testDownloadTasksCreated() {
		buildFile << """
            ftp {
				FakeServer {
					username = "${username}"
					password = "${password}"
					host = "localhost"
					port = 8080
            
					downloads {
						files {
							remoteDir = "/"
							localDir = "/"
						}
					}
				}
            }
        """

		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('tasks')
				.withPluginClasspath()
				.build()
		result.output.contains("downloadFilesFromFakeServer")
		result.task(":tasks").outcome == "SUCCESS"
	}

	@Test
	void testTasksWithoutPassword() {
		buildFile << """
            ftp {
				FakeServer {
					username = "${username}"
					host = "localhost"
					port = 8080
            
					downloads {
						files {
							remoteDir = "/"
							localDir = "/"
						}
					}
				}
            }
        """

		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('downloadFilesFromFakeServer', '--stacktrace', debug)
				.withPluginClasspath()
		.buildAndFail()
		//log.quiet("Output: $result.output")
	}

	@Test
	void testDownloadDirectory() {
		buildFile << """
            ftp {
				FakeServer {
					username = "${username}"
					password = "${password}"
					host = "localhost"
					port = 8080
            
					downloads {
						files {
							remoteDir = "/data"
							localDir = "${destination}"
						}
					}
				}
            }
        """
		StaticReplyCommandHandler authHandler = new StaticReplyCommandHandler(FTPReply.SECURITY_MECHANISM_IS_OK, "Static AUTH OK")
		fakeFtpServer.setCommandHandler("AUTH", authHandler)
		// Create the user-secure file
		CryptoUtil.savePassword(username, password)
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('downloadFilesFromFakeServer', '--stacktrace', debug)
				.withPluginClasspath()
				.withDebug(true)
				.build()

		//log.quiet("Output: $result.output")
		result.output.contains("downloadFilesFromFakeServer")
		result.task(":downloadFilesFromFakeServer").outcome == "SUCCESS"
		assertEquals(3, new File("${destination}/data").listFiles().length)
		assertEquals(1, new File("${destination}/data/subdir").listFiles().length)
	}

	@Test
	void testDownloadFile() {

		buildFile << """
            ftp {
				FakeServer {
					username = "${username}"
					password = "${password}"
					host = "localhost"
					port = 8080
            
					downloads {
						file {
							remoteFile = "/data/file1.txt"
							localDir = "${destination}"
						}
					}
				}
            }
        """
		StaticReplyCommandHandler authHandler = new StaticReplyCommandHandler(FTPReply.SECURITY_MECHANISM_IS_OK, "Static AUTH OK")
		fakeFtpServer.setCommandHandler("AUTH", authHandler)
		// Create the user-secure file
		CryptoUtil.savePassword(username, password)
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('downloadFileFromFakeServer', '--stacktrace', debug)
				.withPluginClasspath()
				.withDebug(true)
				.build()

		//log.quiet("Output: $result.output")
		result.output.contains("downloadFilesFromFakeServer")
		result.task(":downloadFileFromFakeServer").outcome == "SUCCESS"
		assertEquals(1, new File("${destination}/data").listFiles().length)
	}

}
