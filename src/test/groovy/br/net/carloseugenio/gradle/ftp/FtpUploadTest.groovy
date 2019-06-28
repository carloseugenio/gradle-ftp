package br.net.carloseugenio.gradle.ftp

import br.net.carloseugenio.gradle.ftp.security.CryptoUtil
import org.gradle.testkit.runner.GradleRunner
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockftpserver.fake.filesystem.FileSystemEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.junit.Assert.assertEquals

class FtpUploadTest {

	public @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
	private Logger log = LoggerFactory.getLogger(getClass())
	private FtpServer server
	private TestSetup testSetup

	@Before
	void setup() {
		server = new FtpServer()
		server.init()
		testSetup = new TestSetup(testProjectDir)
		testSetup.createEnv()
		testSetup.createUploads()
	}

	@After
	void tearDown() {
		server.stop()
		testSetup.destroy()
	}

	@Test
	void testUploadFile() {
		testSetup.buildFile << """
            ftp {
				FakeServer {
					username = "${server.username}"
					password = "${server.password}"
					host = "localhost"
					port = 8080
            
					uploads {
						file {
							localFile = "${testSetup.destination}/uploads/upload1.txt"
							remoteDir = "/data"
						}
					}
				}
            }
        """
		server.addAuth()
		// Create the user-secure file
		CryptoUtil.savePassword(server.username, server.password)
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('uploadFileToFakeServer', '--stacktrace', testSetup.gradleLogLevel)
				.withPluginClasspath()
				.withDebug(true)
				.build()

		log.quiet("Output: $result.output")
		result.output.contains("uploadFileToFakeServer")
		result.task(":uploadFileToFakeServer").outcome == "SUCCESS"
		assertEquals(4, server.getFileSystem().listFiles("/data").size())
	}

	@Test
	void testUploadDirectory() {
		testSetup.buildFile << """
            ftp {
				FakeServer {
					username = "${server.username}"
					password = "${server.password}"
					host = "localhost"
					port = 8080
            
					uploads {
						dir {
							localDir = "${testSetup.destination}/uploads"
							remoteDir = "/data"
						}
					}
				}
            }
        """
		server.addAuth()
		// Create the user-secure file
		CryptoUtil.savePassword(server.username, server.password)
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('uploadDirToFakeServer', '--stacktrace', testSetup.gradleLogLevel)
				.withPluginClasspath()
				.withDebug(true)
				.build()

		log.quiet("Output: $result.output")
		result.output.contains("uploadDirToFakeServer")
		result.task(":uploadDirToFakeServer").outcome == "SUCCESS"
		assertEquals(4, server.getFileSystem().listFiles("/data/").size())
		assertEquals(1, server.getFileSystem().listFiles("/data/uploads/subDir/").size())
	}

	/**
	 * Use for debug: listEntries(server.getFileSystem().listFiles("/data"))
	 */
	def listEntries(List<FileSystemEntry> entries) {
		entries.each { FileSystemEntry entry ->
			log.quiet("Entry: ${entry}")
			if (entry.isDirectory()) {
				listEntries(server.getFileSystem().listFiles(entry.getPath()))
			}
		}
	}

}
