package org.matsim.project;

import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;

public class GRPCEventHandler implements LinkEnterEventHandler,
        LinkLeaveEventHandler, PersonArrivalEventHandler,
        PersonDepartureEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler
{

/**
 * This event handler prints some event information to the console.
 * @author dgrether
 *
 */

    @Override
    public void reset(int iteration) {
        System.out.println("reset...");
    }


    @Override
    public void handleEvent(LinkEnterEvent event) {
        String type = event.getEventType();
        double time = event.getTime();
        String linkId = event.getLinkId().toString();
        String vehicleId = event.getVehicleId().toString();
        String Response = IsolatedRouterClientWithEvents.sentEnterLeaveEvent(type, time, linkId, vehicleId);
//        System.out.println("Response: " + Response);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        String type = event.getEventType();
        double time = event.getTime();
        String linkId = event.getLinkId().toString();
        String vehicleId = event.getVehicleId().toString();
        String Response = IsolatedRouterClientWithEvents.sentEnterLeaveEvent(type, time, linkId, vehicleId);
//        System.out.println("Response: " + Response);
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        String type = event.getEventType();
        double time = event.getTime();
        String linkId = event.getLinkId().toString();
        String agentId = event.getPersonId().toString();
        String legMode = event.getLegMode();
        String Response = IsolatedRouterClientWithEvents.sentPersonEvent(type, time, linkId, agentId, legMode, null);
//        System.out.println("Response: " + Response);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        String type = event.getEventType();
        double time = event.getTime();
        String linkId = event.getLinkId().toString();
        String agentId = event.getPersonId().toString();
        String legMode = event.getLegMode();
        String routingMode = event.getRoutingMode();
        String Response = IsolatedRouterClientWithEvents.sentPersonEvent(type, time, linkId, agentId, legMode, routingMode);
//        System.out.println("Response: " + Response);
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        String type = event.getEventType();
        double time = event.getTime();
        String linkId = event.getLinkId().toString();
        String vehicleId = event.getVehicleId().toString();
        String driverId = event.getPersonId().toString();
        double relativePositionOnLink = event.getRelativePositionOnLink();
        String networkMode = event.getNetworkMode();
        String Response = IsolatedRouterClientWithEvents.sentVehicleEvent(type, time, linkId, vehicleId, driverId, networkMode, relativePositionOnLink);
//        System.out.println("Response: " + Response);
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        String type = event.getEventType();
        double time = event.getTime();
        String linkId = event.getLinkId().toString();
        String vehicleId = event.getVehicleId().toString();
        String driverId = event.getPersonId().toString();
        double relativePositionOnLink = event.getRelativePositionOnLink();
        String networkMode = event.getNetworkMode();
        String Response = IsolatedRouterClientWithEvents.sentVehicleEvent(type, time, linkId, vehicleId, driverId, networkMode, relativePositionOnLink);
//        System.out.println("Response: " + Response);
    }

}

