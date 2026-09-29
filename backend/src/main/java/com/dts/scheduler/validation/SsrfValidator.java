package com.dts.scheduler.validation;

import com.dts.scheduler.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Slf4j
@Component
public class SsrfValidator {

    public void validateSafeUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            throw new BadRequestException("Target URL cannot be empty");
        }

        URI uri;
        try {
            uri = URI.create(urlString.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid URL syntax: " + urlString);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new BadRequestException("Only HTTP and HTTPS protocols are permitted");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BadRequestException("URL must contain a valid hostname");
        }

        // Block typical local hostnames
        if (host.equalsIgnoreCase("localhost") || host.endsWith(".local") || host.endsWith(".internal")) {
            throw new BadRequestException("Access to internal hostnames is prohibited (SSRF Protection)");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isRestrictedIp(address)) {
                    log.warn("Blocked SSRF attempt to restricted IP [{}] for host [{}]", address.getHostAddress(), host);
                    throw new BadRequestException("Access to internal/private network IP [" + address.getHostAddress() + "] is prohibited (SSRF Protection)");
                }
            }
        } catch (UnknownHostException e) {
            throw new BadRequestException("Failed to resolve hostname: " + host);
        }
    }

    private boolean isRestrictedIp(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isCloudMetadataIp(address);
    }

    private boolean isCloudMetadataIp(InetAddress address) {
        String ip = address.getHostAddress();
        // Common cloud metadata endpoints (AWS, GCP, Azure, DigitalOcean: 169.254.169.254, 169.254.169.250, etc.)
        return ip.startsWith("169.254.");
    }
}
