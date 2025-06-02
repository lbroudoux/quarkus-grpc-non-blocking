# quarkus-grpc-non-blocking

This project is a reproducer for a strange behaviour detected in Quarkus regarding gRPC non-blocking calls.
See the Zulip thread here: https://quarkusio.zulipchat.com/#narrow/channel/187030-users/topic/gRPC.20custom.20auth.20and.20identity.20provider.20-.20blocking.20call/with/520821003

The goal of the application is to expose a gRPC service `ExpositionDiscoveryService` that must be called in a blocking way
because it uses blocking Panache repository.

This gRPC service is also secured using a custom authentication mechanism where an `Authorization` bearer token is expected
and checked against an `ApiToken` entity stored in a Panache repository.

As the thread in Zulip starts discussing blocking/non-blocking calls to the Panache layer in the custom authentication mechanism,
we realized that the gRPC service was not working as expected because always called in a non-blocking way.

While building this reproducer we highlighted that the `@Blocking` or `@RunOnVirtualThread` annotations on the gRPC service method
were not working as expected because the gRPC service was always called in a non-blocking way. Strangely, it seems to happen only when
the gRPC Java classes are coming from an external library (like `quarkus-grpc-non-blocking-api` in this case) and not from the same module.

Whilst we found this workaround, we still think that this is a bug in Quarkus and should be fixed: gRPC protobuf classes should be shareable
between modules and reused without changing the behavior of the gRPC service calls.

## Pre-requisites

This reproducer requires a Java 21 JDK and Maven to be built and run. It uses Quarkus 3.23.0.
You'll also need the `grpcurl` tool to call the gRPC service. You can install it using `brew install grpcurl` on macOS.

## Initial behavior: gRPC classes coming from the api module

