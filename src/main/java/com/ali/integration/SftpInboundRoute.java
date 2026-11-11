
package com.ali.integration;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class SftpInboundRoute extends RouteBuilder {

    @Override
    public void configure() {

        from("sftp://{{integration.sftp.host}}:{{integration.sftp.port}}/{{integration.sftp.remote-directory}}"
                + "?username={{integration.sftp.username}}"
                + "&privateKeyFile={{integration.sftp.private-key}}"
                + "&knownHostsFile={{integration.sftp.known-hosts}}"
                + "&preferredAuthentications=publickey"
                + "&strictHostKeyChecking=yes"
                + "&delay=5000"
                + "&noop=true")

            .routeId("sftp-inbound-route")

            .log("Received SFTP file: ${header.CamelFileName}")

            .to("file:data/day5/sftp-received");
    }
}
