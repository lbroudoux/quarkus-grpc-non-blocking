package org.acme.example.service;

import io.grpc.Metadata;
import io.quarkus.grpc.auth.GrpcSecurityMechanism;
import io.quarkus.security.credential.TokenCredential;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

@Singleton
public class GrpcAuthSecurityMechanism implements GrpcSecurityMechanism {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   public static final Metadata.Key<String> AUTHORIZATION_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

   @Override
   public boolean handles(Metadata metadata) {
      logger.infof("GrpcAuthSecurityMechanism.handles() called with metadata: %s", metadata);
      String token = metadata.get(AUTHORIZATION_KEY);
      return token != null && token.startsWith("Bearer ");
   }

   @Override
   public AuthenticationRequest createAuthenticationRequest(Metadata metadata) {
      String token = metadata.get(AUTHORIZATION_KEY);
      if (token == null || !token.startsWith("Bearer ")) {
         logger.error("GrpcAuthSecurityMechanism.createAuthenticationRequest() called without a valid token");
         return null;
      }
      token = token.substring(7);
      logger.infof("GrpcAuthSecurityMechanism.createAuthenticationRequest() called with token: %s", token);
      return new TokenAuthenticationRequest(new TokenCredential(token, "my-api-token"));
   }
}
