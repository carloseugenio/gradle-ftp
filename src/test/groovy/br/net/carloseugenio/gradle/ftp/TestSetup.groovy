package br.net.carloseugenio.gradle.ftp

import org.junit.rules.TemporaryFolder

class TestSetup {

	String destination = "build/test-data/"
	String uploadDestination = "uploads"
	String gradleLogLevel = "-i"
	File buildFile
	TemporaryFolder testProjectDir
	File uploadDir

	TestSetup(TemporaryFolder testProjectDir) {
		this.testProjectDir = testProjectDir
	}

	void createEnv() {
		new File(destination).deleteDir()
		assert !new File(destination).exists()
		new File("user-secure").delete()
		assert !new File("user-secure").exists()

		buildFile = testProjectDir.newFile('build.gradle')
		buildFile << """
            plugins {
                id 'br.net.carloseugenio.gradle.ftp.gradle-ftp'
            }
        """
	}

	void destroy() {
		new File(destination).deleteDir()
		assert !new File(destination).exists()
		new File("user-secure").delete()
		assert !new File("user-secure").exists()
	}

	void createUploads() {
		uploadDir = new File(destination, "/${uploadDestination}")
		uploadDir.mkdirs()
		File upload1 = new File(uploadDir, "/upload1.txt")
		upload1.createNewFile()
		upload1 << "upload file 1 data"
		File uploadSubDir = new File(uploadDir, "/subDir")
		uploadSubDir.mkdirs()
		File upload2 = new File(uploadSubDir, "/upload2.txt")
		upload2.createNewFile()
		upload2 << "upload file 2 data"
		File upload3 = new File(uploadSubDir, "/upload2.txt")
		upload3.createNewFile()
		upload3 << "upload file 2 data"
	}

}
