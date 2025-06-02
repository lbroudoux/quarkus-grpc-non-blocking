package org.acme.example.service;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GlobalInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.example.model.ApiToken;
import org.acme.example.repository.ApiTokenRepository;
import org.jboss.logging.Logger;

/**
 * Interceptor to handle authentication for gRPC server calls.
 * @author laurent
 */
//@GlobalInterceptor
//@ApplicationScoped
public class GrpcAuthServerInterceptor implements ServerInterceptor {

   /** Get a JBoss logging logger. */
   private final Logger logger = Logger.getLogger(getClass());

   public static final Metadata.Key<String> AUTHORIZATION_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

   private final ApiTokenRepository tokenRepository;

   /**
    * Build a new gRPC authentication server interceptor with mandatory dependencies.
    * @param tokenRepository The repository to access API tokens.
    */
   public GrpcAuthServerInterceptor(ApiTokenRepository tokenRepository) {
      this.tokenRepository = tokenRepository;
   }

   @Override
   public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
         Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {

      // Check for the presence of an Authorization header
      String token = metadata.get(AUTHORIZATION_KEY);
      if (token != null && token.startsWith("Bearer ")) {
         token = token.substring(7);
         logger.debugf("Found authentication token in gRPC call: %s", token);

         ApiToken apiToken = tokenRepository.findByToken(token);
         if (apiToken != null && apiToken.isValid()) {
            return serverCallHandler.startCall(serverCall, metadata);
         }

         logger.warnf("Invalid or expired API token: %s", token);
      }
      logger.error("No valid API token found in gRPC call, rejecting request");
      throw new StatusRuntimeException(Status.FAILED_PRECONDITION);
   }
}
