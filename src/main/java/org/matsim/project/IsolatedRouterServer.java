package org.matsim.project;

import com.google.inject.Key;
import com.google.inject.name.Names;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
//import isolated.proto.IsolatedRouterProto;
import isolated.proto.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.routes.AbstractNetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource.defaultVehicle;
import static org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource.fromVehiclesData;
import static org.matsim.vehicles.VehicleUtils.getVehicleId;

public class IsolatedRouterServer {
    public static void main(String[] args) throws IOException, InterruptedException {


        Config config;
        if ( args==null || args.length==0 || args[0]==null ){
            config = ConfigUtils.loadConfig( "scenarios/equil/config1.xml" );
        } else {
            config = ConfigUtils.loadConfig( args );
        }


        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        //config.qsim().setVehiclesSource(fromVehiclesData);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controller controller = ControllerUtils.createController(scenario);

        TravelTimeCalculator ttc = controller.getInjector().getInstance(Key.get(TravelTimeCalculator.class, Names.named("car")));

        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(ttc);

        RoutingModule carRouter = controller.getInjector().getInstance(
                Key.get(RoutingModule.class, Names.named("car")));


        int port = 50051;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        Server server = ServerBuilder.forPort(port)
                .addService(new RoutingService(scenario,carRouter,ttc,eventsManager))
                .build()
                .start();

        System.out.println("CarRouter gRPC Server läuft auf Port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println(">> Shutdown-Hook: Server wird beendet …");
            server.shutdown();
        }));

        server.awaitTermination();
    }

    static class RoutingService extends RoutingGrpc.RoutingImplBase {

        Scenario scenario;
        RoutingModule carRouter;
        TravelTimeCalculator ttc;
        EventsManager eventsManager;

        RoutingService(Scenario scenario, RoutingModule carRouter, TravelTimeCalculator ttc, EventsManager eventsManager) {
            this.scenario = scenario;
            this.carRouter = carRouter;
            this.ttc = ttc;
            this.eventsManager = eventsManager;
            scenario.getVehicles().addVehicleType(VehicleUtils.createDefaultVehicleType());

        }



        @Override
        public void getRoute(RouteRequest req, StreamObserver<RouteResponse> respObs) {
//            System.out.println("getRoute Aufruf");
//            System.out.println("Vehicles: " + scenario.getVehicles());
            String fromFacility = req.getFromFacility();
            String toFacility = req.getToFacility();
            double departureTime = req.getDepartureTime();
            String personId = req.getPersonId();
            Person person = null;
            if (!personId.isEmpty()) {
                person = scenario.getPopulation().getPersons().get(Id.createPersonId(personId));
                if (!scenario.getVehicles().getVehicles().containsKey(Id.createVehicleId(personId))) {
                    Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.createVehicleId(person.getId()), VehicleUtils.createDefaultVehicleType());
                    scenario.getVehicles().addVehicle(vehicle);
                    Map<String, Id<Vehicle>> modeToVehicle = Map.of("car", vehicle.getId());
                    VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, modeToVehicle);
                }
            }
            Person finalPerson = person;

            RoutingRequest request = new RoutingRequest() {
                @Override
                public Facility getFromFacility() {
                    return new LinkWrapperFacility(scenario.getNetwork().getLinks().get(Id.createLinkId(fromFacility)));
                }

                @Override
                public Facility getToFacility() {
                    return new LinkWrapperFacility(scenario.getNetwork().getLinks().get(Id.createLinkId(toFacility)));
                }

                @Override
                public double getDepartureTime() {
                    return departureTime;
                }

                @Override
                public Person getPerson() {
                    return finalPerson;
                }

                @Override
                public Attributes getAttributes() {
                    // Keine zusätzlichen Attribute für die Anfrage
                    return null;
                }
            };

            System.out.println("calcRoute(...) wird aufgerufen");

            List<? extends PlanElement> planElementsCar = carRouter.calcRoute(request);

//            System.out.println("calcRoute(...) ist durchgelaufen");
            System.out.println("Ergebnis: " + planElementsCar);

            //Leg
            Leg leg = (Leg) planElementsCar.getFirst();
            //Leg - Mode
            String mode = leg.getMode();
            String routingMode = leg.getRoutingMode();
            //Leg - Times
            double legDepartureTime = leg.getDepartureTime().seconds();
            double legTravelTime = leg.getTravelTime().seconds();
            double arrivalTime = legDepartureTime + legTravelTime;
            //Leg - Route
            String startLinkId = leg.getRoute().getStartLinkId().toString();
            String endLinkId = leg.getRoute().getEndLinkId().toString();
            double routeTravelTime = leg.getRoute().getTravelTime().seconds();
            double distance = leg.getRoute().getDistance();
            double travelCost = 0;

            org.matsim.api.core.v01.population.Route route = leg.getRoute();
            if (route instanceof NetworkRoute netRoute) {
                travelCost = netRoute.getTravelCost();
            } else {
                travelCost = 0;
            }
            String routeDescription = leg.getRoute().getRouteDescription();
            //Attributes attr = leg.getAttributes();

