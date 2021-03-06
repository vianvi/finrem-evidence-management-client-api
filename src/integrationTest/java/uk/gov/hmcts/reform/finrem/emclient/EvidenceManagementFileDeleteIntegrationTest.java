package uk.gov.hmcts.reform.finrem.emclient;

import io.restassured.response.Response;
import net.serenitybdd.junit.runners.SerenityRunner;
import net.serenitybdd.junit.spring.integration.SpringIntegrationMethodRule;
import net.serenitybdd.rest.SerenityRest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.finrem.emclient.EvidenceManagementTestUtils.AUTHORIZATION_HEADER_NAME;

@Lazy
@RunWith(SerenityRunner.class)
@ComponentScan(basePackages = {"uk.gov.hmcts.reform.finrem.emclient", "uk.gov.hmcts.auth.provider.service"})
@ImportAutoConfiguration({FeignRibbonClientAutoConfiguration.class,HttpMessageConvertersAutoConfiguration.class,
        FeignAutoConfiguration.class})
@ContextConfiguration(classes = {ServiceContextConfiguration.class})
@PropertySource("classpath:application.properties")
@PropertySource("classpath:application-${env}.properties")
public class EvidenceManagementFileDeleteIntegrationTest {

    @Rule
    public SpringIntegrationMethodRule springMethodIntegration = new SpringIntegrationMethodRule();

    @Autowired
    private IDAMUtils idamTestSupportUtil;

    @Value("${evidence.management.client.api.baseUrl}")
    private String evidenceManagementClientApiBaseUrl;

    @Value("${document.management.store.baseUrl}")
    private String documentManagementURL;

    private EvidenceManagementTestUtils evidenceManagementTestUtils = new EvidenceManagementTestUtils();

    private static final String FILE_PATH = "src/integrationTest/resources/FileTypes/PNGFile.png";
    private static final String IMAGE_FILE_CONTENT_TYPE = "image/png";
    private static final String DELETE_ENDPOINT = "/deleteFile?fileUrl=";

    @Test
    public void verifyDeleteRequestForExistingDocumentIsSuccessful() {
        String fileUrl = uploadFile();
        Response response = deleteFileFromEvidenceManagement(fileUrl, evidenceManagementTestUtils.getAuthenticationTokenHeader(idamTestSupportUtil));

        assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatusCode());
    }

    @Test
    public void verifyDeleteRequestForNonExistentDocumentIs404NotFound() {
        String fileUrl = uploadFile();
        String fileUrlAlt = fileUrl.concat("xyzzy");
        Response response = deleteFileFromEvidenceManagement(fileUrlAlt, evidenceManagementTestUtils.getAuthenticationTokenHeader(idamTestSupportUtil));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode());
    }

    @Test
    public void verifyDeleteRequestWithMissingDocumentIdIsNotAllowed() {
        String fileUrl = uploadFile();
        String fileUrlAlt = fileUrl.substring(0, fileUrl.lastIndexOf("/") + 1);
        Response response = deleteFileFromEvidenceManagement(fileUrlAlt, evidenceManagementTestUtils.getAuthenticationTokenHeader(idamTestSupportUtil));

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), response.getStatusCode());
    }

    @Test
    public void verifyDeleteRequestWithInvalidAuthorizationHeaderIsInternalServerError() {
        String fileUrl = uploadFile();
        Map<String, Object> headers = evidenceManagementTestUtils.getAuthenticationTokenHeader(idamTestSupportUtil);
        String invalidToken = "x".concat(headers.get(AUTHORIZATION_HEADER_NAME).toString()).concat("x");
        headers.put(AUTHORIZATION_HEADER_NAME, invalidToken);
        Response response = deleteFileFromEvidenceManagement(fileUrl, headers);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode());
    }

    @Test
    public void verifyDeleteRequestWithUnauthorisedAuthTokenIsUnauthorized() {
        String fileUrl = uploadFile();
        Map<String, Object> headers = evidenceManagementTestUtils.getInvalidAuthenticationTokenHeader();

        Response response = deleteFileFromEvidenceManagement(fileUrl, headers);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode());
    }

    private Response deleteFileFromEvidenceManagement(String fileUrl, Map<String, Object> headers) {
        return SerenityRest.given()
                .headers(headers)
                .delete(evidenceManagementClientApiBaseUrl.concat(DELETE_ENDPOINT + fileUrl))
                .andReturn();
    }

    private String uploadFile(){
        return evidenceManagementTestUtils.uploadFileToEvidenceManagement(FILE_PATH, IMAGE_FILE_CONTENT_TYPE,
                evidenceManagementClientApiBaseUrl,
                documentManagementURL,
                idamTestSupportUtil);
    }
}
