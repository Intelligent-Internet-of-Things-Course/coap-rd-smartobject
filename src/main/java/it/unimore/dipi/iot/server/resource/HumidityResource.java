package it.unimore.dipi.iot.server.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.utils.CoreInterfaces;
import it.unimore.dipi.iot.utils.SenMLPack;
import it.unimore.dipi.iot.utils.SenMLRecord;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.Type;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Simple Humidity Observable resource represented as int value
 *
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project coap-demo-smartobject
 * @created 20/10/2020 - 21:54
 */
public class HumidityResource extends CoapResource {

	private final static Logger logger = LoggerFactory.getLogger(HumidityResource.class);

	private static final Number SENSOR_VERSION = 0.1;

	private static final String OBJECT_TITLE = "HumiditySensor";

	private static final String RESOURCE_TYPE = "com.iot.demo.sensor.humidity";

	private static final long SENSOR_UPDATE_TIME_MS = 1000;

	private static final int RESOURCE_MAX_AGE_SECONDS = 1;

	private int humidity;

	private String deviceId;

	//Resource Unit according to SenML Units Registry (http://www.iana.org/assignments/senml/senml.xhtml)
	private String HUMIDITY_UNIT = "%RH";

	private ObjectMapper objectMapper;

	public HumidityResource(String deviceId, String name) {

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
		getAttributes().addAttribute("rt",RESOURCE_TYPE);
		getAttributes().addAttribute("if", CoreInterfaces.CORE_S.getValue());

		// schedule a periodic update task, otherwise let events call changed()
		Timer timer = new Timer();
		timer.schedule(new UpdateTask(), 0, SENSOR_UPDATE_TIME_MS);
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
			senMLRecord.setUnit(HUMIDITY_UNIT);
			senMLRecord.setValue(humidity);
			senMLRecord.setTime(System.currentTimeMillis());

			senMLPack.add(senMLRecord);

			return Optional.of(this.objectMapper.writeValueAsString(senMLPack));

		}catch (Exception e){
			logger.error("Error Generating SenML Record ! Msg: {}", e.getLocalizedMessage());
			return Optional.empty();
		}
	}

	private class UpdateTask extends TimerTask {

		@Override
		public void run() {
			// .. periodic update of the resource
			Random rand = new Random();
			humidity = (rand.nextInt(100) + 1);
			changed(); // notify all observers 
		}
	}

	@Override
	public void handleGET(CoapExchange exchange) {
		// the Max-Age value should match the update interval
		exchange.setMaxAge(RESOURCE_MAX_AGE_SECONDS);

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
			exchange.respond(CoAP.ResponseCode.CONTENT, String.valueOf(humidity), MediaTypeRegistry.TEXT_PLAIN);

	}

}
