package com.faust93.proxymator2;

import java.io.Serializable;

/**
 * Created by faust93 on 24.10.13.
 */
public class ProxyProfile implements Serializable {

    public boolean active;
    public enum Auth { BASIC, NTLM };

    public String profile_Name;
    public String associated_Network;
    public boolean autoConnect;

    public String proxy_Address;
    public String proxy_Port;

    public boolean auth_Enabled;
    public boolean dns_Enabled;
    public boolean dns_UsePOST;

    public Auth auth_Type;
    public String auth_Username;
    public String auth_Password;
    public String dns_Resolver;
    public String dns_QueryString;
    public String bypass_IPs;


    public ProxyProfile(String name)
    {
        this.active = false;
        this.profile_Name = name;

        this.associated_Network = "";
        this.proxy_Address = "";
        this.proxy_Port = "8080";
        this.dns_Resolver = "http://gday.bloke.com/cgi-bin/nslookup";
        this.dns_QueryString = "%s";
        this.bypass_IPs = "";

        this.auth_Enabled = false;
        this.dns_Enabled = true;
        this.dns_UsePOST = false;
        this.autoConnect = false;

        this.auth_Type = Auth.BASIC;
        this.auth_Username = "";
        this.auth_Password = "";
    }

}

