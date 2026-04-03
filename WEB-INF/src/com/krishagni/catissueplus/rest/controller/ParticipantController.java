
package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipantsList;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.services.ParticipantService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.services.FormService;

@Controller
@RequestMapping("/participants")
public class ParticipantController {
	@Autowired
	private HttpServletRequest httpServletRequest;

	@Autowired
	private ParticipantService participantSvc;
	
	@Autowired
	private FormService formSvc;

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ParticipantDetail getParticipantById(@PathVariable("id") Long participantId) {
		return response(participantSvc.getParticipant(request(participantId)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/match")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<MatchedParticipant> getMatchedParticipants(@RequestBody ParticipantDetail criteria) {
		if (criteria == null) {
			return null;
		}

		criteria.setReqRegInfo(true);
		List<MatchedParticipantsList> result = response(participantSvc.getMatchingParticipants(request(Collections.singletonList(criteria))));
		return result.get(0).getMatches();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/multi-match")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<MatchedParticipantsList> getMatchedParticipants(@RequestBody List<ParticipantDetail> criteriaList) {
		for (ParticipantDetail criteria : criteriaList) {
			criteria.setReqRegInfo(true);
		}

		return response(participantSvc.getMatchingParticipants(request(criteriaList)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/extension-form")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getForm(
		@RequestParam(value = "cpId", required = false, defaultValue = "-1")
		Long cpId) {

		return formSvc.getExtensionInfo(cpId, Participant.EXTN);
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
