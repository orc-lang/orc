package orc.orchard;

public class GuestOnlyAccounts extends AbstractAccounts {
	@Override
	public Account getAccount(String devKey) {
		return guest;
	}
}
