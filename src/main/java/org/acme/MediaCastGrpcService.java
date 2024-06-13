package org.acme;

import static mcs.Mcs.DialogRequestPayloadType.AUDIO_START;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.inject.Inject;
import java.util.concurrent.SubmissionPublisher;
import mcs.Mcs;
import mcs.MediaCastServiceGrpc;
import mcs.MutinyMediaCastServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

//@GrpcService
public class MediaCastGrpcService extends MediaCastServiceGrpc.MediaCastServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaCastGrpcService.class);
    @Override
    public StreamObserver<Mcs.DialogRequestPayload> dialog(StreamObserver<Mcs.DialogResponsePayload> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(Mcs.DialogRequestPayload value) {
                LOGGER.info("Received dialog request to stream media from MCS");
                streamMedia(value, responseObserver);
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.error("Error while streaming media from MCS", t);
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                LOGGER.info("Client streaming complete");
                responseObserver.onCompleted();
            }
        };
    }

    private void streamMedia(Mcs.DialogRequestPayload requestPayload,
                             StreamObserver<Mcs.DialogResponsePayload> responseObserver) {

        if (AUDIO_START.equals(requestPayload.getPayloadType())) {
            responseObserver.onNext(
                    Mcs.DialogResponsePayload.newBuilder().setPayloadType(Mcs.DialogResponsePayloadType.RESPONSE_END)
                            .setData("{\"status\":\"OK\"}")
                            .build());
        } else {
            if (System.currentTimeMillis() - requestPayload.getTimestamp() > 100) {
                LOGGER.info("Sending audio callLegId: {}, payloadType: {}, payload timestamp: {}, delay: {}",
                        requestPayload.getUuid(), requestPayload.getPayloadType(),
                        requestPayload.getTimestamp(), System.currentTimeMillis() - requestPayload.getTimestamp());
            }
        }
    }
}
