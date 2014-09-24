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
import org.bigbluebutton.conference.ClientMessage;
import org.bigbluebutton.conference.ConnectionInvokerService;
import org.bigbluebutton.conference.Constants;
import org.bigbluebutton.conference.service.recorder.RecorderApplication;
import org.bigbluebutton.conference.service.recorder.whiteboard.WhiteboardEventRecorder;
import org.bigbluebutton.conference.service.videodraw.shapes.Annotation;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.adapter.IApplication;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhiteboardApplication extends ApplicationAdapter implements IApplication {	
	private static Logger log = Red5LoggerFactory.getLogger(WhiteboardApplication.class, "bigbluebutton");
	
	private WhiteboardRoomManager roomManager;
	private RecorderApplication recorderApplication;
	private ConnectionInvokerService connInvokerService;
	private static final String APP = "VIDEODRAW";
	
	@Override
	public boolean appConnect(IConnection conn, Object[] params) {
		log.debug("***** " + APP + " [ " + " appConnect *********");
		return true;
	}

	@Override
	public void appDisconnect(IConnection conn) {
		log.debug("***** " + APP + " [ " + " appDisconnect *********");
	}

	@Override
	public boolean appJoin(IClient client, IScope scope) {
		log.debug("***** " + APP + " [ " + " appJoin [ " + scope.getName() + "] *********");
		return true;
	}

	@Override
	public void appLeave(IClient client, IScope scope) {
		log.debug("***** " + APP + " [ " + " appLeave [ " + scope.getName() + "] *********");
	}

	@Override
	public boolean appStart(IScope scope) {
		log.debug("***** " + APP + " [ " + " appStart [ " + scope.getName() + "] *********");
		return true;
	}

	@Override
	public void appStop(IScope scope) {
		log.debug("***** " + APP + " [ " + " appStop [ " + scope.getName() + "] *********");
		roomManager.removeRoom(getMeetingId());
	}
	
	@Override
	public boolean roomConnect(IConnection connection, Object[] params) {
		log.debug(APP+" - getting record parameters");
		if (getBbbSession().getRecord()){
			log.debug(APP+"WHITEBOARD - recording : true");
			WhiteboardEventRecorder recorder = new WhiteboardEventRecorder(getMeetingId(), recorderApplication);
			roomManager.getRoom(getMeetingId()).addRoomListener((IWhiteboardRoomListener) recorder);
			log.debug("event session is " + getMeetingId());
		}
    	return true;
	}

	@Override
	public void roomDisconnect(IConnection connection) {
		
	}

	@Override
	public boolean roomJoin(IClient client, IScope scope) {
		return true;
	}

	@Override
	public void roomLeave(IClient client, IScope scope) {
	}

	@Override
	public boolean roomStart(IScope scope) {
		roomManager.addRoom(scope.getName());
    	return true;
	}

	@Override
	public void roomStop(IScope scope) {
		roomManager.removeRoom(scope.getName());
	}
	
	public void setActivePresentation(String presentationID, int numPages) {
		WhiteboardRoom room = roomManager.getRoom(getMeetingId());
		if (room.presentationExists(presentationID)) {
			room.setActivePresentation(presentationID);
		} else {
			room.addPresentation(presentationID, numPages);
		}
		
		Map<String, Object> message = new HashMap<String, Object>();
		message.put("presentationID", presentationID);
		message.put("numberOfPages", numPages);
		ClientMessage m = new ClientMessage(ClientMessage.BROADCAST, getMeetingId(), "WhiteboardChangePresentationCommand", message);
		connInvokerService.sendMessage(m);
	}
	
	public void enableWhiteboard(boolean enabled) {
		roomManager.getRoom(getMeetingId()).setWhiteboardEnabled(enabled);
		
		Map<String, Object> message = new HashMap<String, Object>();
		message.put("enabled", roomManager.getRoom(getMeetingId()).isWhiteboardEnabled());
		ClientMessage m = new ClientMessage(ClientMessage.BROADCAST, getMeetingId(), "WhiteboardEnableWhiteboardCommand", message);
		connInvokerService.sendMessage(m);
	}
	
	public void isWhiteboardEnabled(String userid) {
		Map<String, Object> message = new HashMap<String, Object>();
		message.put("enabled", roomManager.getRoom(getMeetingId()).isWhiteboardEnabled());
		ClientMessage m = new ClientMessage(ClientMessage.DIRECT, userid, "WhiteboardIsWhiteboardEnabledReply", message);
		connInvokerService.sendMessage(m);
	}

	public void sendAnnotationHistory(String userid, String presentationID, Integer pageNumber) {
		Map<String, Object> message = new HashMap<String, Object>();		
		List<Annotation> annotations = roomManager.getRoom(getMeetingId()).getAnnotations(presentationID, pageNumber);
		message.put("count", new Integer(annotations.size()));
		
		/** extract annotation into a Map */
		List<Map<String, Object>> a = new ArrayList<Map<String, Object>>();
		for (Annotation v : annotations) {
			a.add(v.getAnnotation());
		}
		
		message.put("presentationID", presentationID);
		message.put("pageNumber", pageNumber);
		message.put("annotations", a);
		ClientMessage m = new ClientMessage(ClientMessage.DIRECT, userid, "WhiteboardRequestAnnotationHistoryReply", message);
		connInvokerService.sendMessage(m);
	}
	
	private static final String TEXT_CREATED = "textCreated";
	private static final String TEXT_TYPE = "text";
	private static final String PENCIL_TYPE = "pencil";
	private static final String RECTANGLE_TYPE = "rectangle";
	private static final String ELLIPSE_TYPE = "ellipse";
	private static final String TRIANGLE_TYPE = "triangle";
	private static final String LINE_TYPE = "line";
	
	public void sendAnnotation(Annotation annotation) {
		String status = annotation.getStatus();

		if ("textCreated".equals(status)) {
			roomManager.getRoom(getMeetingId()).addAnnotation(annotation);
		} else if (PENCIL_TYPE.equals(annotation.getType()) && "DRAW_START".equals(status)) {
			roomManager.getRoom(getMeetingId()).addAnnotation(annotation);
		} else if ("DRAW_END".equals(status) && (RECTANGLE_TYPE.equals(annotation.getType()) 
													|| ELLIPSE_TYPE.equals(annotation.getType())
													|| TRIANGLE_TYPE.equals(annotation.getType())
													|| LINE_TYPE.equals(annotation.getType()))) {				
			roomManager.getRoom(getMeetingId()).addAnnotation(annotation);
		} else {
			if ("text".equals(annotation.getType())) {
				roomManager.getRoom(getMeetingId()).modifyText(annotation);				
			}
		}
		
		ClientMessage m = new ClientMessage(ClientMessage.BROADCAST, getMeetingId(), "WhiteboardNewAnnotationCommand", annotation.getAnnotation());
		connInvokerService.sendMessage(m);
	}
	
	public void changePage(int pageNum) {
		Presentation pres = roomManager.getRoom(getMeetingId()).getActivePresentation();
		pres.setActivePage(pageNum);
				
		Map<String, Object> message = new HashMap<String, Object>();		
		message.put("pageNum", pageNum);
		message.put("numAnnotations", pres.getActivePage().getNumShapesOnPage());
		ClientMessage m = new ClientMessage(ClientMessage.BROADCAST, getMeetingId(), "WhiteboardChangePageCommand", message);
		connInvokerService.sendMessage(m);
	}
			
	public void clear() {
		roomManager.getRoom(getMeetingId()).clear();

		Map<String, Object> message = new HashMap<String, Object>();		
		ClientMessage m = new ClientMessage(ClientMessage.BROADCAST, getMeetingId(), "WhiteboardClearCommand", message);
		connInvokerService.sendMessage(m);		
	}
			
	public void undo() {
		roomManager.getRoom(getMeetingId()).undo();

		Map<String, Object> message = new HashMap<String, Object>();		
		ClientMessage m = new ClientMessage(ClientMessage.BROADCAST, getMeetingId(), "WhiteboardUndoCommand", message);
		connInvokerService.sendMessage(m);
	}
	
	public void toggleGrid(){
//		System.out.println("toggling grid mode ");
//		roomManager.getRoom(getLocalScope().getName()).toggleGrid();
//		ISharedObject drawSO = getSharedObject(getLocalScope(), WHITEBOARD_SHARED_OBJECT);
//		drawSO.sendMessage("toggleGridCallback", new ArrayList<Object>());
	}
	
	public void setRoomManager(WhiteboardRoomManager manager) {
		this.roomManager = manager;
	}
	
	private String getMeetingId(){
		return Red5.getConnectionLocal().getScope().getName();
	}
	
	private BigBlueButtonSession getBbbSession() {
		return (BigBlueButtonSession) Red5.getConnectionLocal().getAttribute(Constants.SESSION);
	}
	
	public void setRecorderApplication(RecorderApplication a) {
		recorderApplication = a;
	}
	
	public void setConnInvokerService(ConnectionInvokerService connInvokerService) {
		this.connInvokerService = connInvokerService;
	}
}
