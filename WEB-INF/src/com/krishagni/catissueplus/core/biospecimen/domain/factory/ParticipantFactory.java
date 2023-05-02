
package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;

public interface ParticipantFactory {
	Participant createParticipant(ParticipantDetail detail);
	
	Participant createParticipant(Participant existing, ParticipantDetail detail);
}
