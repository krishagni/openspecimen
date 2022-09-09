//package krishagni.catissueplus.upgrade;
//
//public class FormInfo {
//
//	private boolean participant;
//
//	private boolean scg;
//
//	private boolean specimen;
//
//	private boolean specimenEvent;
//
//	private Long cpId;
//
//	private Long newFormCtxId;
//
//	private Long oldFormCtxId;
//
//	private boolean isDefaultForm;
//
//	public Long getOldFormCtxId() {
//		return oldFormCtxId;
//	}
//
//	public void setOldFormCtxId(Long oldFormCtxId) {
//		this.oldFormCtxId = oldFormCtxId;
//	}
//
//	public boolean isParticipant() {
//		return participant;
//	}
//
//	public void setParticipant(boolean participant) {
//		this.participant = participant;
//	}
//
//	public boolean isScg() {
//		return scg;
//	}
//
//	public void setScg(boolean scg) {
//		this.scg = scg;
//	}
//
//	public boolean isSpecimen() {
//		return specimen;
//	}
//
//	public void setSpecimen(boolean specimen) {
//		this.specimen = specimen;
//	}
//
//	public boolean isSpecimenEvent() {
//		return specimenEvent;
//	}
//
//	public void setSpecimenEvent(boolean specimenEvent) {
//		this.specimenEvent = specimenEvent;
//	}
//
//	public Long getCpId() {
//		return cpId != null ? cpId : -1;
//	}
//
//	public void setCpId(Long cpId) {
//		this.cpId = cpId;
//	}
//
//	public Long getNewFormCtxId() {
//		return newFormCtxId;
//	}
//
//	public void setNewFormCtxId(Long newFormCtxId) {
//		this.newFormCtxId = newFormCtxId;
//	}
//
//	public String getEntityType() {
//		String entityType = null;
//
//		if (participant) {
//			entityType = "Participant";
//		} else if (specimen) {
//			entityType = "Specimen";
//		} else if (scg) {
//			entityType = "SpecimenCollectionGroup";
//		} else if (specimenEvent) {
//			entityType = "SpecimenEvent";
//		}
//
//		return entityType;
//	}
//
//	public boolean isDefaultForm() {
//		return isDefaultForm;
//	}
//
//	public void setDefaultForm(boolean isDefault) {
//		this.isDefaultForm = isDefault;
//	}
//}
