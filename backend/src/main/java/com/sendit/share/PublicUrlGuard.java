package com.sendit.share;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.springframework.stereotype.Component;

@Component
public class PublicUrlGuard {

    public void validate(URI uri) {
        if (uri.getHost() == null
                || (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme()))) {
            throw new ContentAnalysisException("HTTP 또는 HTTPS 공개 URL만 분석할 수 있습니다.");
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (isBlocked(address)) {
                    throw new ContentAnalysisException("내부 네트워크 주소는 분석할 수 없습니다.");
                }
            }
        } catch (UnknownHostException exception) {
            throw new ContentAnalysisException("URL의 호스트를 찾을 수 없습니다.");
        }
    }

    private boolean isBlocked(InetAddress address) {
        byte[] bytes = address.getAddress();
        boolean carrierGradeNat = bytes.length == 4
                && Byte.toUnsignedInt(bytes[0]) == 100
                && Byte.toUnsignedInt(bytes[1]) >= 64
                && Byte.toUnsignedInt(bytes[1]) <= 127;
        boolean metadataAddress = bytes.length == 4
                && Byte.toUnsignedInt(bytes[0]) == 169
                && Byte.toUnsignedInt(bytes[1]) == 254;
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || carrierGradeNat
                || metadataAddress;
    }
}

