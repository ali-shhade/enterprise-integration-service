package com.ali.integration;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class SftpOutboundRoute extends RouteBuilder {

    @Override
    public void configure() {

from("file:data/day5/sftp-outgoing?noop=true")



 .routeId("sftp-outbound-route")

            .log("Received outgoing file: ${file:name}")

	.to("sftp://{{integration.sftp.host}}:{{integration.sftp.port}}/{{integration.sftp.outbound-directory}}"
                + "?username={{integration.sftp.username}}"
                + "&privateKeyFile={{integration.sftp.private-key}}"
                + "&knownHostsFile={{integration.sftp.known-hosts}}"
                + "&preferredAuthentications=publickey"
                + "&strictHostKeyChecking=yes");
	}

}