Initial situation:
* The `server` module depends on the `quarkus-grpc-non-blocking-api` module which contains the gRPC service definition
* `ExpositionDiscoveryServiceHandler` is annotated with `@Authenticated`, `@RunOnVirtualThread` (or `@Blocking`) and `@ActivateRequestContext` (not sure it's mandatory)
* `GrpcAuthSecurityMechanism` is activated and produces a `TokenAuthenticationRequest`
* The `ApiTokenIdentityProvider` is registered and annotated with `@ActivateRequestContext` so that it can call the Panache entity manager without Transaction in a blocking way (different ways of doing that were tested and all worked)

Start building the whole project from root folder:

```shell
mvn clean install -DskipTests
```

Then, start the server application:

```shell    
cd server
mvn clean quarkus:dev
```

and you'll get the following output in console:

```shell
Listening for transport dt_socket at address: 5005
2025-06-02 10:58:06,666 WARN  [io.qua.grp.dep.GrpcServerProcessor] (build-40) At least one unused gRPC interceptor found: org.acme.example.service.GrpcAuthServerInterceptor. If there are meant to be used globally, annotate them with @GlobalInterceptor.
2025-06-02 10:58:07,627 INFO  [io.qua.dat.dep.dev.DevServicesDatasourceProcessor] (build-17) Dev Services for default datasource (postgresql) started - container ID is 9cc3d42c09b8
2025-06-02 10:58:07,628 INFO  [io.qua.hib.orm.dep.dev.HibernateOrmDevServicesProcessor] (build-32) Setting quarkus.hibernate-orm.schema-management.strategy=drop-and-create to initialize Dev Services managed database
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2025-06-02 10:58:08,699 INFO  [io.qua.grp.run.GrpcServerRecorder] (Quarkus Main Thread) Starting new Quarkus gRPC server (using Vert.x transport)...

2025-06-02 10:58:08,771 INFO  [io.quarkus] (Quarkus Main Thread) quarkus-grpc-non-blocking-server 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.23.0) started in 2.734s. Listening on: http://localhost:5555
2025-06-02 10:58:08,771 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2025-06-02 10:58:08,772 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [agroal, cdi, compose, grpc-server, hibernate-orm, hibernate-orm-panache, jdbc-postgresql, narayana-jta, security, smallrye-context-propagation, vertx]
```

Then, from another terminal, you can call the gRPC service using the `grpcurl` tool and get the following output:

```shell
$ grpcurl -plaintext -d '{"gatewayId": "test-gateway", "labels": {"test-label": "test-value"}}' -H 'Authorization: Bearer my-super-secret-token' localhost:5555 org.acme.example.discovery.v1.ExpositionDiscoveryService/FetchExpositions
==== OUTPUT ==== 
ERROR:
  Code: Unknown
  Message: io.quarkus.runtime.BlockingOperationNotAllowedException - Blocking security check attempted in code running on the event loop. Make the secured method return an async type, i.e. Uni, Multi or CompletionStage, or use an authentication mechanism that sets the SecurityIdentity in a blocking manner prior to delegating the call
```

On the server side console, you'll see these new lines appear:

```shell
2025-06-02 10:58:18,676 INFO  [org.acm.exa.ser.GrpcAuthSecurityMechanism] (vert.x-eventloop-thread-0) GrpcAuthSecurityMechanism.handles() called with metadata: Metadata(content-type=application/grpc,user-agent=grpcurl/1.8.9 grpc-go/1.57.0,te=trailers,grpc-accept-encoding=gzip,authorization=Bearer my-super-secret-token)
2025-06-02 10:58:18,677 INFO  [org.acm.exa.ser.GrpcAuthSecurityMechanism] (vert.x-eventloop-thread-0) GrpcAuthSecurityMechanism.createAuthenticationRequest() called with token: my-super-secret-token
2025-06-02 10:58:18,692 INFO  [org.acm.exa.ser.GrpcAuthSecurityMechanism] (vert.x-eventloop-thread-0) GrpcAuthSecurityMechanism.handles() called with metadata: Metadata(content-type=application/grpc,user-agent=grpcurl/1.8.9 grpc-go/1.57.0,te=trailers,grpc-accept-encoding=gzip,authorization=Bearer my-super-secret-token)
2025-06-02 10:58:18,692 INFO  [org.acm.exa.ser.GrpcAuthSecurityMechanism] (vert.x-eventloop-thread-0) GrpcAuthSecurityMechanism.createAuthenticationRequest() called with token: my-super-secret-token
```

You see that the security mechanism is invoked twice and each time on the Vert.x event loop thread.

## Fixed behavior: gRPC classes coming from the server module

For this new situation, we need to change a few stuffs:
* The `server` module no longer depends on the `quarkus-grpc-non-blocking-api` module. So we must comment lines 21-25 in the `server/pom.xml` file.
* The `server` module now compiles its own gRPC service definition in the `org.acme.example.discovery.v1` package. So we must rename the `server/src/main/proto/eds-v1.proto.not` file to `server/src/main/proto/eds-v1.proto`

Now, from the `server` folder, we can start the server application again:

```shell
mvn clean quarkus:dev
```

and you'll get the following output in console:

```shell
Listening for transport dt_socket at address: 5005
2025-06-02 11:19:30,426 WARN  [io.qua.grp.dep.GrpcServerProcessor] (build-58) At least one unused gRPC interceptor found: org.acme.example.service.GrpcAuthServerInterceptor. If there are meant to be used globally, annotate them with @GlobalInterceptor.
2025-06-02 11:19:31,308 INFO  [io.qua.dat.dep.dev.DevServicesDatasourceProcessor] (build-20) Dev Services for default datasource (postgresql) started - container ID is 9cc3d42c09b8
2025-06-02 11:19:31,309 INFO  [io.qua.hib.orm.dep.dev.HibernateOrmDevServicesProcessor] (build-6) Setting quarkus.hibernate-orm.schema-management.strategy=drop-and-create to initialize Dev Services managed database
__  ____  __  _____   ___  __ ____  ______ 
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
2025-06-02 11:19:32,492 INFO  [io.qua.grp.run.GrpcServerRecorder] (Quarkus Main Thread) Starting new Quarkus gRPC server (using Vert.x transport)...

2025-06-02 11:19:32,555 INFO  [io.quarkus] (Quarkus Main Thread) quarkus-grpc-non-blocking-server 1.0.0-SNAPSHOT on JVM (powered by Quarkus 3.23.0) started in 2.771s. Listening on: http://localhost:5555
2025-06-02 11:19:32,555 INFO  [io.quarkus] (Quarkus Main Thread) Profile dev activated. Live Coding activated.
2025-06-02 11:19:32,556 INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [agroal, cdi, compose, grpc-server, hibernate-orm, hibernate-orm-panache, jdbc-postgresql, narayana-jta, security, smallrye-context-propagation, vertx]
```

Then, from another terminal, you can call the gRPC service using the `grpcurl` tool and get the following output:

```shell
$ grpcurl -plaintext -d '{"gatewayId": "test-gateway", "labels": {"test-label": "test-value"}}' -H 'Authorization: Bearer my-super-secret-token' localhost:5555 org.acme.example.discovery.v1.ExpositionDiscoveryService/FetchExpositions
==== OUTPUT ==== 
{
  "expositions": [
    {
      "id": "1",
      "name": "My exposition"
    }
  ]
}
```

It works! :tada:
On the server side console, you'll see these new lines appear:

```shell
2025-06-02 11:20:10,061 INFO  [org.acm.exa.ser.GrpcAuthSecurityMechanism] (vert.x-eventloop-thread-0) GrpcAuthSecurityMechanism.handles() called with metadata: Metadata(content-type=application/grpc,user-agent=grpcurl/1.8.9 grpc-go/1.57.0,te=trailers,grpc-accept-encoding=gzip,authorization=Bearer my-super-secret-token)
2025-06-02 11:20:10,061 INFO  [org.acm.exa.ser.GrpcAuthSecurityMechanism] (vert.x-eventloop-thread-0) GrpcAuthSecurityMechanism.createAuthenticationRequest() called with token: my-super-secret-token
2025-06-02 11:20:10,082 INFO  [org.acm.exa.ser.GrpcAuthSecurityMechanism] (quarkus-virtual-thread-0) GrpcAuthSecurityMechanism.handles() called with metadata: Metadata(content-type=application/grpc,user-agent=grpcurl/1.8.9 grpc-go/1.57.0,te=trailers,grpc-accept-encoding=gzip,authorization=Bearer my-super-secret-token)
2025-06-02 11:20:10,083 INFO  [org.acm.exa.ser.GrpcAuthSecurityMechanism] (quarkus-virtual-thread-0) GrpcAuthSecurityMechanism.createAuthenticationRequest() called with token: my-super-secret-token
2025-06-02 11:20:10,086 INFO  [org.acm.exa.ser.ApiTokenIdentityProvider_Subclass] (quarkus-virtual-thread-3) ApiTokenIdentityProvider.authenticate() called with token: my-super-secret-token
2025-06-02 11:20:10,087 INFO  [org.acm.exa.ser.ApiTokenIdentityProvider_Subclass] (vert.x-worker-thread-1) Running blocking authentication for token: my-super-secret-token
2025-06-02 11:20:10,270 INFO  [org.acm.exa.ser.ApiTokenIdentityProvider_Subclass] (vert.x-worker-thread-1) ApiTokenIdentityProvider.authenticate(): Found valid API token: my-super-secret-token
2025-06-02 11:20:10,271 INFO  [org.acm.exa.ser.ExpositionDiscoveryServiceHandler_Subclass] (quarkus-virtual-thread-3) Received ExpositionDiscoveryRequest for gatewayId: test-gateway
2025-06-02 11:20:10,271 INFO  [org.acm.exa.ser.ExpositionDiscoveryServiceHandler_Subclass] (quarkus-virtual-thread-3) Executing on thread: quarkus-virtual-thread-3
```

So this is now working as expected!
* We can see that the `GrpcAuthSecurityMechanism` is called on the Vert.x event loop thread and then the `ApiTokenIdentityProvider` is called on a Quarkus virtual thread
* However, we can see that the `GrpcAuthSecurityMechanism` is called twice on different threads. Looks like Quarkus is doing some retry?
* The `ExpositionDiscoveryServiceHandler` is then called on a Quarkus virtual thread and can safely call the Panache repository in a blocking way. (It would have been a `quarkus-worker-thread-x` with the `@Blocking` annotation instead of `@RunsOnVirtualThread`)

## Notes on security/custom authentication

We tried different approaches to implement the custom authentication mechanism and we found that the following worked:
* 1st: we tried using `@GlobalInterceptor` on the `GrpcAuthServerInterceptor` class and it was not working because always called in a non-blocking way
* 2nd (current): we used the custom `GrpcAuthSecurityMechanism` and `ApiTokenIdentityProvider` classes as explained in https://quarkus.io/guides/grpc-service-implementation#custom-auth-mechanism
* 3rd: we tried using the `CustomHttpAuthenticationMechanism extends HttpAuthenticationMechanism` (currently commented to avoid conflicts) in combination with the `ApiTokenIdentityProvider` and it was also working.