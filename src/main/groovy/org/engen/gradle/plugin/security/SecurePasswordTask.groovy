package org.engen.gradle.plugin.security

import groovy.transform.CompileStatic
import org.engen.gradle.plugin.Server
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

@CompileStatic
class SecurePasswordTask extends DefaultTask {

    Server serverInfo

    @TaskAction
    void securePassword() {
        String username = serverInfo.username
        if (username == null) {
            throw new IllegalArgumentException("The username must be provided!")
        }
        String password = serverInfo.password
        if (password == null) {
            // Cannot get console. Read build property
            if (this.project.hasProperty("password")) {
                password = this.project.property("password")
                println "Password set via project property!"
            } else {
                // Try via console
                def console = System.console()
                if (console) {
                    password = console.readPassword('> Please enter your password: ')
                } else {
                    if (password == null) {
                        throw new IllegalArgumentException("Error. The console is not present (please use --no-daemon) and a password was not provided")
                    }
                }
            }
        }
        CryptoUtil.savePassword(username, password)
    }

}
