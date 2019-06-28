package br.net.carloseugenio.gradle.ftp

import br.net.carloseugenio.gradle.ftp.security.CryptoUtil
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.junit.Assert.assertEquals

class FtpDownloadTest {

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
	}

	@After
	void tearDown() {
		server.stop()
		testSetup.destroy()
	}

	@Test
	void testFTPServerAndFileSystemCreated () {
		log.info("Verify existence of file...")
		assert server.getFileSystem().exists("/data/file1.txt")
	}

	@Test
	void testApplyPlugin() {
		Project project = ProjectBuilder.builder().build()
		project.pluginManager.apply "br.net.carloseugenio.gradle.ftp.gradle-ftp"
		assert project.plugins.size() == 1
	}

	@Test
	void testDownloadTasksCreated() {
		testSetup.buildFile << """
            ftp {
				FakeServer {
					username = "${server.username}"
					password = "${server.password}"
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
		testSetup.buildFile << """
            ftp {
				FakeServer {
					username = "${server.username}"
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
				.withArguments('downloadFilesFromFakeServer', '--stacktrace', testSetup.gradleLogLevel)
				.withPluginClasspath()
		.buildAndFail()
		//log.quiet("Output: $result.output")
	}

	@Test
	void testDownloadDirectory() {
		testSetup.buildFile << """
            ftp {
				FakeServer {
					username = "${server.username}"
					password = "${server.password}"
					host = "localhost"
					port = 8080
            
					downloads {
						files {
							remoteDir = "/data"
							localDir = "${testSetup.destination}"
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
				.withArguments('downloadFilesFromFakeServer', '--stacktrace', testSetup.gradleLogLevel)
				.withPluginClasspath()
				.withDebug(true)
				.build()

		//log.quiet("Output: $result.output")
		result.output.contains("downloadFilesFromFakeServer")
		result.task(":downloadFilesFromFakeServer").outcome == "SUCCESS"
		assertEquals(3, new File("${testSetup.destination}/data").listFiles().length)
		assertEquals(1, new File("${testSetup.destination}/data/subdir").listFiles().length)
	}

	@Test
	void testDownloadFile() {

		testSetup.buildFile << """
            ftp {
				FakeServer {
					username = "${server.username}"
					password = "${server.password}"
					host = "localhost"
					port = 8080
            
					downloads {
						file {
							remoteFile = "/data/file1.txt"
							localDir = "${testSetup.destination}"
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
				.withArguments('downloadFileFromFakeServer', '--stacktrace', testSetup.gradleLogLevel)
				.withPluginClasspath()
				.withDebug(true)
				.build()
		//log.quiet("Output: $result.output")
		result.output.contains("downloadFilesFromFakeServer")
		result.task(":downloadFileFromFakeServer").outcome == "SUCCESS"
		assertEquals(1, new File("${testSetup.destination}/data").listFiles().length)
	}

}
