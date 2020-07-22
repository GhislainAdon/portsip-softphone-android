package com.portgo.manager;

public interface ICustomCdt {
	String getCdt(final UserAccount account,final String remoteAccountString,final PortSipCall call);
}
