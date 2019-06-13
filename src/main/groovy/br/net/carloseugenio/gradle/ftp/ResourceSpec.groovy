package br.net.carloseugenio.gradle.ftp

class ResourceSpec {

    /* ResourceSpec options */
    String name
    String remoteDir
    String remoteFile
    String wildCard
    boolean recursive

    String localDir
    String localFile

    String projectPath
    String sourcesDir

    ResourceSpec(String name) {
        this.name = name
    }


    @Override
    String toString() {
        return "ResourceSpec{" +
                "name='" + name +'\'' +
                ", remoteDir='" + remoteDir + '\'' +
                ", remoteFile='" + remoteFile + '\'' +
                ", wildCard='" + wildCard + '\'' +
                ", recursive=" + recursive +
                ", localDir='" + localDir + '\'' +
                ", localFile='" + localFile + '\'' +
                ", projectPath='" + projectPath + '\'' +
                ", sourcesDir='" + sourcesDir + '\'' +
                '}';
    }

    String localSpec() {
        localFile ? localFile : localDir
    }

    String remoteSpec() {
        remoteFile ? remoteFile : remoteDir
    }
}