package com.project.kiro.service;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.project.kiro.exception.GmailApiException;
import com.project.kiro.exception.GmailNotConnectedException;
import com.project.kiro.model.GmailToken;
import com.project.kiro.repository.GmailTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;

@Service
public class GmailOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GmailOAuthService.class);

    private final GmailTokenRepository gmailTokenRepository;

    @Value("${gmail.oauth.client-id}")
    private String clientId;

    @Value("${gmail.oauth.client-secret}")
    private String clientSecret;

    @Value("${gmail.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${gmail.oauth.scopes}")
    private String scopes;

    public GmailOAuthService(GmailTokenRepository gmailTokenRepository) {
        this.gmailTokenRepository = gmailTokenRepository;
    }

    /**
     * Builds the Google OAuth2 authorization URL with required params.
     * access_type=offline ensures a refresh token is issued.
     * prompt=consent forces the consent screen each time to guarantee refresh token.
     */
    public String getAuthorizationUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth"
            + "?client_id=" + clientId
            + "&redirect_uri=" + redirectUri
            + "&response_type=code"
            + "&scope=" + scopes
            + "&access_type=offline"
            + "&prompt=consent";
    }

    /**
     * Checks if a Gmail account is currently connected.
     */
    public boolean isConnected() {
        return gmailTokenRepository.findByConnectedTrue().isPresent();
    }

    /**
     * Retrieves the active GmailToken if connected.
     */
    public Optional<GmailToken> getActiveToken() {
        return gmailTokenRepository.findByConnectedTrue();
    }

    /**
     * Exchanges an OAuth2 authorization code for tokens and persists them.
     */
    public void exchangeCodeForTokens(String authCode) {
        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                authCode,
                redirectUri
            ).execute();

            // Delete any existing token before saving new one
            gmailTokenRepository.deleteAll();

            GmailToken token = GmailToken.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .connected(true)
                .tokenExpiresAt(Instant.now().plusSeconds(tokenResponse.getExpiresInSeconds()))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

            gmailTokenRepository.save(token);
            log.info("Gmail account connected successfully");
        } catch (Exception e) {
            log.error("Failed to exchange authorization code for tokens", e);
            throw new GmailApiException("Failed to complete Gmail connection: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a valid (non-expired) access token.
     * Refreshes the token if expired. Throws if not connected or refresh fails.
     */
    public String getValidAccessToken() {
        GmailToken token = gmailTokenRepository.findByConnectedTrue()
            .orElseThrow(() -> new GmailNotConnectedException(
                "No Gmail account connected. Please connect your Gmail account first."));

        // If token not expired, return it
        if (token.getTokenExpiresAt().isAfter(Instant.now())) {
            return token.getAccessToken();
        }

        // Token expired — refresh it
        try {
            GoogleTokenResponse refreshResponse = new GoogleRefreshTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                token.getRefreshToken(),
                clientId,
                clientSecret
            ).execute();

            token.setAccessToken(refreshResponse.getAccessToken());
            token.setTokenExpiresAt(Instant.now().plusSeconds(refreshResponse.getExpiresInSeconds()));
            token.setUpdatedAt(Instant.now());
            gmailTokenRepository.save(token);

            log.info("Gmail access token refreshed successfully");
            return token.getAccessToken();
        } catch (TokenResponseException e) {
            if (e.getDetails() != null && "invalid_grant".equals(e.getDetails().getError())) {
                // Refresh token revoked/expired — delete and force reconnect
                gmailTokenRepository.delete(token);
                log.warn("Gmail refresh token is invalid, account disconnected");
                throw new GmailNotConnectedException(
                    "Gmail access has been revoked. Please reconnect your Gmail account.");
            }
            throw new GmailApiException("Failed to refresh Gmail token: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new GmailApiException("Failed to refresh Gmail token: " + e.getMessage(), e);
        }
    }

    /**
     * Disconnects the Gmail account.
     * Revokes the token with Google, then deletes locally.
     * Always deletes locally even if revocation fails.
     * @return warning message if revocation failed, null if clean disconnect
     */
    public String disconnect() {
        Optional<GmailToken> tokenOpt = gmailTokenRepository.findByConnectedTrue();
        if (tokenOpt.isEmpty()) {
            return null; // Already disconnected
        }

        GmailToken token = tokenOpt.get();
        String warning = null;

        // Attempt to revoke token with Google
        try {
            RestTemplate restTemplate = new RestTemplate();
            String revokeUrl = "https://oauth2.googleapis.com/revoke?token=" + token.getAccessToken();
            restTemplate.postForEntity(revokeUrl, null, String.class);
            log.info("Gmail token revoked successfully with Google");
        } catch (Exception e) {
            log.warn("Failed to revoke Gmail token with Google: {}", e.getMessage());
            warning = "Token revocation could not be confirmed with Google. Local credentials have been removed.";
        }

        // Always delete locally
        gmailTokenRepository.delete(token);
        log.info("Gmail account disconnected locally");

        return warning;
    }
}
