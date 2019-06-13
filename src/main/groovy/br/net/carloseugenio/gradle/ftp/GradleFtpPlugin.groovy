package br.net.carloseugenio.gradle.ftp


import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class GradleFtpPlugin implements Plugin<Project> {

	static final FTP_EXTENSION_NAME = "ftp"
	private static final String DOWNLOAD_TASK_PATTERN = 'download%sFrom%s'
    private static final String UPLOAD_TASK_PATTERN = 'upload%sTo%s'
	private static final String SECURE_SERVER_PASSWORD_PATTERN = 'secure%sPassword'
	private static final String TASK_GROUP_NAME = "Engen"

    void apply(Project project) {
    	project.afterEvaluate {
	    	def manager = project.pluginManager
	    	if (!manager.hasPlugin("java")) {
				project.plugins.apply JavaPlugin
			}
			println "Found java in project ${project.name}"
			project.tasks.create('printSourceSets', PrintSourceSetsTask) {
				description = "Print all configured sourceSets for this project"
				group = "Engen"
			}
	    }
		// The ftp util task and extension
		this.setupExtension(project)
		this.createTasks(project)
    }

	private void setupExtension(final Project project) {

		// Create NamedDomainObjectContainer for Server objects.
		// We must use the container() method of the Project class
		// to create an instance. New Server instances are
		// automatically created, because we have String argument
		// constructor that will get the name we use in the DSL.
		final NamedDomainObjectContainer<Server> servers =
				project.container(Server)

		servers.all { Server it ->
			// Here we have access to the project object, so we
			// can use the container() method to create a
			// NamedDomainObjectContainer for ResourceSpec objects.
			it.downloads = project.container(ResourceSpec)
            it.uploads = project.container(ResourceSpec)
		}

		// Use FTP as the name in the build script to define
		// servers and inner downloads and uploads.
		project.extensions.add(FTP_EXTENSION_NAME, servers)

	}

	/**
	 * Create a new download task for each server download.
	 */
	private void createTasks(final Project project) {
		def servers = project.extensions.getByName(FTP_EXTENSION_NAME)

		servers.all {
			// Actual Server instance is the delegate
			// of this closure. We assign it to a variable
			// so we can use it again inside the
			// closure for downloads.all() method.
			def serverInfo = delegate

			it.downloads.all {
				// Assign this closure's delegate to
				// variable so we can use it in the task
				// configuration closure.
				def resourceSpec = delegate
                //println "Download delegate remoteDir: ${delegate.remoteDir}"
				createFtpDownloadTask(project, resourceSpec, serverInfo)
			}
            it.uploads.all {
                def resourceSpec = delegate
                createFtpUploadTask(project, resourceSpec, serverInfo)
            }
			createSecureServerPasswordTask(project, serverInfo)
		}
	}

	def createSecureServerPasswordTask(Project project, Server serverInfo) {
		// Security utility to store user password for the given server
		def taskName =
				String.format(
						SECURE_SERVER_PASSWORD_PATTERN,
						serverInfo.name.capitalize())

		project.task(taskName, type: br.net.carloseugenio.gradle.ftp.security.SecurePasswordTask) {
			description = "Create a secure user file password on disk to login on FTPServer: $serverInfo.name"
			group = "Engen"
			it.serverInfo = serverInfo
		}
	}

	private void createFtpDownloadTask(project, resourceSpec, serverInfo) {
		// Make download and server names pretty for use in task name.
		def taskName =
				String.format(
						DOWNLOAD_TASK_PATTERN,
						resourceSpec.name.capitalize(),
						serverInfo.name.capitalize())
        project.afterEvaluate {
            // Create new task for this node.
            project.task(taskName, type: FtpDownloadDirectoryTask) {
                description = "Download remote file/directory ['${resourceSpec.remoteSpec()}]' from server '${serverInfo.name}'"
                group = TASK_GROUP_NAME

                it.serverInfo = serverInfo
                it.downloadInfo = resourceSpec
            }
        }
    }

    private void createFtpUploadTask(project, resourceSpec, serverInfo) {
        // Make upload and server names pretty for use in task name.
        def taskName =
                String.format(
                        UPLOAD_TASK_PATTERN,
                        resourceSpec.name.capitalize(),
                        serverInfo.name.capitalize())

        project.afterEvaluate {
            // Create new task for this node.
            project.task(taskName, type: FtpUploadDirectoryTask) {
                description = "Uplaod local file/directory ['${resourceSpec.localSpec()}'] to server '${serverInfo.name}'"
                group = TASK_GROUP_NAME

                it.serverInfo = serverInfo
                it.uploadInfo = resourceSpec
            }
        }
    }
}