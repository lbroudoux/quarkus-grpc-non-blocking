package org.acme.example.service;

import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import org.jboss.logging.Logger;

import java.util.Set;

//@Alternative
//@Priority(1)
//@ApplicationScoped
public class CustomHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   @Override
   public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
      return Set.of(TokenAuthenticationRequest.class);
   }

   @Override
   public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
      return HttpAuthenticationMechanism.super.getCredentialTransport(context);
   }

   @Override
   public Uni<ChallengeData> getChallenge(RoutingContext context) {
      return null;
   }

   @Override
   public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
      logger.infof("CustomHttpAuthenticationMechanism.authenticate() called with context: %s", context);

//      logger.infof("Context request headers are %s", context.request().headers());
//      context.request().body().onSuccess(h -> logger.infof("Context body onSuccess: %s", h.toString()))
//            .onFailure(t -> logger.error("Failed to read context body", t));

      if (context.request().getHeader("Authorization") != null) {
         String token = context.request().getHeader("Authorization").substring("Bearer ".length());
         logger.infof("CustomHttpAuthenticationMechanism.authenticate() called with token: %s", token);
         TokenAuthenticationRequest request = new TokenAuthenticationRequest(new TokenCredential(token, "micepe-api-token"));
         return identityProviderManager.authenticate(request);
      }
      logger.error("CustomHttpAuthenticationMechanism.authenticate() called without a valid token");
      return null;
   }
}
