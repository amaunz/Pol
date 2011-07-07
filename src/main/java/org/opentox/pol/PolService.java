package org.opentox.pol;

/**
 * 
 * JAX-RS service that provide CRUD ops on Pol resources
 *
 * @author Gabriel Mateescu gabriel@vt.edu
 */

// Annotations 

// Service designator
import javax.ws.rs.Path;

import java.io.IOException;
import java.io.InputStream;

// Map HTTP methods to methods in this class
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.WebApplicationException;

// Content handlers 
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

// Parameter injection
import javax.ws.rs.HeaderParam;


//
// JAX-RS service with relative root URI /opensso-pol
//
@Path("/opensso-pol")
public interface PolService {

	/**
	 * Create a Pol resource. A client issues a  
	 * 
	 *     POST /pols   
	 *     Content-Type: application/xml
	 *
	 * request to send XML representation 
	 * of the Pol resource to be created. 
	 * 
	 * @param is    XML representation of the new pol
	 * @return      the response to be sent to the client
	 * @throws WebApplicationException 
	 * @throws IOException 
	 */
	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public Response createPol(@HeaderParam("subjectid") String subjectId, @Context UriInfo uriInfo, InputStream s) throws IOException, WebApplicationException;
	

	/**
	 * Read a Pol resource. A client issues a 
	 *
	 *     GET /pols/
	 *     Content-Type: application/xml
	 *  
	 * request to get the XML representation 
	 * of the Pol resource with this ID.
	 *  
	 * @param   id 
	 * @return  response to be sent to the client
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	public Response getPolID(@HeaderParam("subjectid") String subjectId, @HeaderParam("id") String id, @HeaderParam("uri") String uri, @HeaderParam("polnames") String polnames);
	
	
	/**
	 * Read the Pol resources. A client issues a 
	 *
	 *     GET /pols/
	 *     Content-Type: application/xml
	 *  
	 * no uri: request the names of all policies for a given token
	 * uri: request the owner of uri
	 * uri + pols: request also the names of policies associated with uri
	 * 
	 * @return  response to be sent to the client
	 *
	 * @GET
	 * @Produces(MediaType.APPLICATION_XML)
	 * public Response getPol(@HeaderParam("subjectid") String subjectId, @HeaderParam("uri") String uri, @HeaderParam("polnames") String polnames);
	 */
		
	
	/**
	 * Delete a Book resource. A client issues a 
	 *
	 *     DELETE /pols/{id}
	 *     Content-Type: application/xml
	 *  
	 * request to delete the Pol resource with 
	 * the id matching the URI pattern /pols/{id}.
	 *  
	 * @param   id 
	 */
	@DELETE
	public Response deletePol(@HeaderParam("subjectid") String subjectId, @HeaderParam("id") String id);

	
}
