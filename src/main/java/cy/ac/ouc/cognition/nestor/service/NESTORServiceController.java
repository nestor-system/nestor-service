package cy.ac.ouc.cognition.nestor.service;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;

import cy.ac.ouc.cognition.nestor.lib.base.NESTORException;
import cy.ac.ouc.cognition.nestor.lib.pipeline.Pipeline;

@CrossOrigin
@RestController
@Tag(name="NESTOR Service", description="Knowledge-Based Natural languagE to Symbolic form TranslatOR")
public class NESTORServiceController {

	private boolean init;

	static enum CommandType {
		INIT, GETTITLE, GETSERVERIP, GETSERVERPORT, GETMISC, UNDEFINED;
	}
	
	private static String		ServiceTitle = "NESTOR - Knowledge-Based Natural languagE to Symbolic form TranslatOR";
	private static int			InstanceId = 0;
	private int					ObjectCounter = 0;
	private int					InfoCounter;


	public NESTORServiceController() {

		InstanceId++;
		ObjectCounter++;
		InfoCounter = 0;

	}


	@Autowired
	public void setInit(boolean initValue) {
		this.init = initValue;
	}


	private NESTORResponse initializeNESTOR(boolean forceNew) {
		try {

			new Pipeline(true).setAndLoadNLProcessor(forceNew);
			System.out.println("NLP Loaded!");

			return new NESTORResponse(0, "NESTOR Initialized", "NLP Loaded!");

		}
		
		catch (NESTORException ne) {
			System.out.println("NESTOR Service failed to set and load NLP!: " + ne.getMessage());
			ne.printStackTrace();			

			return new NESTORResponse(11211, "NESTOR Failed to Initialize", ne.getMessage());
		}
	
		catch (Exception | OutOfMemoryError nlpe) {
			System.out.println("NESTOR Service failed to initialize!");
			nlpe.printStackTrace();

			return new NESTORResponse(1121, "NESTOR Failed to Initialize", nlpe.getMessage());
		}

	}


	@PostConstruct
	public void postNESTORServiceController() {

		System.out.println("nestorpipeline.init=[" + Boolean.toString(init) + "]");

		if (init)
			initializeNESTOR(false);

	}


