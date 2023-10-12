package org.example.cxfclientotelbug.config;

import de.codecentric.cxf.logging.soapmsg.SoapMessageLoggingInInterceptor;
import de.codecentric.cxf.logging.soapmsg.SoapMessageLoggingOutInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.tracing.opentelemetry.OpenTelemetryClientFeature;
import org.apache.hello_world_soap_http.Greeter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfiguration {

    @Value("${webservice.client.url}")
    private String clientUrl;

    /*
     * CXF JaxWs Client config
     */
    @Bean
    public org.apache.hello_world_soap_http.Greeter greeterClient(OpenTelemetry otel, Tracer tracer) {
        var jaxWsFactory = new JaxWsProxyFactoryBean();
        jaxWsFactory.setServiceClass(org.apache.hello_world_soap_http.Greeter.class);
        jaxWsFactory.setAddress(clientUrl);

        // setup cxf client auto-tracing
        jaxWsFactory.getFeatures().add(new OpenTelemetryClientFeature(otel, tracer));

        jaxWsFactory.getInInterceptors().add(logInInterceptorClientSoapMsgLogger());
        jaxWsFactory.getOutInterceptors().add(logOutInterceptorClientSoapMsgLogger());
        return (Greeter) jaxWsFactory.create();
    }

    @Bean
    public AbstractLoggingInterceptor logInInterceptorClientSoapMsgLogger() {
        var logInInterceptor = new SoapMessageLoggingInInterceptor();
        logInInterceptor.logSoapMessage(true);
        logInInterceptor.setPrettyLogging(true);
        return logInInterceptor;
    }

    @Bean
    public AbstractLoggingInterceptor logOutInterceptorClientSoapMsgLogger() {
        var logOutInterceptor = new SoapMessageLoggingOutInterceptor();
        logOutInterceptor.logSoapMessage(true);
        logOutInterceptor.setPrettyLogging(true);
        return logOutInterceptor;
    }

}
