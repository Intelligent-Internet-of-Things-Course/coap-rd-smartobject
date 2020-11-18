package it.unimore.dipi.iot.server.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.utils.CoreInterfaces;
import it.unimore.dipi.iot.utils.SenMLPack;
import it.unimore.dipi.iot.utils.SenMLRecord;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Simple Switch Demo Actuator represented as an int value
 * 0 = disabled, 1=enabled
 *
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-demo-smartobject
 * @created 20/10/2020 - 21:54
 */
public class SwitchActuatorResource extends CoapResource {

	private final static Logger logger = LoggerFactory.getLogger(SwitchActuatorResource.class);

	private static final Number SENSOR_VERSION = 0.1;

	private static final String OBJECT_TITLE = "SwitchActuator";

	private static final String RESOURCE_TYPE = "com.iot.demo.actuator.switch";

	private int switchStatus;

	private String deviceId;

	private ObjectMapper objectMapper;

	public SwitchActuatorResource(String deviceId, String name) {

		super(name);

		this.deviceId = deviceId;

		//Jackson Object Mapper + Ignore Null Fields in order to properly generate the SenML Payload
		this.objectMapper = new ObjectMapper();
		this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		setObservable(true); // enable observing
		setObserveType(Type.CON); // configure the notification type to CONs

		getAttributes().setTitle(OBJECT_TITLE);
		getAttributes().setObservable(); // mark observable in the Link-Format

		//Specify Resource Attributes
		getAttributes().addAttribute("rt", RESOURCE_TYPE);
		getAttributes().addAttribute("if", CoreInterfaces.CORE_A.getValue());

		// Reset Switch Status Value
		this.switchStatus = 0;
	}

	/**
	 * Create the SenML Response with the updated value and the resource information
	 * @return
	 */
	private Optional<String> getJsonSenmlResponse(){

		try{

			SenMLPack senMLPack = new SenMLPack();

			SenMLRecord senMLRecord = new SenMLRecord();
			senMLRecord.setBaseName(String.format("%s:%s", this.deviceId, this.getName()));
			senMLRecord.setVersion(SENSOR_VERSION);
			senMLRecord.setBooleanValue(getBooleanSwitchStatus());
			senMLRecord.setTime(System.currentTimeMillis());

			senMLPack.add(senMLRecord);

			return Optional.of(this.objectMapper.writeValueAsString(senMLPack));

		}catch (Exception e){
			logger.error("Error Generating SenML Record ! Msg: {}", e.getLocalizedMessage());
			return Optional.empty();
		}
	}

	private boolean getBooleanSwitchStatus(){
		return this.switchStatus != 0;
	}

	@Override
	public void handleGET(CoapExchange exchange) {

		//If the request specify the MediaType as JSON or JSON+SenML
		if (exchange.getRequestOptions().getAccept() == MediaTypeRegistry.APPLICATION_SENML_JSON ||
				exchange.getRequestOptions().getAccept() == MediaTypeRegistry.APPLICATION_JSON){

			Optional<String> senmlPayload = getJsonSenmlResponse();

			if(senmlPayload.isPresent())
				exchange.respond(CoAP.ResponseCode.CONTENT, senmlPayload.get(), exchange.getRequestOptions().getAccept());
			else
				exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
		}
		//Otherwise respond with the default textplain payload
		else
			exchange.respond(CoAP.ResponseCode.CONTENT, String.valueOf(this.switchStatus), MediaTypeRegistry.TEXT_PLAIN);

	}

	@Override
	public void handlePUT(CoapExchange exchange) {

		try{

			logger.info("Request Pretty Print:\n{}", Utils.prettyPrint(exchange.advanced().getRequest()));
			logger.info("Received PUT Request with body: {}", exchange.getRequestPayload());

			//If the request body is available
			if(exchange.getRequestPayload() != null){

				int submittedValue = Integer.parseInt(new String(exchange.getRequestPayload()));

				//If the value is not correct
				if(submittedValue == 0 || submittedValue == 1){

					//Update internal status
					this.switchStatus = submittedValue;

					logger.info("Resource Status Updated: {}", this.switchStatus);

					exchange.respond(CoAP.ResponseCode.CHANGED);

					//Notify Observers
					changed();
				}
				else
					exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
			}
			else
				exchange.respond(CoAP.ResponseCode.BAD_REQUEST);

		}catch (Exception e){
			logger.error("Error Handling POST -> {}", e.getLocalizedMessage());
			exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
		}

	}

	@Override
	public void handlePOST(CoapExchange exchange) {

		//According to CoRE Interface a POST request has an empty body and change the current status
		try{

			logger.info("Request Pretty Print:\n{}", Utils.prettyPrint(exchange.advanced().getRequest()));
			logger.info("Received POST Request with body: {}", exchange.getRequestPayload());

			//Empty request
			if(exchange.getRequestPayload() == null){

				//Update internal status
				this.switchStatus = (switchStatus == 1) ? 0 : 1;

				logger.info("Resource Status Updated: {}", this.switchStatus);

				exchange.respond(CoAP.ResponseCode.CHANGED);

				//Notify Observers
				changed();
			}
			else
				exchange.respond(CoAP.ResponseCode.BAD_REQUEST);

		}catch (Exception e){
			logger.error("Error Handling POST -> {}", e.getLocalizedMessage());
			exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
		}

	}
}
