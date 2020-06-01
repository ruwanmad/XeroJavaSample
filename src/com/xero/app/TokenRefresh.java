package com.xero.app;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;

public class TokenRefresh {

    final static Logger LOGGER = LogManager.getLogger(AuthenticatedResource.class);
    // Set these values using envirnoment variables or hardcoded.
    final String clientId = System.getenv("XERO_CLIENT_ID");
    final String clientSecret = System.getenv("XERO_CLIENT_SECRET");
    final String TOKEN_SERVER_URL = "https://identity.xero.com/connect/token";

    public TokenRefresh() {
        super();
    }

    public String checkToken(String accessToken, String refreshToken, HttpServletResponse response) throws IOException {
        String currToken = null;

        try {
            DecodedJWT jwt = JWT.decode(accessToken);

            if (jwt.getExpiresAt().getTime() > System.currentTimeMillis()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "------------------ Refresh Token : NOT NEEDED - return current token -------------------");
                }
                currToken = accessToken;
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("------------------ Refresh Token : BEGIN -------------------");
                }
                try {
                    TokenResponse tokenResponse = new RefreshTokenRequest(new NetHttpTransport(), new JacksonFactory(),
                            new GenericUrl(TOKEN_SERVER_URL), refreshToken)
                            .setClientAuthentication(new BasicAuthentication(this.clientId, this.clientSecret))
                            .execute();
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("------------------ Refresh Token : SUCCESS -------------------");
                    }

                    // DEMO PURPOSE ONLY - You'll need to implement your own token storage solution
                    TokenStorage store = new TokenStorage();
                    store.saveItem(response, "jwt_token", tokenResponse.toPrettyString());
                    store.saveItem(response, "access_token", tokenResponse.getAccessToken());
                    store.saveItem(response, "refresh_token", tokenResponse.getRefreshToken());
                    store.saveItem(response, "expires_in_seconds", tokenResponse.getExpiresInSeconds().toString());

                    currToken = tokenResponse.getAccessToken();
                } catch (TokenResponseException e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("------------------ Refresh Token : EXCEPTION -------------------");
                    }
                    if (e.getDetails() != null) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Error: " + e.getDetails().getError());
                        }
                        if (e.getDetails().getErrorDescription() != null) {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(e.getDetails().getErrorDescription());
                            }
                        }
                        if (e.getDetails().getErrorUri() != null) {

                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug(e.getDetails().getErrorUri());
                            }
                        }
                    } else {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("------------------ Refresh Token : EXCEPTION -------------------");
                            LOGGER.debug(e.getMessage());
                        }
                    }
                }
            }

        } catch (JWTDecodeException exception) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("------------------ Refresh Token : INVALID TOKEN -------------------");
                LOGGER.debug(exception.getMessage());
            }
        }

        return currToken;
    }

}