//            System.out.println("leg.getRoutingMode(): " + routingMode);
            if (routingMode == null) {
                routingMode = "";
            }

//            System.out.println("Leg leg: " + leg);
//            System.out.println("leg.getRoute(): " + leg.getRoute());
//            System.out.println(mode + " " + routingMode + " " + legDepartureTime + " " + legTravelTime + " " + arrivalTime + " " +
//                    startLinkId + " " + endLinkId + " " + routeTravelTime + " " + distance + " " + routeDescription + " " + travelCost);

            RouteResponse response = RouteResponse.newBuilder()
                .setModes(Modes.newBuilder()
                        .setMode(mode)
                        .setRoutingMode(routingMode))
                .setTimes(Times.newBuilder()
                        .setDepartureTime(legDepartureTime)
                        .setLegTravelTime(legTravelTime)
                        .setArrivalTime(arrivalTime))
                .setRoute(Route.newBuilder()
                        .setStartLinkId(startLinkId)
                        .setEndLinkId(endLinkId)
                        .setRouteTravelTime(routeTravelTime)
                        .setDistance(distance)
                        .setTravelCost(travelCost)
                        .setRouteDescription(routeDescription))
                .build();

            System.out.println("Antwort wird versendet");

            respObs.onNext(response);
            respObs.onCompleted();
        }

        @Override
        public void updateRouter(EventMessage eM, StreamObserver<EventReceived> respObs) {

            if (eM.getType().equals("entered link")) {
                LinkEnterEvent e = new LinkEnterEvent(eM.getTime(), Id.createVehicleId(eM.getVehicleId()), Id.createLinkId(eM.getLinkId()));
//                System.out.println("LinkEnterEvent: " + e);
                eventsManager.processEvent(e);
            } else if (eM.getType().equals("left link")) {
                LinkLeaveEvent e = new LinkLeaveEvent(eM.getTime(), Id.createVehicleId(eM.getVehicleId()), Id.createLinkId(eM.getLinkId()));
//                System.out.println("LinkLeftEvent: " + e);
                eventsManager.processEvent(e);
            } else if (eM.getType().equals("arrival")) {
                PersonArrivalEvent e = new PersonArrivalEvent(eM.getTime(), Id.createPersonId(eM.getAgentId()), Id.createLinkId(eM.getLinkId()), eM.getLegMode());
//                System.out.println("PersonArrivalEvent: " + e);
                eventsManager.processEvent(e);
            } else if (eM.getType().equals("departure")) {
                PersonDepartureEvent e = new PersonDepartureEvent(eM.getTime(), Id.createPersonId(eM.getAgentId()), Id.createLinkId(eM.getLinkId()), eM.getLegMode(), eM.getRoutingMode());
//                System.out.println("PersonDepartureEvent: " + e);
                eventsManager.processEvent(e);
            } else if (eM.getType().equals("vehicle enters traffic")) {
                VehicleEntersTrafficEvent e = new VehicleEntersTrafficEvent(eM.getTime(), Id.createPersonId(eM.getDriverId()), Id.createLinkId(eM.getLinkId()), Id.createVehicleId(eM.getVehicleId()), eM.getNetworkMode(), eM.getRelativePositionOnLink());
//                System.out.println("VehicleEntersTrafficEvent: " + e);
                eventsManager.processEvent(e);
            } else if (eM.getType().equals("vehicle leaves traffic")) {
                VehicleLeavesTrafficEvent e = new VehicleLeavesTrafficEvent(eM.getTime(), Id.createPersonId(eM.getDriverId()), Id.createLinkId(eM.getLinkId()), Id.createVehicleId(eM.getVehicleId()), eM.getNetworkMode(), eM.getRelativePositionOnLink());
//                System.out.println("VehicleLeavesTrafficEvent: " + e);
                eventsManager.processEvent(e);
            }


            EventReceived response = EventReceived.newBuilder()
                    .setReceived(eM.getType() + " Event Received")
                    .build();

            respObs.onNext(response);
            respObs.onCompleted();

        }

        }
    }

