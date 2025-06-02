package org.acme.example;

import org.acme.example.discovery.v1.ExpositionDiscoveryServiceGrpc;
import org.acme.example.discovery.v1.ExpositionDiscoveryRequest;
import org.acme.example.discovery.v1.ExpositionDiscoveryResponse;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
public class ExpositionDiscoveryServiceTest {

   @GrpcClient
   ExpositionDiscoveryServiceGrpc.ExpositionDiscoveryServiceBlockingStub discoveryService;

   @Test
   void testDiscoveryService() {
      ExpositionDiscoveryRequest discoveryRequest = ExpositionDiscoveryRequest.newBuilder()
            .setGatewayId("test-gateway")
            .putLabels("test-label", "test-value")
            .build();

      try {
         ExpositionDiscoveryResponse discoveryResponse = discoveryService.fetchExpositions(discoveryRequest);
         System.err.println("Fetched expositions during startup: " + discoveryResponse.getExpositionsCount());
      } catch (Throwable t) {
         t.printStackTrace();
         fail("Failed to fetch expositions during startup");
      }
   }
}
