/**
* BigBlueButton open source conferencing system - http://www.bigbluebutton.org/
* 
* Copyright (c) 2012 BigBlueButton Inc. and by respective authors (see below).
*
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License as published by the Free Software
* Foundation; either version 3.0 of the License, or (at your option) any later
* version.
* 
* BigBlueButton is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
* PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License along
* with BigBlueButton; if not, see <http://www.gnu.org/licenses/>.
*
*/
package org.bigbluebutton.conference.service.videodraw;

import org.bigbluebutton.conference.BigBlueButtonSession;
import org.bigbluebutton.conference.Constants;
import org.bigbluebutton.conference.service.videodraw.shapes.Annotation;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.Red5;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Map;

public class VideoDrawService {

	private static Logger log = Red5LoggerFactory.getLogger(VideoDrawService.class, "bigbluebutton");
	
	private VideoDrawApplication application;

	public void setVideoDrawApplication(VideoDrawApplication a){
		log.debug("Setting whiteboard application instance");
		this.application = a;
	}
	
	public void sendAnnotation(Map<String, Object> annotation) {
//		for (Map.Entry<String, Object> entry : annotation.entrySet()) {
//		    String key = entry.getKey();
//		    Object value = entry.getValue();
		    
//		    if (key.equals("points")) {
//		    	String points = "points=[";
//		    	ArrayList<Double> v = (ArrayList<Double>) value;
//		    	log.debug(points + pointsToString(v) + "]");
//		    } else {
//		    	log.debug(key + "=[" + value + "]");
//		    }
//		}
		
		Annotation a = new Annotation(annotation);
		
		application.sendAnnotation(a);
	}
	
	private String pointsToString(ArrayList<Double> points){
    	String datapoints = "";
    	for (Double i : points) {
    		datapoints += i + ",";
    	}
    	// Trim the trailing comma
//    	log.debug("Data Point = " + datapoints);
    	return datapoints.substring(0, datapoints.length() - 1);

//		application.sendShape(shape, type, color, thickness, fill, fillColor, transparency, id, status);

	}
	
	public void setActivePage(Map<String, Object> message){		
		log.info("VideoDrawApplication - Getting number of shapes for page: " + (Integer) message.get("pageNum"));
		application.changePage((Integer) message.get("pageNum"));
	}
	
	public void requestAnnotationHistory(Map<String, Object> message) {
		log.info("VideoDrawApplication - requestAnnotationHistory");
		application.sendAnnotationHistory(getBbbSession().getInternalUserID(), 
				(String) message.get("presentationID"), (Integer) message.get("pageNumber"));
	}
		
	public void clear(Map<String, Object> annotation) {
		log.info("VideoDrawApplication - Clearing board");
        Annotation a = new Annotation(annotation);
		application.clear(a);
	}
	
	public void undo(Map<String, Object> annotation) {
		log.info("VideoDrawApplication - Deleting last graphic");
        Annotation a = new Annotation(annotation);
		application.undo(a);
	}
	
	public void toggleGrid() {
		log.info("VideoDrawApplication - Toggling grid mode");
		application.toggleGrid();
	}
	
	public void setActivePresentation(Map<String, Object> message) {		
		log.info("VideoDrawApplication - Setting active presentation: " + (String)message.get("presentationID"));
		application.setActivePresentation((String)message.get("presentationID"), (Integer) message.get("numberOfSlides"));
	}
	
	public void enableWhiteboard(Map<String, Object> message) {
		log.info("VideoDrawApplication - Setting whiteboard enabled: " + (Boolean)message.get("enabled"));
		application.enableWhiteboard((Boolean)message.get("enabled"));
	}
	
	public void isWhiteboardEnabled() {
		application.isWhiteboardEnabled(getBbbSession().getInternalUserID());
	}
	
	private BigBlueButtonSession getBbbSession() {
		return (BigBlueButtonSession) Red5.getConnectionLocal().getAttribute(Constants.SESSION);
	}
	
}
