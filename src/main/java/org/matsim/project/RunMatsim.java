/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.project;

import com.google.inject.name.Names;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.SingleHandlerEventsManager;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.scenario.ScenarioUtils;

import java.beans.EventHandler;
import java.io.IOException;

/**
 * @author nagel
 *
 */
public class RunMatsim{

    public static final String outputDirectory = "output/gRPCEventTest" ;

	public static void main(String[] args) {

		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "scenarios/equil/config1.xml" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

        config.controller().setOutputDirectory(outputDirectory);
		config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
        //config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
//        config.global().setNumberOfThreads(1);
//        config.qsim().setNumberOfThreads(1);

        // possibly modify config here
		 config.controller().setLastIteration(2);

		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		// possibly modify scenario here
		
		// ---
		
		Controler controler = new Controler( scenario ) ;

		// possibly modify controler here

        IsolatedRouterClient grpcRouterClient = new IsolatedRouterClient("localhost", 50051);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addRoutingModuleBinding("car").to(GrpcRoutingModule.class);
            }
        });

        // add the events handlers
        controler.addOverridingModule(new AbstractModule(){
            @Override public void install() {
                bind(IsolatedRouterClient.class).toInstance(grpcRouterClient);
				this.addEventHandlerBinding().toInstance( new GRPCEventHandler() );
            }
        });

//		controler.addOverridingModule( new OTFVisLiveModule() ) ;

//		controler.addOverridingModule( new SimWrapperModule() );
		
		// ---
//		System.out.println("Der Controller läuft an...");
		controler.run();
//        System.out.println("Main ist zu Ende...");
        try {
            grpcRouterClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	
}
