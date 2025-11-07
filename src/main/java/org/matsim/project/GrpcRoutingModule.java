package org.matsim.project;

import io.grpc.stub.StreamObserver;
import isolated.proto.RouteRequest;
import isolated.proto.RouteResponse;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.List;

public class GrpcRoutingModule implements RoutingModule {

    private final IsolatedRouterClient client; // dein gRPC-Client, z. B. mit Channel und Stub

    @Inject
    public GrpcRoutingModule(IsolatedRouterClient client) {
        this.client = client;
    }

    @Override
    public List<? extends PlanElement> calcRoute(RoutingRequest request) {

        final Facility fromFacility = request.getFromFacility();
        final Facility toFacility = request.getToFacility();
        final double departureTime = request.getDepartureTime();
        final Person person = request.getPerson();

        // 1️⃣ Anfrage an gRPC senden
        //List<? extends PlanElement> response = client.getRoute("car", fromFacility, toFacility, departureTime, person, null);

        return client.getRoute("car", fromFacility, toFacility, departureTime, person, null);
    }
}
