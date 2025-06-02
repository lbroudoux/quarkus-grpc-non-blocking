package org.acme.example.service;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Alternative;
import org.acme.example.model.ApiToken;
import org.acme.example.repository.ApiTokenRepository;
import org.jboss.logging.Logger;

/**
 *
 */
@Alternative
@Priority(1)
@ApplicationScoped
public class ApiTokenIdentityProvider implements IdentityProvider<TokenAuthenticationRequest> {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   private final ApiTokenRepository tokenRepository;

   public ApiTokenIdentityProvider(ApiTokenRepository tokenRepository) {
      this.tokenRepository = tokenRepository;
   }

   @Override
   public Class<TokenAuthenticationRequest> getRequestType() {
      return TokenAuthenticationRequest.class;
   }

   @Override
   @ActivateRequestContext
   public Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request, AuthenticationRequestContext context) {
      logger.infof("ApiTokenIdentityProvider.authenticate() called with token: %s", request.getToken().getToken());

//      logger.infof("Running blocking authentication for token: %s", request.getToken().getToken());
//      ApiToken apiToken = tokenRepository.findByToken(request.getToken().getToken());
//      if (apiToken != null && apiToken.isValid()) {
//         return Uni.createFrom().item(QuarkusSecurityIdentity.builder()
//               .setPrincipal(new QuarkusPrincipal(apiToken.user.username))
//               .addCredential(request.getToken())
//               .build());
//      }
//      throw new AuthenticationFailedException("Token is invalid or expired: " + request.getToken().getToken());


      Context vContext = Vertx.currentContext();
      Future<QuarkusSecurityIdentity> future = vContext.executeBlocking(() -> {
         logger.infof("Running blocking authentication for token: %s", request.getToken().getToken());
         ApiToken apiToken = tokenRepository.findByToken(request.getToken().getToken());
         if (apiToken != null && apiToken.isValid()) {
            logger.infof("ApiTokenIdentityProvider.authenticate(): Found valid API token: %s", apiToken.token);

            return QuarkusSecurityIdentity.builder()
                  .setPrincipal(new QuarkusPrincipal(apiToken.user.username))
                  .addCredential(request.getToken())
                  .build();
         }
         // Authentication failed
         logger.warnf("ApiTokenIdentityProvider.authenticate(): Invalid or expired API token: %s", request.getToken().getToken());
         return null;
      });
      return Uni.createFrom().completionStage(future::toCompletionStage);

//      return context.runBlocking(() -> {
//         logger.infof("Running blocking authentication for token: %s", request.getToken().getToken());
//         ApiToken apiToken = tokenRepository.findByToken(request.getToken().getToken());
//         if (apiToken != null && apiToken.isValid()) {
//            logger.debugf("ApiTokenIdentityProvider.authenticate(): Found valid API token: %s", apiToken.token);
//
//            return QuarkusSecurityIdentity.builder()
//                  .setPrincipal(new QuarkusPrincipal(apiToken.user.username))
//                  .addCredential(request.getToken())
//                  .build();
//         }
//         // Authentication failed
//         logger.warnf("ApiTokenIdentityProvider.authenticate(): Invalid or expired API token: %s", request.getToken().getToken());
//         return null;
//      });
   }
}
