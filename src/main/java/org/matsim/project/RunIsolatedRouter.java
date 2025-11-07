package org.matsim.project;

// Importiert die SwissRailRaptor-Komponenten der SBB-Erweiterung (Raptor-Algorithmus für ÖV-Routing).

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;
// Google Guice wird von MATSim genutzt, um Abhängigkeiten (Module, Services) bereitzustellen.

import org.apache.commons.compress.harmony.unpack200.Segment;
import org.apache.logging.log4j.Level;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
// Kern-APIs von MATSim: Ids, Szenario, Personen, Plan-Elemente.

import org.matsim.contrib.analysis.vsp.traveltimedistance.HereMapsLayer;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.*;
// MATSim-Core: Config laden, Controller starten, Output-Handling.

import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.*;
// Router-Klassen: Facility-Hülle um Netzwerkkanten, RoutingModule (z. B. pt, car, walk), RoutingRequest (Abfrage).

import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.*;
// Hilfsklassen: Szenario laden, IO, Beispiel-Szenarien, generische Facilities, Attribut-Container.

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.matsim.core.controler.Controler.DefaultFiles.network;
// Standard Java: URL-Handling und List.


// Standard Java: URL-Handling und List.

public class RunIsolatedRouter {

    public static void main(String[] args) {

//        System.out.println( " Starte den Isolated Router! " );

//        // 1) Beispiel-Szenario laden (z. B. 'equil' aus MATSim-Beispielen)
//        URL scenarioURL = ExamplesUtils.getTestScenarioURL("equil");
//        Config config = ConfigUtils.loadConfig(scenarioURL + "/config.xml");
//        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
//
//        // 2) Szenario erzeugen
//        Scenario scenario = ScenarioUtils.loadScenario(config);
//
//        // 3) Controller aufbauen (liefert auch die RoutingModule)
//        Controller controller = ControllerUtils.createController(scenario);

//        // 1) Beispiel-Szenario laden (MATSim liefert "pt-tutorial" als kleines Demo-ÖV-Netz)
//        URL ptScenarioURL = ExamplesUtils.getTestScenarioURL("berlin");
//
//        // 2) Config aus dem Beispiel-Szenario laden (0.config.xml innerhalb des pt-tutorial)
//        Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ptScenarioURL, "config.xml"));
//        config.controller().setLastIteration(1);
//
//        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
//        Scenario scenario = ScenarioUtils.loadScenario(config);
//        Controller controller = ControllerUtils.createController(scenario);

        Config config;
        if ( args==null || args.length==0 || args[0]==null ){
            config = ConfigUtils.loadConfig( "scenarios/equil/config1.xml" );
        } else {
            config = ConfigUtils.loadConfig( args );
        }

        //config.controller().setLastIteration(1);

        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controller controller = ControllerUtils.createController(scenario);

        Person person = scenario.getPopulation().getPersons().values().iterator().next();
        scenario.getVehicles().addVehicleType(VehicleUtils.createDefaultVehicleType());
        Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.createVehicleId(person.getId()), VehicleUtils.createDefaultVehicleType());
        scenario.getVehicles().addVehicle(vehicle);

// 4️⃣ Vehicle-IDs pro Modus in Map ablegen
        Map<String, Id<Vehicle>> modeToVehicle = Map.of("car", vehicle.getId());

// 5️⃣ In Personenattribute einfügen (neue Methode!)
        VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, modeToVehicle);

        // 6) Aus dem Guice-Injektor das RoutingModule für "pt" holen
        //    → Das ist der SwissRailRaptor, den wir gleich verwenden
        //RoutingModule swissRailRaptor = controller.getInjector().getInstance(
        //        Key.get(RoutingModule.class, Names.named("pt")));

        TravelTimeCalculator ttc = controller.getInjector().getInstance(Key.get(TravelTimeCalculator.class, Names.named("car")));

        Link link = scenario.getNetwork().getLinks().values().iterator().next();

        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(ttc);
        eventsManager.processEvent(new LinkEnterEvent(645, Id.createVehicleId("2"), Id.createLinkId("1")));
        eventsManager.processEvent(new LinkEnterEvent(745, Id.createVehicleId("1"), Id.createLinkId("1")));
        eventsManager.processEvent(new LinkLeaveEvent(999, Id.createVehicleId("2"), Id.createLinkId("1")));



        double t1 = ttc.getLinkTravelTimes().getLinkTravelTime(link, 1000, null, null);
        double t2 = ttc.getLinkTravelTimes().getLinkTravelTime(link, 745, null, null);
        double t3 = ttc.getLinkTravelTimes().getLinkTravelTime(link, 872.5, null, null);
        System.out.println("TravelTime now: " + t1 + "\n" + t2 + "\n" + t3);

        RoutingModule carRouter = controller.getInjector().getInstance(
               Key.get(RoutingModule.class, Names.named("car")));
