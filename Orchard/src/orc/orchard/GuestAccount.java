package orc.orchard;

/**
 * The guest account is special.
 * @author quark
 */
public class GuestAccount extends Account {
	public GuestAccount() {
		// jobs can run no more than 30 minutes
		setLifespan(30*60);
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
