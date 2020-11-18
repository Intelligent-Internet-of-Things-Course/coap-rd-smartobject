package it.unimore.dipi.iot.server;

import it.unimore.dipi.iot.server.resource.HumidityResource;
import it.unimore.dipi.iot.server.resource.SwitchActuatorResource;
import it.unimore.dipi.iot.server.resource.TemperatureResource;
import org.eclipse.californium.core.*;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.LinkFormat;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 *
 * Demo Temperature CoAP Smart Object hosting 3 different resources:
 *
 * - observable temperature sensor resource with a random int value (updated every 1 sec)
 * - observable humidity sensor resource with a random int value (updated every 1 sec)
 * - observable actuator represented as an int (0 disabled, 1 enabled)
 *
 * The Smart Object register itself and its resources to a target Resource Directory
 *
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-rd-smartobject
 * @created 20/10/2020 - 21:54
 */
public class DemoCoapSmartObjectProcess extends CoapServer {

	private final static Logger logger = LoggerFactory.getLogger(DemoCoapSmartObjectProcess.class);

	//private static final String RD_COAP_ENDPOINT_BASE_URL = "coap://127.0.0.1:5683/rd";

	private static final String RD_COAP_ENDPOINT_BASE_URL = "coap://192.168.1.102:5683/rd";

	private static final String TARGET_LISTENING_IP_ADDRESS = "192.168.1.17";

	private static final int TARGET_COAP_PORT = 5783;

	public DemoCoapSmartObjectProcess(){

		super();

		// explicitly bind to each address to avoid the wildcard address reply problem
		// (default interface address instead of original destination)
		for (InetAddress addr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
			if (!addr.isLinkLocalAddress()) {
				CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
				builder.setInetSocketAddress(new InetSocketAddress(addr, TARGET_COAP_PORT));
				this.addEndpoint(builder.build());
			}
		}

		String deviceId = String.format("dipi:iot:%s", UUID.randomUUID().toString());

		//Create Demo Resources
		TemperatureResource temperatureResource = new TemperatureResource(deviceId,"temperature");
		HumidityResource humidityResource = new HumidityResource(deviceId,"humidity");
		SwitchActuatorResource switchActuatorResource = new SwitchActuatorResource(deviceId,"switch");

		logger.info("Defining and adding resources ...");

		//Add resources ....
		this.add(temperatureResource);
		this.add(humidityResource);
		this.add(switchActuatorResource);

	}

	private static void registerToCoapResourceDirectory(Resource rootResource, String endPointName, String sourceIpAddress, int sourcePort){

		try{

			//coap://192.168.1.102:5683/rd?ep=myEndpointName&base=coap://192.168.1.17:5783
			String finalRdUrl = String.format("%s?ep=%s&base=coap://%s:%d", RD_COAP_ENDPOINT_BASE_URL, endPointName, sourceIpAddress, sourcePort);

			logger.info("Registering to Resource Directory: {}", finalRdUrl);

			//Initialize coapClient
			CoapClient coapClient = new CoapClient(finalRdUrl);

			//Request Class is a generic CoAP message: in this case we want a GET.
			//"Message ID", "Token" and other header's fields can be set
			Request request = new Request(CoAP.Code.POST);

			//If the POST request has a payload it can be set with the following command
			request.setPayload(LinkFormat.serializeTree(rootResource));

			//Set Request as Confirmable
			request.setConfirmable(true);

			logger.info("Request Pretty Print:\n{}", Utils.prettyPrint(request));

			//Synchronously send the POST request (blocking call)
			CoapResponse coapResp = null;

			try {

				coapResp = coapClient.advanced(request);

				//Pretty print for the received response
				logger.info("Response Pretty Print: \n{}", Utils.prettyPrint(coapResp));

				//The "CoapResponse" message contains the response.
				String text = coapResp.getResponseText();
				logger.info("Payload: {}", text);
				logger.info("Message ID: " + coapResp.advanced().getMID());
				logger.info("Token: " + coapResp.advanced().getTokenString());

			} catch (ConnectorException | IOException e) {
				e.printStackTrace();
			}

		}catch (Exception e){
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		DemoCoapSmartObjectProcess demoCoapServerProcess = new DemoCoapSmartObjectProcess();

		logger.info("Starting Coap Server...");

		demoCoapServerProcess.start();

		logger.info("Coap Server Started ! Available resources: ");

		demoCoapServerProcess.getRoot().getChildren().stream().forEach(resource -> {
			logger.info("Resource {} -> URI: {} (Observable: {})", resource.getName(), resource.getURI(), resource.isObservable());
		});

		registerToCoapResourceDirectory(demoCoapServerProcess.getRoot(),
				"testCoapEndpoint",
				TARGET_LISTENING_IP_ADDRESS,
				TARGET_COAP_PORT
				);
	}
}
