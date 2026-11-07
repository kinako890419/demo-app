package com.example.app.security.saml;

import com.onelogin.saml2.authn.SamlResponse;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.exception.ValidationError;
import com.onelogin.saml2.factory.SamlMessageFactory;
import com.onelogin.saml2.http.HttpRequest;
import com.onelogin.saml2.settings.Saml2Settings;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;

public class CustomSamlMessageFactory implements SamlMessageFactory {
    @Override
    public SamlResponse createSamlResponse(Saml2Settings settings, HttpRequest httpRequest) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException, ValidationError, SettingsException {
        // Create and return an instance of your custom SamlResponse class
        return new CustomSamlResponse(settings, httpRequest);
    }
}
