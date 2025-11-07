package org.matsim.project;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import isolated.proto.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.router.DefaultMainLegRouter;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.matsim.core.population.PopulationUtils.createLeg;

public class IsolatedRouterClient {

    private final RoutingGrpc.RoutingBlockingStub stub;
    private final ManagedChannel channel;

    public IsolatedRouterClient(String host, int port) {
        this.channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();
        this.stub = RoutingGrpc.newBlockingStub(channel);
    }

    public List<? extends PlanElement> getRoute(String mainMode, Facility fromFacility, Facility toFacility, double now, Person person, Attributes attributes) {

        System.out.println("RouteRequest wird gebaut...");
        
        String hasPerson = "";
        if (person != null) {hasPerson = person.getId().toString();}
        RouteRequest req = RouteRequest.newBuilder()
                .setFromFacility(fromFacility.getLinkId().toString()).setToFacility(toFacility.getLinkId().toString())
                .setDepartureTime(now).setPersonId(hasPerson)
                .build();

        System.out.println("...RouteRequest wurde gebaut und es folgt der getRoute-Serveraufruf... " + req);

        RouteResponse resp = stub.getRoute(req);

        System.out.println("...der Server Antwortet mit: OK!");
//        System.out.printf("Route:" + resp);

        List<PlanElement> result = new ArrayList<>();

        Leg newLeg = createLeg(resp.getModes().getMode());
        newLeg.setDepartureTime(resp.getTimes().getDepartureTime());
        newLeg.setTravelTime(resp.getTimes().getLegTravelTime());

        NetworkRoute matsimRoute = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId(resp.getRoute().getStartLinkId()),
                Id.createLinkId(resp.getRoute().getEndLinkId()));
        matsimRoute.setDistance(resp.getRoute().getDistance());
        matsimRoute.setTravelTime(resp.getRoute().getRouteTravelTime());
        matsimRoute.setRouteDescription(resp.getRoute().getRouteDescription());
        matsimRoute.setTravelCost(resp.getRoute().getTravelCost());

        newLeg.setRoute(matsimRoute);

        result.add(newLeg);

        System.out.println("...Client ist durch");
        System.out.println("Ergebnis: " + result);

        return result;
    }


    public void close() throws IOException {
        channel.shutdown();
        try { channel.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ex) { channel.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}

