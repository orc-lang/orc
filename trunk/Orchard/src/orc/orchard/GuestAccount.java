package orc.orchard;

/**
 * The guest account is special.
 * @author quark
 */
public class GuestAccount extends Account {
	public GuestAccount() {
		// set properties
		setQuota(OrchardProperties.getInteger(
				"orc.orchard.GuestAccount.quota"));
		setLifespan(OrchardProperties.getInteger(
				"orc.orchard.GuestAccount.lifespan"));
		setCanSendMail(OrchardProperties.getBoolean(
				"orc.orchard.GuestAccount.canSendMail",
				getCanSendMail()));
		setCanImportJava(OrchardProperties.getBoolean(
				"orc.orchard.GuestAccount.canImportJava",
				getCanImportJava()));
		setTokenPoolSize(OrchardProperties.getInteger(
				"orc.orchard.GuestAccount.tokenPoolSize",
				-1));
		setStackSize(OrchardProperties.getInteger(
				"orc.orchard.GuestAccount.stackSize",
				-1));
	}
	@Override
	public boolean getIsGuest() {
		return true;
	}
	@Override
	protected void onNoMoreJobs() {
		// the guest account never needs to be deleted
	}
}
