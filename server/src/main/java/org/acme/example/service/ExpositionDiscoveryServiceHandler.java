package org.acme.example.service;

import io.smallrye.common.annotation.RunOnVirtualThread;
import org.acme.example.discovery.v1.ExpositionDiscoveryRequest;
import org.acme.example.discovery.v1.ExpositionDiscoveryResponse;
import org.acme.example.discovery.v1.ExpositionDiscoveryServiceGrpc;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.control.ActivateRequestContext;
import org.acme.example.model.Exposition;
import org.acme.example.repository.ExpositionRepository;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * gRPC service handler for exposition discovery requests.
 * @author laurent
 */
@GrpcService
public class ExpositionDiscoveryServiceHandler extends ExpositionDiscoveryServiceGrpc.ExpositionDiscoveryServiceImplBase {

    /** Get a JBoss logging logger. */
    private final Logger logger = Logger.getLogger(getClass());

    private final ExpositionRepository expositionRepository;


    public ExpositionDiscoveryServiceHandler(ExpositionRepository expositionRepository) {
        this.expositionRepository = expositionRepository;
    }

    @Override
    @Authenticated
    @RunOnVirtualThread
    @ActivateRequestContext
    public void fetchExpositions(ExpositionDiscoveryRequest request, StreamObserver<ExpositionDiscoveryResponse> responseObserver) {
        logger.infof("Received ExpositionDiscoveryRequest for gatewayId: %s", request.getGatewayId());
        logger.infof("Executing on thread: %s", Thread.currentThread().getName());

        ExpositionDiscoveryResponse.Builder builder = ExpositionDiscoveryResponse.newBuilder();

        List<Exposition> expositions = expositionRepository.findAll().list();
        for (Exposition exposition : expositions) {
            // Assuming Exposition has a method to convert to gRPC representation
            builder.addExpositions(grpcExpositionFromModel(exposition));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private org.acme.example.discovery.v1.Exposition grpcExpositionFromModel(Exposition exposition) {
        // Convert the Exposition model to its gRPC representation
        // This is a placeholder method; implement the actual conversion logic
        return org.acme.example.discovery.v1.Exposition.newBuilder()
              .setId(exposition.id.toString())
              .setName(exposition.name)
              .build();
    }
}
