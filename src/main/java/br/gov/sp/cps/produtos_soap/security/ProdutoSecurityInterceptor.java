package br.gov.sp.cps.produtos_soap.security;

import java.util.Locale;

import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.RequestData;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.security.WsSecurityValidationException;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;

public class ProdutoSecurityInterceptor extends Wss4jSecurityInterceptor {
    public static final String SECURITY_FAULT = "Falha na autenticação WS-Security.";

    @Override
    protected RequestData initializeValidationRequestData(MessageContext messageContext) {
        RequestData data = super.initializeValidationRequestData(messageContext);
        // Exige digest, Nonce e Created; PasswordText nao e aceito.
        data.setRequiredPasswordType(WSConstants.PASSWORD_DIGEST);
        data.setUtTTL(300);
        data.setUtFutureTTL(60);
        return data;
    }

    @Override
    protected boolean handleValidationException(WsSecurityValidationException ex,
            MessageContext messageContext) {
        logger.debug("Requisicao recusada pelo WS-Security", ex);
        // Nao informa ao cliente se o usuario existe ou qual parte do token falhou.
        SoapMessage response = (SoapMessage) messageContext.getResponse();
        response.getSoapBody().addClientOrSenderFault(SECURITY_FAULT, Locale.forLanguageTag("pt-BR"));
        return false;
    }
}
