package org.acme;


import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import mcs.Mcs;
import mcs.MediaCastServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class Application {
    private final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private ScheduledExecutorService executors = Executors.newScheduledThreadPool(1);

    void onStart(@Observes StartupEvent ev) throws IOException {
        LOGGER.info("The application is starting...");
        new Thread(this::startSession).start();
       /* var totalDuration = 10 * 60 * 1000;
        var cps = 0.5;
        //Conccurent calls = cps * 160;
        var startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < totalDuration) {
            LOGGER.info("Running for {} seconds", (System.currentTimeMillis() - startTime) / 1000);
            try {
                new Thread(this::startSession).start();
                Thread.sleep((long) (1000 / cps));
            } catch (InterruptedException e) {
                // throw new RuntimeException(e);
            }
        }*/


    }

    private static AtomicInteger nextChannel = new AtomicInteger(0);
    private static ArrayList<ManagedChannel> channels = new ArrayList<>();

    private static ByteString data = ByteString.copyFrom(
            "1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh1280huoisadhuasoduhaoushdoaushdoiuahsduihausihdaoiuhdouashdiuahsduhasudh".getBytes());

    private void startSession() {

        var stub = MediaCastServiceGrpc.newStub(initializeChannel());
        var requestStreamObserver = stub.dialog(new StreamObserver<Mcs.DialogResponsePayload>() {
            @Override
            public void onNext(Mcs.DialogResponsePayload value) {
                LOGGER.debug("Received response from MCS: {}", value.getPayloadType());
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.error("Error while streaming media from MCS", t);
            }

            @Override
            public void onCompleted() {
                LOGGER.info("Client streaming complete");
            }
        });
        var uuid = UUID.randomUUID().toString();
        var currentTime = System.currentTimeMillis();
        LOGGER.debug("UUID: {}", uuid);

        executors.schedule(() -> {
            var request = Mcs.DialogRequestPayload.newBuilder()
                    .setPayloadType(Mcs.DialogRequestPayloadType.AUDIO_START)
                    .setUuid(uuid)
                    .setEventData(
                            "{\"mcc_metadata_homeDatacenter\":\"us-east-1\",\"request_type\":\"AGENT_ASSIST\",\"mode\":\"split\",\"app_id\":\"92231356-e4fd-43e8-98ca-5358e56409db\",\"client_state\":\"" +
                            uuid + "\"}")
                    .build();
            LOGGER.debug("Sending Audoi Start");
            requestStreamObserver.onNext(request);

        }, 0, java.util.concurrent.TimeUnit.MILLISECONDS);

        executors.schedule(() -> sendAudio(uuid, requestStreamObserver, currentTime), 160,
                java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void sendAudio(String uuid, StreamObserver<Mcs.DialogRequestPayload> requestStreamObserver,
                           long currentTime) {
        var request = Mcs.DialogRequestPayload.newBuilder()
                .setPayloadType(Mcs.DialogRequestPayloadType.AUDIO_SPLIT)
                .setUuid(uuid)
                .setAudioLeft(data)
                .setAudioRight(data)
                .setTimestamp(System.currentTimeMillis())
                .build();
        LOGGER.debug("Sending Audoi Split");

        requestStreamObserver.onNext(request);
        if (System.currentTimeMillis() - currentTime < 1000 * 60 * 2) {
            executors.schedule(() -> sendAudio(uuid, requestStreamObserver, currentTime), 160,
                    java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            executors.schedule(() -> {
                        var requestEnd = Mcs.DialogRequestPayload.newBuilder()
                                .setPayloadType(Mcs.DialogRequestPayloadType.AUDIO_END)
                                .setUuid(uuid)
                                .setEventData(
                                        "{\"mcc_metadata_homeDatacenter\":\"us-east-1\",\"request_type\":\"AGENT_ASSIST\",\"mode\":\"split\",\"app_id\":\"92231356-e4fd-43e8-98ca-5358e56409db\",\"client_state\":\"" +
                                        uuid + "\"}")
                                .setTimestamp(System.currentTimeMillis())
                                .build();
                        LOGGER.debug("Sending Audoi End");

                        requestStreamObserver.onNext(requestEnd);
                        requestStreamObserver.onCompleted();
                    }, 0,
                    java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    private ManagedChannel initializeChannel() {
        LOGGER.debug("Creating Channel");
        var idx = nextChannel.getAndIncrement();
        if (idx == 30) {
            idx = 0;
            nextChannel.set(1);
        }
        ManagedChannel channel;

        if (idx < channels.size()) {
            channel = channels.get(idx);
        } else {
            String apiUrl = "mcc.load-us1.rtmslab.net";//"nightly-load-mcc.loadus1.ciscoccservice.com";
            var port = 31400;
            ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(apiUrl, port);
            LOGGER.debug("Creating Channel 1");
            if (port == 443) {
                channelBuilder.useTransportSecurity();
            } else {
                channelBuilder.usePlaintext();
            }
            LOGGER.debug("Creating Channel 2");
            channel = channelBuilder.build();
            channels.add(channel);
        }
        LOGGER.debug("Created Channel");
        return channel;
    }


    void onStop(@Observes @Priority(1) ShutdownEvent ev) throws InterruptedException {
        LOGGER.info("The application shutdown event is received: {}", ev);

    }
}
