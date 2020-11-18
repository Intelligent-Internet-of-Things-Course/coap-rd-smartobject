package it.unimore.dipi.iot.client;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * A  CoAP Synchronous Client implemented using Californium Java Library
 * The client send a GET request to rd-lookup/res endpoint of a target Resource Directory
 *
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-rd-smartobject
 * @created 18/11/2020 - 09:19
 */
public class RdLookupCoapGetClientProcess {

	private final static Logger logger = LoggerFactory.getLogger(RdLookupCoapGetClientProcess.class);

	//private static final String COAP_ENDPOINT = "coap://127.0.0.1:5683/rd-lookup/res";

	private static final String COAP_ENDPOINT = "coap://192.168.1.102:5683/rd-lookup/res";

	public static void main(String[] args) {
		
		//Initialize coapClient
		CoapClient coapClient = new CoapClient(COAP_ENDPOINT);

		//Request Class is a generic CoAP message: in this case we want a GET.
		//"Message ID", "Token" and other header's fields can be set 
		Request request = new Request(Code.GET);

		//In required it is also possible to set a specific MID
		//request.setMID(8888);

		//In required it is also possible to set a specific Token
		//byte[] token = "a".getBytes();
		//request.setToken(token);

		//Set Request as Confirmable
		request.setConfirmable(true);

		logger.info("Request Pretty Print: \n{}", Utils.prettyPrint(request));

		//Synchronously send the GET message (blocking call)
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
	}
}