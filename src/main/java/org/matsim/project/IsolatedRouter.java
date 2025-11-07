package org.matsim.project;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import isolated.proto.RouteRequest;
import isolated.proto.RouteResponse;
import isolated.proto.RoutingGrpc;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.List;

public class IsolatedRouter {
    public static RouteResponse calcRoute(String mainMode, Facility fromFacility, Facility toFacility, double now, Person person, Attributes attributes) {
        String host = "localhost";
        int port = 50051;
//        if (args.length > 0) host = args[0];
//        if (args.length > 1) port = Integer.parseInt(args[1]);

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        RoutingGrpc.RoutingBlockingStub stub = RoutingGrpc.newBlockingStub(channel);

        RouteRequest req = RouteRequest.newBuilder()
                .setFromFacility(fromFacility.toString()).setToFacility(toFacility.toString())
                .setDepartureTime(now)
                .build();

        RouteResponse resp = stub.getRoute(req);

        System.out.println("== Antwort vom Server ==");
        System.out.printf("Route:" + resp);

        channel.shutdown();
        return resp;
    }
}

