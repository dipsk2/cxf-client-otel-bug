package org.example.cxfclientotelbug.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.extern.slf4j.Slf4j;
import org.apache.hello_world_soap_http.Greeter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.example.cxfclientotelbug.config.WiremockConfig.WIREMOCK_BEAN;

@RestController
@Slf4j
public class SampleController {

    public static final String MOCK_TEST_URI = "/mock-uri";
    private final org.apache.hello_world_soap_http.Greeter greeter;
    private final WireMockServer mockServer;

    public SampleController(Greeter greeter, @Qualifier(WIREMOCK_BEAN) WireMockServer mockServer) {
        this.greeter = greeter;
        this.mockServer = mockServer;
        // setup mock for the SOAP client calls
        mockServer.stubFor(post(urlPathEqualTo(MOCK_TEST_URI))
                .withHeader("Content-Type", WireMock.containing("text/xml; charset=UTF-8"))
                .withHeader("SoapAction", WireMock.containing(""))
                .willReturn(aResponse().withBody("<soapenv:Envelope " +
                        "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                        "xmlns:v1=\"http://apache.org/hello_world_soap_http/types\">\n" +
                        "   <soapenv:Header/>\n" +
                        "   <soapenv:Body>\n" +
                        "      <v1:greetMeResponse>\n" +
                        "         <v1:responseType>hello</v1:responseType>\n" +
                        "      </v1:greetMeResponse>\n" +
                        "   </soapenv:Body>\n" +
                        "</soapenv:Envelope>")));
    }

    /**
     * A rest endpoint which makes a SOAP (cxf) client call to a one-way operation
     */
    @RequestMapping(path = "/one-way", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public void greet() {
        log.info("one-way call!");
        greeter.greetMeOneWay("hello world!");
    }

    /**
     * A rest endpoint which makes a SOAP (cxf) client call to a two-way operation
     */
    @RequestMapping(path = "/two-way", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String greet2Way() {
        log.info("two-way call!");
        return greeter.greetMe("hello world!");
    }

}