	@Operation(summary  = "NESTOR Service Index Page")
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "text/html")
	public String index(HttpServletRequest request) {

		return	"<H1>" + ServiceTitle + "</H1><H2>REST Service</H2>" +
				"[" + request.getLocalName() + "][" + request.getLocalPort()+ "][" + InstanceId + "][" + ObjectCounter + "][" + InfoCounter + "]";

	}


	@Operation(summary  =	"Control NESTOR Service. \n" +
							" 0: Initialize Service, \n" +
							" 1: Get NESTOR Service Title, \n" +
							" 2: Get NESTOR Service Server Name, \n" +
							" 3: Get NESTOR Service Port, \n" +
							" 4: Get Miscalleneous Information")
    @RequestMapping(value = "/control", method = RequestMethod.GET, produces = "application/json")
	public NESTORResponse controlService(HttpServletRequest request, @RequestParam("command") Integer command) {

		InfoCounter++;

		if (command == CommandType.INIT.ordinal()) {
			return initializeNESTOR(true);	
		}

		else if (command == CommandType.GETTITLE.ordinal()) {

			return new NESTORResponse(0, "NESTOR Service Title", ServiceTitle);
		}
		
		else if (command == CommandType.GETSERVERIP.ordinal()) {

			return new NESTORResponse(0, "NESTOR Service Server Name", request.getServerName());
		}
		
		else if (command == CommandType.GETSERVERPORT.ordinal()) {

			return new NESTORResponse(0, "NESTOR Service Port", Integer.toString(request.getLocalPort()));
		}
		
		else if (command == CommandType.GETMISC.ordinal()) {

			return new NESTORResponse(0, "NESTOR Service Misc Information",
										ServiceTitle + "," + request.getLocalName() + "," +
										Integer.toString(request.getLocalPort()) + "," + InstanceId + "," + ObjectCounter + "," + InfoCounter);
		}
		
		else {
			
			return new NESTORResponse(1221, "Invalid NESTOR Service Control Command", Integer.toString(command));
		}

	}


    @Operation(summary  = "Process natural language text")
    @RequestMapping(value = "/processnl", method = RequestMethod.POST, produces = "application/json")
	public NESTORResponse processNL(@RequestBody String nlText) {
		try {
			Pipeline NESTORPipe = new Pipeline();

			System.out.println("Will try to process natural language text: [" + nlText + "]");
			NESTORPipe.processNL(nlText);

			System.out.println("Natural language text processed!");
			return new NESTORResponse(0, "Natural Language Processing Data", NESTORPipe.getNLPData());

		} catch (Exception | OutOfMemoryError nlpe) {
			System.out.println("Failed to process natural language text!");
			System.out.println(nlpe.getMessage());
			nlpe.printStackTrace();

			return new NESTORResponse(1301, "Failed to process natural language text", nlpe.getMessage());
		}
	}


    @Operation(summary = "Generate logic predicates and return logic annotation of natural language text")
    @RequestMapping(value = "/generatelogicpredicates", method = RequestMethod.POST, produces = "application/json")
	public NESTORResponse generateLogicPredicates(@RequestBody String nlText) {
		try {
			Pipeline NESTORPipe = new Pipeline();

			System.out.println("Will try to generate logic predicates from natural language text: [" + nlText + "]");
			NESTORPipe.processNL(nlText);
			NESTORPipe.generateLogicPredicates();

			System.out.println("Logic predicates from natural Language text generated!");
			return new NESTORResponse(0, "Logic Annotation", NESTORPipe.getLogicAnnotationText(), NESTORPipe.getDocumentJSON());

		} catch (Exception nlpe) {
			System.out.println("Failed to generate logic predicates!");
			System.out.println(nlpe.getMessage());
			nlpe.printStackTrace();

			return new NESTORResponse(1401, "Failed to generate logic predicates", nlpe.getMessage());
		}
	}


    @Operation(summary = "Generate logic expressions and return logic-based representation of natural language text")
    @RequestMapping(value = "/generatelogicexpressions", method = RequestMethod.POST, produces = "application/json")
	public NESTORResponse generateLogicExpressions(@RequestBody NESTORRequest request) {
		String translationPolicy = "";
		try {
			Pipeline NESTORPipe = new Pipeline(request.getTranslationParametersJSON());

			String nlText = request.getNlText();
			translationPolicy = request.getTranslationPolicy();

			System.out.println("Will try to generate logic expressions from natural language text: [" + nlText + "] using Translation Policy: [" + translationPolicy + "]");
			NESTORPipe.processNL(nlText);
			NESTORPipe.generateLogicPredicates();
			NESTORPipe.generateLogicExpressions(translationPolicy);

			System.out.println("Logic expressions generated from natural language text!");
			String message = "Logic-based Representation";

			return new NESTORResponse(0, message, NESTORPipe.getLogicBasedRepresentationText(), NESTORPipe.getDocumentJSON());

		} catch (Exception | OutOfMemoryError nlpe) {
			System.out.println("Failed to generate logic expressions from natural language text!");
			System.out.println(nlpe.getMessage());
			nlpe.printStackTrace();

			String message = "Failed to generate logic expressions from natural language text";

			return new NESTORResponse(1601, message, nlpe.getMessage());
		}
	}

}



@Schema(title = "NESTORRequest", description = "NESTOR Service Request Object")
class NESTORRequest {

	private String	nlText = null;
	private String	translationPolicy = null;
	private String	translationParametersJSON = null;


	
	public NESTORRequest() {

		this.nlText = "";
		this.translationPolicy = "";
		this.translationParametersJSON = "";
									   
	}

	public NESTORRequest(String nlText) {

		this.nlText = nlText;
		this.translationPolicy = "";
		this.translationParametersJSON = "";
									   
	}

	public NESTORRequest(String nlText, String translationPolicy) {

		this.nlText = nlText;
		this.translationPolicy = translationPolicy;
		this.translationParametersJSON = "";
									   
	}


	public NESTORRequest(String nlText, String translationPolicy, String translationParametersJSON) {

		this.nlText = nlText;
		this.translationPolicy = translationPolicy;
		this.translationParametersJSON = translationParametersJSON;
									   
	}




	public NESTORRequest nlText(String nlText) {
		this.nlText = nlText;
		return this;
	}

