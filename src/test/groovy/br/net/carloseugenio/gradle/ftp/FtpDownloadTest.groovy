package br.net.carloseugenio.gradle.ftp

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

import static org.junit.Assert.*

class FtpDownloadTest {

	public @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
	private Logger log = LoggerFactory.getLogger(getClass())
	private FakeFtpServer fakeFtpServer
	File buildFile

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
		fakeFtpServer = new FakeFtpServer()
		fakeFtpServer.setServerControlPort(8080)
		fakeFtpServer.addUserAccount(new UserAccount("user", "password", "c:\\data"))

		FileSystem fs = createFileSystem(true)
		makeDirectory(fs, "/")
		addFile(fs, "/data", "file1.txt", "file 1 test data")
		addFile(fs, "/data", "file2.txt", "file 2 test data")
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
	}

	@Test
	void fTPServerAndFileSystemCreated () {
		log.info("Verify existence of file...")
		assert fakeFtpServer.getFileSystem().exists("/data/file1.txt")
	}

	@Test
	void testPrintSourceSetsTaskAdded() {
		Project project = ProjectBuilder.builder().build()
		project.pluginManager.apply "br.net.carloseugenio.gradle.ftp.gradle-ftp"
		log.quiet("Tasks: $project.tasks")
	}

	@Test
	void testTasksWithBuildStream() {
		buildFile << """
            ftp {
				FakeServer {
					username = "user"
					password = "password"
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

		log.quiet("Output: $result.output")
		result.output.contains("downloadFilesFromFakeServer")
		result.task(":tasks").outcome == "SUCCESS"
	}

	@Test
	void testTasksWithoutPassword() {
		buildFile << """
            ftp {
				FakeServer {
					username = "user"
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
				.withArguments('downloadFilesFromFakeServer')
				.withPluginClasspath()
		.buildAndFail()
	}

	@Test
	void testDownloadDir() {
		buildFile << """
            ftp {
				FakeServer {
					username = "user"
					password = "password"
					host = "localhost"
					port = 8080
            
					downloads {
						files {
							remoteDir = "/data"
							localDir = "."
						}
					}
				}
            }
        """

		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('downloadFilesFromFakeServer', '--stacktrace')
				.withPluginClasspath()
				.build()

		log.quiet("Output: $result.output")
		//result.output.contains("downloadFilesFromFakeServer")
		//result.task(":tasks").outcome == "SUCCESS"
	}

	@Test
	void testDownloadDirectory() {

		//FtpDownloadDirectoryTask task = new FtpDownloadDirectoryTask();
		Project project = ProjectBuilder.builder().withName("testProject").build()
		project.plugins.apply(GradleFtpPlugin)

		NamedDomainObjectContainer<Server> ftpExtension = project.extensions.findByName(GradleFtpPlugin.FTP_EXTENSION_NAME)


		Server serverInfo = new Server("fakeServer")
		serverInfo.host = "localhost"
		serverInfo.port = 8080
		serverInfo.username = "user"
		serverInfo.password = "password"
		serverInfo.downloads = new HashSet()
		def spec = new ResourceSpec("downloadDir")
		spec.localDir = "."
		spec.remoteDir = "/data"
		serverInfo.downloads.add spec

	}
}
