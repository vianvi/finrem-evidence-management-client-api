package uk.gov.hmcts.reform.emclient.health;

import java.util.Collections;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class DefaultHttpEntityFactory implements HttpEntityFactory {
    @Override
    public HttpEntity<Object> createRequestEntityForHealthCheck() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        return new HttpEntity<>(headers);
    }
}
