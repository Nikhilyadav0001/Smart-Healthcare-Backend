package com.smarthealth.report.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class ReportServiceGrpc {

  private ReportServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "report.v1.ReportService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.smarthealth.report.v1.GenerateReportRequest,
      com.smarthealth.report.v1.GenerateReportResponse> getGenerateReportMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GenerateReport",
      requestType = com.smarthealth.report.v1.GenerateReportRequest.class,
      responseType = com.smarthealth.report.v1.GenerateReportResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.smarthealth.report.v1.GenerateReportRequest,
      com.smarthealth.report.v1.GenerateReportResponse> getGenerateReportMethod() {
    io.grpc.MethodDescriptor<com.smarthealth.report.v1.GenerateReportRequest, com.smarthealth.report.v1.GenerateReportResponse> getGenerateReportMethod;
    if ((getGenerateReportMethod = ReportServiceGrpc.getGenerateReportMethod) == null) {
      synchronized (ReportServiceGrpc.class) {
        if ((getGenerateReportMethod = ReportServiceGrpc.getGenerateReportMethod) == null) {
          ReportServiceGrpc.getGenerateReportMethod = getGenerateReportMethod =
              io.grpc.MethodDescriptor.<com.smarthealth.report.v1.GenerateReportRequest, com.smarthealth.report.v1.GenerateReportResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GenerateReport"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.smarthealth.report.v1.GenerateReportRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.smarthealth.report.v1.GenerateReportResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ReportServiceMethodDescriptorSupplier("GenerateReport"))
              .build();
        }
      }
    }
    return getGenerateReportMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ReportServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ReportServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ReportServiceStub>() {
        @java.lang.Override
        public ReportServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ReportServiceStub(channel, callOptions);
        }
      };
    return ReportServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static ReportServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ReportServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ReportServiceBlockingV2Stub>() {
        @java.lang.Override
        public ReportServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ReportServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return ReportServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ReportServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ReportServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ReportServiceBlockingStub>() {
        @java.lang.Override
        public ReportServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ReportServiceBlockingStub(channel, callOptions);
        }
      };
    return ReportServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ReportServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ReportServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ReportServiceFutureStub>() {
        @java.lang.Override
        public ReportServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ReportServiceFutureStub(channel, callOptions);
        }
      };
    return ReportServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void generateReport(com.smarthealth.report.v1.GenerateReportRequest request,
        io.grpc.stub.StreamObserver<com.smarthealth.report.v1.GenerateReportResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGenerateReportMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ReportService.
   */
  public static abstract class ReportServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ReportServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ReportService.
   */
  public static final class ReportServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ReportServiceStub> {
    private ReportServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ReportServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ReportServiceStub(channel, callOptions);
    }

    /**
     */
    public void generateReport(com.smarthealth.report.v1.GenerateReportRequest request,
        io.grpc.stub.StreamObserver<com.smarthealth.report.v1.GenerateReportResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGenerateReportMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ReportService.
   */
  public static final class ReportServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<ReportServiceBlockingV2Stub> {
    private ReportServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ReportServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ReportServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    public com.smarthealth.report.v1.GenerateReportResponse generateReport(com.smarthealth.report.v1.GenerateReportRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGenerateReportMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service ReportService.
   */
  public static final class ReportServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ReportServiceBlockingStub> {
    private ReportServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ReportServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ReportServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.smarthealth.report.v1.GenerateReportResponse generateReport(com.smarthealth.report.v1.GenerateReportRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGenerateReportMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ReportService.
   */
  public static final class ReportServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ReportServiceFutureStub> {
    private ReportServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ReportServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ReportServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.smarthealth.report.v1.GenerateReportResponse> generateReport(
        com.smarthealth.report.v1.GenerateReportRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGenerateReportMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GENERATE_REPORT = 0;

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
        case METHODID_GENERATE_REPORT:
          serviceImpl.generateReport((com.smarthealth.report.v1.GenerateReportRequest) request,
              (io.grpc.stub.StreamObserver<com.smarthealth.report.v1.GenerateReportResponse>) responseObserver);
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
          getGenerateReportMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.smarthealth.report.v1.GenerateReportRequest,
              com.smarthealth.report.v1.GenerateReportResponse>(
                service, METHODID_GENERATE_REPORT)))
        .build();
  }

  private static abstract class ReportServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ReportServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.smarthealth.report.v1.ReportProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ReportService");
    }
  }

  private static final class ReportServiceFileDescriptorSupplier
      extends ReportServiceBaseDescriptorSupplier {
    ReportServiceFileDescriptorSupplier() {}
  }

  private static final class ReportServiceMethodDescriptorSupplier
      extends ReportServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ReportServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (ReportServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ReportServiceFileDescriptorSupplier())
              .addMethod(getGenerateReportMethod())
              .build();
        }
      }
    }
    return result;
  }
}