    @Schema(title = "nlText", description = "NESTOR Service Request natural language text to be processed or translated")
	public String getNlText() {
		return this.nlText;
	}
	
	public void setNlText(String nlText) {
		this.nlText = nlText;
	}



	public NESTORRequest translationPolicy(String translationPolicy) {
		this.translationPolicy = translationPolicy;
		return this;
	}

    @Schema(title = "translationPolicy", description = "NESTOR Service Request translation policy to be used")
	public String getTranslationPolicy() {
		return this.translationPolicy;
	}
	
	public void setTranslationPolicy(String translationPolicy) {
		this.translationPolicy = translationPolicy;
	}



	public NESTORRequest translationParametersJSON(String translationParametersJSON) {
		this.translationParametersJSON = translationParametersJSON;
		return this;
	}

    @Schema(title = "translationParametersJSON", description = "Translation parameters JSON")
	public String getTranslationParametersJSON() {
		return this.translationParametersJSON;
	}
	
	public void setTranslationParametersJSON(String translationParametersJSON) {
		this.translationParametersJSON = translationParametersJSON;
	}



	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append("Translation Data {\n");    
		sb.append("    nlText: ").append(toIndentedString(nlText)).append("\n");
		sb.append("    translationPolicy: ").append(toIndentedString(translationPolicy)).append("\n");
		sb.append("    translationParametersJSON: ").append(toIndentedString(translationParametersJSON)).append("\n");
		sb.append("}");

		return sb.toString();
	}

	
	
	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {

		if (o == null) {
			return "null";
		}

		return o.toString().replace("\n", "\n    ");

	}

}



@Schema(title = "NESTORResponse", description = "NESTOR Service Response Object")
class NESTORResponse {

	private int		ResultCode = -1;
	private String	Message = null;
	private String	TextData = null;
	private String	JSONData = null;

	
	
	public NESTORResponse(int ResultCode, String Message, String TextData, String JSONData) {

		this.ResultCode = ResultCode;
		this.Message = Message;
		this.TextData = TextData;
		this.JSONData = JSONData;
	}


	
	public NESTORResponse(int ResultCode, String Message, String TextData) {

		this.ResultCode = ResultCode;
		this.Message = Message;
		this.TextData = TextData;
		this.JSONData = "";
	}


	
	public NESTORResponse ResultCode(int ResultCode) {
		this.ResultCode = ResultCode;
		return this;
	}

	@Schema(title="ResultCode", required = true, description = "NESTOR Service Response internal result code")
	@NotNull
	public int getResultCode() {
		return ResultCode;
	}

	public void setResultCode(int ResultCode) {
		this.ResultCode = ResultCode;
	}
	


	public NESTORResponse Message(String Message) {
		this.Message = Message;
		return this;
	}

	@Schema(title="Message", description = "NESTOR Service Response internal message")
	public String getMessage() {
		return Message;
	}

	public void setMessage(String Message) {
		this.Message = Message;
	}



	public NESTORResponse TextData(String TextData) {
		this.TextData = TextData;
		return this;
	}

	@Schema(title="TextData", description = "NESTOR Service Response text data")
	public String getTextData() {
		return this.TextData;
	}

	public void setTextData(String TextData) {
		this.TextData = TextData;
	}



	public NESTORResponse JSONData(String JSONData) {
		this.JSONData = JSONData;
		return this;
	}

	@Schema(title="JSONData", description = "NESTOR Service Response JSON data")
	public String getJSONData() {
		return this.JSONData;
	}

	public void setJSONData(String JSONData) {
		this.JSONData = JSONData;
	}



	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append("Return Message {\n");    
		sb.append("    ResultCode: ").append(toIndentedString(Integer.valueOf(ResultCode))).append("\n");
		sb.append("    Message: ").append(toIndentedString(Message)).append("\n");
		sb.append("    JSONData: ").append(toIndentedString(JSONData)).append("\n");
		sb.append("    TextData: ").append(toIndentedString(TextData)).append("\n");
		sb.append("}");

		return sb.toString();
	}
	
	
	/**
	 * Convert the given object to string with each line indented by 4 spaces
	 * (except the first line).
	 */
	private String toIndentedString(java.lang.Object o) {

		if (o == null) {
			return "null";
		}

		return o.toString().replace("\n", "\n    ");

	}
}