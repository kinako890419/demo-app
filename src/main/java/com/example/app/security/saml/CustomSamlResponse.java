package com.example.app.security.saml;

import com.onelogin.saml2.authn.SamlResponse;
import com.onelogin.saml2.exception.SettingsException;
import com.onelogin.saml2.exception.ValidationError;
import com.onelogin.saml2.http.HttpRequest;
import com.onelogin.saml2.model.SubjectConfirmationIssue;
import com.onelogin.saml2.settings.Saml2Settings;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;


public class CustomSamlResponse extends SamlResponse {
    public CustomSamlResponse(Saml2Settings settings, HttpRequest request) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException, SettingsException, ValidationError {
        super(settings, request);
    }

    @Override
    protected void validateDestination(Element element) {
        // Do nothing
    }

    @Override
    protected SubjectConfirmationIssue validateRecipient(final Node recipient, final int index) {
        return null;
    }

    @Override
    protected void validateAudiences() {
    }
}

