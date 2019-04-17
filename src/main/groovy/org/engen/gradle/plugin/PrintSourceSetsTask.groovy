package org.engen.gradle.plugin

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

//@CompileStatic
class PrintSourceSetsTask extends DefaultTask {

    @TaskAction
    void printSourceSets() {
        project.sourceSets.each {
            println "================ SourceSet: ${it} ===================="
            def dirs = it.allSource.srcDirs
            println "SourceSet: [${it}] allSource: \n" + dirs.join("\n")
            def classesOutputDir = it.output.classesDir
            println "SourceSet Classes OutputDir: ${classesOutputDir}"
            def resourcesOutputDir = it.output.resourcesDir
            println "SourceSet Resources OutputDir: ${resourcesOutputDir}"
            def outputFiles = it.output.files
            println "SourceSet Files: \n" + outputFiles.join("\n")
            println "======================================================="
            println ""
        }
    }

}
