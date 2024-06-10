package org.acme;

import static mcs.Mcs.DialogRequestPayloadType.AUDIO_START;

import io.quarkus.grpc.GrpcService;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import java.util.concurrent.SubmissionPublisher;
import mcs.Mcs;
import mcs.MutinyMediaCastServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@GrpcService
public class MediaCastGrpcService extends MutinyMediaCastServiceGrpc.MediaCastServiceImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(MediaCastGrpcService.class);

    @Override
    @Blocking
    public Multi<Mcs.DialogResponsePayload> dialog(Multi<Mcs.DialogRequestPayload> request) {
        LOGGER.info("Received dialog request to stream media from MCS");
        return streamMedia(request);
    }

    public Multi<Mcs.DialogResponsePayload> streamMedia(Multi<Mcs.DialogRequestPayload> dialogRequestPayload) {
        var flowPublisher = new SubmissionPublisher<Mcs.DialogResponsePayload>();
        var context = Context.of("UUID", "UUID");
        dialogRequestPayload
                .runSubscriptionOn(Infrastructure.getDefaultExecutor())
                .subscribe()
                .with(context, payload -> {
                            context.put("UUID", payload.getUuid());
                            handlePayload(payload, flowPublisher);
                        },
                        error -> {
                            LOGGER.error("Stream Terminated with error for callId " + context.get("UUID"), error);
                            StackTraceElement[] stkTrace = error.getStackTrace();
                            var stackTraceStr = new StringBuilder("");
                            for (StackTraceElement stackTraceElement : stkTrace) {
                                stackTraceStr.append(stackTraceElement.toString());
                            }
                            LOGGER.error("Stream Terminated with error for callId " + context.get("UUID")
                                         + " Errro = " + stackTraceStr);

                        },
                        () -> {
                            LOGGER.info("Stream completed for callId " + context.get("UUID"));
                        });

        return Multi.createFrom().publisher(flowPublisher);
    }

    void handlePayload(Mcs.DialogRequestPayload payload,
                       SubmissionPublisher<Mcs.DialogResponsePayload> flowPublisher) {
        if (AUDIO_START.equals(payload.getPayloadType())) {
            //LOGGER.info("Creating streaming session for CallLegId: {}, Payload type: {} ",
            //        payload.getUuid(), payload.getPayloadType());
            flowPublisher.submit(Mcs.DialogResponsePayload.newBuilder()
                    .setPayloadType(Mcs.DialogResponsePayloadType.RESPONSE_END)
                    .setData("{\"status\":\"OK\"}")
                    .build());
            flowPublisher.close();
        } else {
            if(System.currentTimeMillis() - payload.getTimestamp() > 100) {
                LOGGER.info("Sending audio callLegId: {}, payloadType: {}, payload timestamp: {}, delay: {}",
                        payload.getUuid(), payload.getPayloadType(),
                        payload.getTimestamp(), System.currentTimeMillis() - payload.getTimestamp());
            }
        }
    }

}
