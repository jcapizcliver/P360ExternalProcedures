package mx.com.liverpool.p360.services.core.restservices;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mx.com.liverpool.p360.services.core.GetAttributeValuesForo;
import mx.com.liverpool.p360.services.core.PropertiesManager;

/**
 * Servlet implementation class GetProposalsForo
 */
@WebServlet("/public/rt/GetProposalsForo")
public class GetProposalsForo extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetProposalsForo() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String baseUrl = PropertiesManager.get( "p360.contingency.base_url" );
		String encoded = PropertiesManager.get( "p360.contingency.basic_token_auth" );
		
		String baseDirectory = PropertiesManager.get("p360.contingency.base_directory");
		if(baseDirectory == null || "".equals(baseDirectory)) {
			baseDirectory = "/u01/stage/";
		}
		request.setCharacterEncoding("UTF-8");
		java.io.BufferedReader br = request.getReader();
		String line = null;
		StringBuilder sb = new StringBuilder();
		Object rawResponse = null;
		while((line = br.readLine()) != null) {
			sb.append(line);
		}
		try {
            rawResponse = mx.com.liverpool.p360.services.core.ForoRequestGate.execute(sb.toString(), () -> {
                try (GetAttributeValuesForo ga = new GetAttributeValuesForo()) {
                    return String.valueOf(ga.procesamelo(sb.toString(), baseUrl, encoded));
                }
            });
        } catch(org.json.JSONException e) {
			rawResponse = new org.json.JSONObject().put("Error", "Input was not a json object.");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            rawResponse = new org.json.JSONObject().put("Error", "Solicitud interrumpida.");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            rawResponse = new org.json.JSONObject().put("Error", "No se pudo completar la consulta de Foro.");
        }
		response.setHeader("Content-Type", "application/json");
		response.setHeader("Accept", "application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().println(rawResponse);
	}

}