//        RoutingModule walkRouter = controller.getInjector().getInstance(
//                Key.get(RoutingModule.class, Names.named("walk")));
//        RoutingModule bikeRouter = controller.getInjector().getInstance(
//                Key.get(RoutingModule.class, Names.named("bike")));




//        EventsManager events = EventsUtils.createEventsManager();
//        TravelTimeCalculator ttcTest = new TravelTimeCalculator.Builder(scenario.getNetwork())
//                .build();
//        events.addHandler(ttc);

//        TravelTime tt = controller.getInjector().getInstance(TravelTime.class);
        TravelTime carTT = controller.getInjector().getInstance(
                Key.get(TravelTime.class, Names.named("car"))
        );
        System.out.println("TravelTime impl: " + carTT.getClass().getName());

        // 7) Eine Routing-Anfrage (RoutingRequest) definieren:
        //    von Link "1" nach Link "5", Abfahrtszeit 7:00 Uhr, ohne Person/Attribute.
        RoutingRequest request = new RoutingRequest() {
            @Override
            public Facility getFromFacility() {
                // Startpunkt ist Link 1 im Netzwerk
                return new LinkWrapperFacility(scenario.getNetwork().getLinks().get(Id.createLinkId("1")));
            }

            @Override
            public Facility getToFacility() {
                // Zielpunkt ist Link 5 im Netzwerk
                return new LinkWrapperFacility(scenario.getNetwork().getLinks().get(Id.createLinkId("23")));
            }

            @Override
            public double getDepartureTime() {
                // Abfahrt um 7 Uhr morgens (7 * 3600 Sekunden)
                return 7 * 3600;
            }

            @Override
            public Person getPerson() {
                return person;
            }

            @Override
            public Attributes getAttributes() {
                // Keine zusätzlichen Attribute für die Anfrage
                return null;
            }
        };

        // 8) Routing durchführen: Raptor berechnet Route basierend auf Anfrage
//        List<? extends PlanElement> planElementsRaptor = swissRailRaptor.calcRoute(request);
        List<? extends PlanElement> planElementsCar = carRouter.calcRoute(request);
//        List<? extends PlanElement> planElementsBike = bikeRouter.calcRoute(request);
//        List<? extends PlanElement> planElementsWalk = walkRouter.calcRoute(request);


        // 9) Ergebnis ausgeben (jede Teilstrecke der Route wird als PlanElement auf die Konsole geschrieben)
//        System.out.println( " Isolated swissRailRouter result " );
//        planElementsRaptor.forEach(System.out::println);

//        System.out.println( " Isolated carRouter result " );
//        planElementsCar.forEach(System.out::println);
//        System.out.println(planElementsCar.getFirst());
        Leg leg = (Leg) planElementsCar.getFirst();
//        Route route = leg.getRoute();
//        System.out.println("leg: " + leg);
        System.out.println("Route " + leg.getRoute());
//        System.out.println("Distance: " + leg.getRoute().getDistance());
//        System.out.println("RouteDescription: " + leg.getRoute().getRouteDescription());
//        System.out.println("RouteType: " + leg.getRoute().getRouteType());
//        System.out.println("StartLinkId: " + leg.getRoute().getTravelTime());
//        System.out.println("EndLinkId: " + leg.getRoute().getEndLinkId());
//        System.out.println("StartLinkId: " + leg.getRoute().getStartLinkId());
//        System.out.println("Class: " + leg.getRoute().getClass());
//
//        System.out.println("RoutingMode: " + leg.getRoutingMode() + " DepartureTime: " + leg.getDepartureTime()
//                + " TravelTime: " + leg.getTravelTime() + " Mode: " + leg.getMode() + " Attrributes: " + leg.getAttributes());
//
//        System.out.println("leg String: " + leg.toString());

        List<PlanElement> result = new ArrayList<>();

//        //Leg
//        //Leg - Mode
//        String mode = leg.getMode();
//        //Leg - Times
//        double legDepartureTime = leg.getDepartureTime().seconds();
//        double legTravelTime = leg.getTravelTime().seconds();
//        double arrivalTime = legDepartureTime + legTravelTime;
//        //Leg - Route
//        String startLinkId = leg.getRoute().getStartLinkId().toString();
//        String endLinkId = leg.getRoute().getEndLinkId().toString();
//        double routeTravelTime = leg.getRoute().getTravelTime().seconds();
//        double distance = leg.getRoute().getDistance();
//        //double travelCost = leg.getRoute().getT
//        //String routeDescription = leg.getRoute().getRouteDescription();
//        String routeType = leg.getRoute().getRouteType();
//        //Leg - RoutingMode
//        String routingMode = leg.getRoutingMode();
//        //Attributes attr = leg.getAttributes();





//        System.out.println( " Isolated bikeRouter result " );
//        planElementsBike.forEach(System.out::println);
//
//        System.out.println( " Isolated walkRouter result " );
//        planElementsWalk.forEach(System.out::println);


        System.out.println( " \nDas war der Isolated Router :)! " );
    }

}
