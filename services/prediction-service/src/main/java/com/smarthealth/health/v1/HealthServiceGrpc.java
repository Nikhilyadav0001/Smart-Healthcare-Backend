package com.smarthealth.health.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class HealthServiceGrpc {

  private HealthServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "health.v1.HealthService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.smarthealth.health.v1.GetLatestRecordRequest,
      com.smarthealth.health.v1.GetLatestRecordResponse> getGetLatestRecordMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetLatestRecord",
      requestType = com.smarthealth.health.v1.GetLatestRecordRequest.class,
      responseType = com.smarthealth.health.v1.GetLatestRecordResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.smarthealth.health.v1.GetLatestRecordRequest,
      com.smarthealth.health.v1.GetLatestRecordResponse> getGetLatestRecordMethod() {
    io.grpc.MethodDescriptor<com.smarthealth.health.v1.GetLatestRecordRequest, com.smarthealth.health.v1.GetLatestRecordResponse> getGetLatestRecordMethod;
    if ((getGetLatestRecordMethod = HealthServiceGrpc.getGetLatestRecordMethod) == null) {
      synchronized (HealthServiceGrpc.class) {
        if ((getGetLatestRecordMethod = HealthServiceGrpc.getGetLatestRecordMethod) == null) {
          HealthServiceGrpc.getGetLatestRecordMethod = getGetLatestRecordMethod =
              io.grpc.MethodDescriptor.<com.smarthealth.health.v1.GetLatestRecordRequest, com.smarthealth.health.v1.GetLatestRecordResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetLatestRecord"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.smarthealth.health.v1.GetLatestRecordRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.smarthealth.health.v1.GetLatestRecordResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HealthServiceMethodDescriptorSupplier("GetLatestRecord"))
              .build();
        }
      }
    }
    return getGetLatestRecordMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.smarthealth.health.v1.GetRecordRequest,
      com.smarthealth.health.v1.GetRecordResponse> getGetRecordMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetRecord",
      requestType = com.smarthealth.health.v1.GetRecordRequest.class,
      responseType = com.smarthealth.health.v1.GetRecordResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.smarthealth.health.v1.GetRecordRequest,
      com.smarthealth.health.v1.GetRecordResponse> getGetRecordMethod() {
    io.grpc.MethodDescriptor<com.smarthealth.health.v1.GetRecordRequest, com.smarthealth.health.v1.GetRecordResponse> getGetRecordMethod;
    if ((getGetRecordMethod = HealthServiceGrpc.getGetRecordMethod) == null) {
      synchronized (HealthServiceGrpc.class) {
        if ((getGetRecordMethod = HealthServiceGrpc.getGetRecordMethod) == null) {
          HealthServiceGrpc.getGetRecordMethod = getGetRecordMethod =
              io.grpc.MethodDescriptor.<com.smarthealth.health.v1.GetRecordRequest, com.smarthealth.health.v1.GetRecordResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetRecord"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.smarthealth.health.v1.GetRecordRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.smarthealth.health.v1.GetRecordResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HealthServiceMethodDescriptorSupplier("GetRecord"))
              .build();
        }
      }
    }
    return getGetRecordMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static HealthServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HealthServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HealthServiceStub>() {
        @java.lang.Override
        public HealthServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HealthServiceStub(channel, callOptions);
        }
      };
    return HealthServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static HealthServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HealthServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HealthServiceBlockingV2Stub>() {
        @java.lang.Override
        public HealthServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HealthServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return HealthServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static HealthServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HealthServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HealthServiceBlockingStub>() {
        @java.lang.Override
        public HealthServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HealthServiceBlockingStub(channel, callOptions);
        }
      };
    return HealthServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static HealthServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HealthServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HealthServiceFutureStub>() {
        @java.lang.Override
        public HealthServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HealthServiceFutureStub(channel, callOptions);
        }
      };
    return HealthServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void getLatestRecord(com.smarthealth.health.v1.GetLatestRecordRequest request,
        io.grpc.stub.StreamObserver<com.smarthealth.health.v1.GetLatestRecordResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetLatestRecordMethod(), responseObserver);
    }

    /**
     */
    default void getRecord(com.smarthealth.health.v1.GetRecordRequest request,
        io.grpc.stub.StreamObserver<com.smarthealth.health.v1.GetRecordResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetRecordMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service HealthService.
   */
  public static abstract class HealthServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return HealthServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service HealthService.
   */
  public static final class HealthServiceStub
      extends io.grpc.stub.AbstractAsyncStub<HealthServiceStub> {
    private HealthServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HealthServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HealthServiceStub(channel, callOptions);
    }

    /**
     */
    public void getLatestRecord(com.smarthealth.health.v1.GetLatestRecordRequest request,
        io.grpc.stub.StreamObserver<com.smarthealth.health.v1.GetLatestRecordResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetLatestRecordMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getRecord(com.smarthealth.health.v1.GetRecordRequest request,
        io.grpc.stub.StreamObserver<com.smarthealth.health.v1.GetRecordResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetRecordMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service HealthService.
   */
  public static final class HealthServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<HealthServiceBlockingV2Stub> {
    private HealthServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HealthServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HealthServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    public com.smarthealth.health.v1.GetLatestRecordResponse getLatestRecord(com.smarthealth.health.v1.GetLatestRecordRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetLatestRecordMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.smarthealth.health.v1.GetRecordResponse getRecord(com.smarthealth.health.v1.GetRecordRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetRecordMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service HealthService.
   */
  public static final class HealthServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<HealthServiceBlockingStub> {
    private HealthServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HealthServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HealthServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.smarthealth.health.v1.GetLatestRecordResponse getLatestRecord(com.smarthealth.health.v1.GetLatestRecordRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetLatestRecordMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.smarthealth.health.v1.GetRecordResponse getRecord(com.smarthealth.health.v1.GetRecordRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetRecordMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service HealthService.
   */
  public static final class HealthServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<HealthServiceFutureStub> {
    private HealthServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HealthServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HealthServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.smarthealth.health.v1.GetLatestRecordResponse> getLatestRecord(
        com.smarthealth.health.v1.GetLatestRecordRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetLatestRecordMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.smarthealth.health.v1.GetRecordResponse> getRecord(
        com.smarthealth.health.v1.GetRecordRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetRecordMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_LATEST_RECORD = 0;
  private static final int METHODID_GET_RECORD = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_LATEST_RECORD:
          serviceImpl.getLatestRecord((com.smarthealth.health.v1.GetLatestRecordRequest) request,
              (io.grpc.stub.StreamObserver<com.smarthealth.health.v1.GetLatestRecordResponse>) responseObserver);
          break;
        case METHODID_GET_RECORD:
          serviceImpl.getRecord((com.smarthealth.health.v1.GetRecordRequest) request,
              (io.grpc.stub.StreamObserver<com.smarthealth.health.v1.GetRecordResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetLatestRecordMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.smarthealth.health.v1.GetLatestRecordRequest,
              com.smarthealth.health.v1.GetLatestRecordResponse>(
                service, METHODID_GET_LATEST_RECORD)))
        .addMethod(
          getGetRecordMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.smarthealth.health.v1.GetRecordRequest,
              com.smarthealth.health.v1.GetRecordResponse>(
                service, METHODID_GET_RECORD)))
        .build();
  }

  private static abstract class HealthServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    HealthServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.smarthealth.health.v1.HealthProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("HealthService");
    }
  }

  private static final class HealthServiceFileDescriptorSupplier
      extends HealthServiceBaseDescriptorSupplier {
    HealthServiceFileDescriptorSupplier() {}
  }

  private static final class HealthServiceMethodDescriptorSupplier
      extends HealthServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    HealthServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (HealthServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new HealthServiceFileDescriptorSupplier())
              .addMethod(getGetLatestRecordMethod())
              .addMethod(getGetRecordMethod())
              .build();
        }
      }
    }
    return result;
  }
}
