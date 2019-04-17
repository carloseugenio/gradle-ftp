package org.engen.gradle.plugin

import org.gradle.api.NamedDomainObjectContainer

class Server {

    String name
    String host
    int port
    String username
    String password

    NamedDomainObjectContainer<ResourceSpec> downloads
    NamedDomainObjectContainer<ResourceSpec> uploads

    Server(String name) {
        this.name = name
    }

    def downloads(final Closure configureClosure) {
        downloads.configure(configureClosure)
    }

    def uploads(final Closure configureClosure) {
        uploads.configure(configureClosure)
    }

    @Override
    String toString() {
        return "Server{" +
                "name='" + name + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", downloads=" + downloads +
                '}'
    }

}
