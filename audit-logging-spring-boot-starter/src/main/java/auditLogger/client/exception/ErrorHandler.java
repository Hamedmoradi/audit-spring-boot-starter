package auditLogger.client.exception;

import auditLogger.client.infra.http.MediaType;
import auditLogger.client.serialization.SerializationHelper;
import auditLogger.model.RequestInfo;
import auditLogger.model.ResponseInfo;
import auditLogger.util.MessageBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import auditLogger.client.kafka.AuditLogProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import auditLogger.client.config.AuditConfiguration;
import auditLogger.client.config.ConfigurationFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * hamedMoradi.mailsbox@gmail.com
 */

@Slf4j(topic = "[AUDIT]")
@RequiredArgsConstructor
public class ErrorHandler extends HttpServlet {

    private String auditResponseServiceURL;
    private final AuditLogProducer auditLogProducerForRequest;
    private AuditConfiguration configuration = ConfigurationFactory.auditConfigurationAsync();
    private  static final String TOPIC = "audit_logger";

    @Override
    public void init() throws ServletException {
        super.init();
        String baseUri = configuration.getServiceUrl();
        auditResponseServiceURL = baseUri + "/response";
    }

    /**
     *  Overridden to process error for all http methods
     **/
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processError(req, resp);
    }

    private void processError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Throwable throwable = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String servletName = (String) request.getAttribute("javax.servlet.error.servlet_name");
        if (servletName.equals(this.getClass().getName())) {
            return;
        }
        if (response.isCommitted()) {
            log.error("handleError: Response already committed; cannot send error " + statusCode + throwable.getMessage(), throwable);
            auditError(request, throwable);
        } else {
            if (servletName == null) {
                servletName = "Unknown";
            }
            String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
            if (requestUri == null)
                requestUri = "Unknown";

            auditError(request, throwable);

            Map exRespMap = new HashMap();
            if (throwable != null) {
                exRespMap.put("servletName", servletName);
                String exceptionType = throwable.getCause() != null ? throwable.getCause().getClass().getName() : throwable.getClass().getName();
                exRespMap.put("exceptionType", exceptionType);
                exRespMap.put("exceptionMessage", throwable.getMessage());
                exRespMap.put("requestUri", requestUri);
                if (statusCode != null) {
                    exRespMap.put("statusCode", statusCode);
                }
            } else {
                exRespMap.put("message", "Error information is missing");
            }

            response.reset();
            response.setContentType(MediaType.APPLICATION_JSON);
            response.setStatus(statusCode);

            PrintWriter respStream = response.getWriter();

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectMapper mapper = SerializationHelper.getMapper();
            mapper.writeValue(stream, exRespMap);
            String exRespMsg = stream.toString();
            log.error(exRespMsg, throwable);

            mapper.writeValue(respStream, exRespMap);
            respStream.write(exRespMsg);
            response.flushBuffer();
            respStream.close();
        }
    }

    private void auditError(HttpServletRequest request, Throwable throwable) throws IOException {
        Principal principal = request.getUserPrincipal();
        ResponseInfo responseInfo = new ResponseInfo();
        if (principal != null) responseInfo.username = principal.getName();
        responseInfo.Id = request.getHeader("traceId");
        responseInfo.requestId = (request.getAttribute(RequestInfo.REQUEST_ID) == null) ? null : request.getAttribute(RequestInfo.REQUEST_ID).toString();
        responseInfo.payload = SerializationHelper.serialize(throwable);
        responseInfo.date = Calendar.getInstance().getTime();

        responseInfo.status = (Integer) request.getAttribute("javax.servlet.error.status_code");
        sendAudit(responseInfo,request);
    }

    private void sendAudit(ResponseInfo responseInfo,HttpServletRequest request) throws IOException {
        String json= MessageBuilder.getResponseMessage(responseInfo,request);
        log.info("implement kafka send message");
        auditLogProducerForRequest.sendMessage(json,TOPIC);

    }
}