package org.matsim.project;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import isolated.proto.*;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;

public class IsolatedRouterClientWithEvents {

    public static String sentEnterLeaveEvent(String type, double time, String linkId, String vehicleId) {
        String host = "localhost";
        int port = 50051;
//        if (args.length > 0) host = args[0];
//        if (args.length > 1) port = Integer.parseInt(args[1]);

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        RoutingGrpc.RoutingBlockingStub stub = RoutingGrpc.newBlockingStub(channel);

        EventMessage em;
            em = EventMessage.newBuilder()
                    .setTime(time)
                    .setType(type)
                    .setVehicleId(vehicleId)
                    .setLinkId(linkId)
                    .build();

        EventReceived resp = stub.updateRouter(em);

//        System.out.println("== Antwort vom Server ==");
//        System.out.println(resp);

        channel.shutdown();

        return resp.toString();
    }

    public static String sentPersonEvent(String type, double time, String linkId, String agentId, String legMode, String routingMode) {
        String host = "localhost";
        int port = 50051;
//        if (args.length > 0) host = args[0];
//        if (args.length > 1) port = Integer.parseInt(args[1]);

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        RoutingGrpc.RoutingBlockingStub stub = RoutingGrpc.newBlockingStub(channel);

        EventMessage em;
        if (routingMode == null) {
            em = EventMessage.newBuilder()
                    .setTime(time)
                    .setType(type)
                    .setLinkId(linkId)
                    .setAgentId(agentId)
                    .setLegMode(legMode)
                    .build();
        }
        else {
            em = EventMessage.newBuilder()
                    .setTime(time)
                    .setType(type)
                    .setLinkId(linkId)
                    .setAgentId(agentId)
                    .setLegMode(legMode)
                    .setRoutingMode(routingMode)
                    .build();
            }

        EventReceived resp = stub.updateRouter(em);

//        System.out.println("== Antwort vom Server ==");
//        System.out.println(resp);

        channel.shutdown();

        return resp.toString();
    }

    public static String sentVehicleEvent(String type, double time, String linkId, String vehicleId, String driverId, String networkMode, double relativePositionOnLink) {
        String host = "localhost";
        int port = 50051;
//        if (args.length > 0) host = args[0];
//        if (args.length > 1) port = Integer.parseInt(args[1]);

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        RoutingGrpc.RoutingBlockingStub stub = RoutingGrpc.newBlockingStub(channel);

        EventMessage em;
            em = EventMessage.newBuilder()
                    .setTime(time)
                    .setType(type)
                    .setLinkId(linkId)
                    .setVehicleId(vehicleId)
                    .setDriverId(driverId)
                    .setRelativePositionOnLink(relativePositionOnLink)
                    .setNetworkMode(networkMode)
                    .build();

        EventReceived resp = stub.updateRouter(em);

//        System.out.println("== Antwort vom Server ==");
//        System.out.println(resp);

        channel.shutdown();

        return resp.toString();
    }
}



