package com.faust93.proxymator2;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.exceptions.RootDeniedException;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.CommandCapture;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Created by faust93 on 10.10.13.
 */
///// FIRST RUN - CREATE PROFS!!
public class MainFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, Constants {

    private View myView;

    private ProxyProfile[] Profiles;

    private int cur_profile; // current profile

    private static boolean rstate = false; // running state 1 - running, 0 - not

    private TextView profileName;
    private TextView profileProxyAddr;
    private TextView profileProxyPort;
    private TextView profileNetwork;
    private CheckBox profileAtoConnect;
    private CheckBox profileUseAuth;
    private CheckBox profileUseDns;
    private CheckBox profileDnsPost;
    private TextView profileUsername;
    private TextView profilePassword;
    private TextView profileDnsUrl;
    private TextView profileDnsQuery;
    private TextView profileBypassIPs;

    private static SharedPreferences preferences;

    // application data dir
    private static String myDataDir;

    // this fragment activity
    private static Context parentActivity;

    // TODO implement arch detection
    private static String arch = "arm";

    static private boolean logging;
    static private int max_profs = 2;

    // trick to get my activity
    // @Override
    // public void onAttach(Activity activity) {
    //    super.onAttach(activity);
    //    parentActivity = activity;
    // }

    static public void setLogging (boolean val){
        logging = val;
    }

    static public boolean getLogging (){
        return logging;
    }
    //*********************
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        myView = inflater.inflate(R.layout.main_fragment, container, false);

        stub_init(getActivity());

        // set listeners for profile add/del buttons
        ImageButton imageButton;
        imageButton = (ImageButton) myView.findViewById(R.id.prof_add_btn);
        imageButton.setOnClickListener(this);
        imageButton = (ImageButton) myView.findViewById(R.id.prof_del_btn);
        imageButton.setOnClickListener(this);
        imageButton = (ImageButton) myView.findViewById(R.id.prof_save_btn);
        imageButton.setOnClickListener(this);

        profileName = (TextView) myView.findViewById(R.id.profile_name);
        profileName.setOnClickListener(this);

        // find views
        profileNetwork = (TextView) myView.findViewById(R.id.profile_associated_net);
        profileProxyAddr = (TextView) myView.findViewById(R.id.profile_proxy_address);
        profileProxyPort = (TextView) myView.findViewById(R.id.profile_proxy_port);
        profileUsername = (TextView) myView.findViewById(R.id.profile_auth_username);
        profilePassword = (TextView) myView.findViewById(R.id.profile_auth_passw);
        profileDnsUrl = (TextView) myView.findViewById(R.id.profile_dns_resolver);
        profileDnsQuery = (TextView) myView.findViewById(R.id.profile_dns_query_str);
        profileBypassIPs = (TextView) myView.findViewById(R.id.profile_bypass_ips);

        // set onclick handler
        myView.findViewById(R.id.associated_network).setOnClickListener(this);
        myView.findViewById(R.id.proxy_address).setOnClickListener(this);
        myView.findViewById(R.id.proxy_port).setOnClickListener(this);
        myView.findViewById(R.id.auth_passw).setOnClickListener(this);
        myView.findViewById(R.id.auth_username).setOnClickListener(this);
        myView.findViewById(R.id.dns_resolver).setOnClickListener(this);
        myView.findViewById(R.id.dns_query).setOnClickListener(this);
        myView.findViewById(R.id.bypass_ips).setOnClickListener(this);

        profileAtoConnect = (CheckBox) myView.findViewById(R.id.proxy_auto_connect);
        profileUseAuth = (CheckBox) myView.findViewById(R.id.proxy_use_auth);
        profileUseDns = (CheckBox) myView.findViewById(R.id.proxy_use_dns);
        profileDnsPost = (CheckBox) myView.findViewById(R.id.proxy_use_post);

        profileAtoConnect.setOnCheckedChangeListener(this);
        profileUseAuth.setOnCheckedChangeListener(this);
        profileUseDns.setOnCheckedChangeListener(this);
        profileDnsPost.setOnCheckedChangeListener(this);

        Utils.copyApp(parentActivity.getAssets(), arch + "/" + "u2nl", myDataDir);
        Utils.copyApp(parentActivity.getAssets(), arch + "/" + "dnsp", myDataDir);
        Utils.copyApp(parentActivity.getAssets(), arch + "/" + "xtables-multi", myDataDir);
        Utils.copyApp(parentActivity.getAssets(), arch + "/" + "dnsproxy2", myDataDir);

        initProfiles();

        return myView;
    }
    //*********************
    // return running status
    public static boolean get_rstate(){

        boolean tmp = false;
        boolean tmp1 = false;

        try {
            tmp = Utils.checkFwRule("1256");
            tmp1 = Utils.checkProcess("u2nl");
        } catch (Exception e) { e.printStackTrace();}

        if(!tmp && !tmp1)
            rstate = false;
        else
            rstate = true;

        return rstate;
    }

    public boolean start_proxy(){
        return start_proxy_profile(Profiles[cur_profile]);
    }

    // auto start proxy from network receiver
    static public boolean start_proxy_nr(Context ctx, int profile_num) {
        ProxyProfile pProfile;
        pProfile = readProfile(ctx,"profile" + profile_num);
        if(start_proxy_profile(pProfile)){
            doNotify(true,pProfile.profile_Name);
            return true;
        }
        return false;
    }
    //*********************
    // start tunneling
    static public boolean start_proxy_profile(ProxyProfile profile){

        String ip = profile.proxy_Address;
        String port = profile.proxy_Port;
        String user = profile.auth_Username;
        String pass = profile.auth_Password;
        String bypassIPs = profile.bypass_IPs;

        String dnsp_exec = "";
        String dnsproxy2 = "";

        if(logging)
            RootTools.debugMode = true;
        else
            RootTools.debugMode = false;

        //clean rules
      //  Utils.spawnCmd(myDataDir + "/iptables -P INPUT ACCEPT");
      //  Utils.spawnCmd(myDataDir + "/iptables -P OUTPUT ACCEPT");
      //  Utils.spawnCmd(myDataDir + "/iptables -P FORWARD ACCEPT");
      //  Utils.spawnCmd(myDataDir + "/iptables -F");

        iptablesSave();

        CommandCapture command = new CommandCapture(0, myDataDir + "/xtables-multi iptables -t nat -F", myDataDir + "/xtables-multi iptables -X");
        try {
            RootTools.getShell(true).add(command);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        }

        String u2nl_exec = String.format("/u2nl %s %s 1256", ip, port);
        if(profile.auth_Enabled && user.length() >= 1 && pass.length() >= 1)
            u2nl_exec.concat(" -u" + user + " -p " + pass);

        // dns handler
        // does not work anymore on >= 4.3
        // Utils.spawnCmd("setprop net.dns1 127.0.0.1");
        // Alternatively we can get current DNS address and redirect all queries by this rule
        // Utils.spawnCmd(myDataDir + "/iptables -t nat -I OUTPUT -p udp -d DNS_IP --dport 53 -j DNAT --to-destination 127.0.0.1:53");
        if(profile.dns_Enabled){

           dnsp_exec = "/dnsp -p 53 -l 127.0.0.1 -h " + ip + " -r " + port + " -s " + profile.dns_Resolver + " -P " + "\"" + profile.dns_QueryString+"\"";
           dnsproxy2 = "/dnsproxy2 127.0.0.1 &";

            if(profile.dns_UsePOST)
                dnsp_exec.concat(" -m 1");
            else
                dnsp_exec.concat(" -m 0");

            if(profile.auth_Enabled && user.length() >= 1 && pass.length() >= 1)
                dnsp_exec.concat(" -u " + user + " -x " + pass);
        }

            if(bypassIPs.length() >= 7 ){
                command = new CommandCapture(0, myDataDir + "/xtables-multi iptables -t nat -A OUTPUT -p 6 -d " + bypassIPs + " -j ACCEPT");
                try {
                    RootTools.getShell(true).add(command);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                } catch (RootDeniedException e) {
                    e.printStackTrace();
                }

            }
            String nat_rules = String.format("/xtables-multi iptables -t nat -A OUTPUT -p 6 -d ! %s -j REDIRECT --to-port 1256", ip);
            command = new CommandCapture(0, myDataDir + u2nl_exec + " &", myDataDir + dnsp_exec + " &", myDataDir + dnsproxy2, myDataDir + nat_rules );
            try {
                commandWait(RootTools.getShell(true).add(command));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (RootDeniedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(!get_rstate()){
                stop_proxy();
                rstate = false;
            } else {

                rstate = true;
            }
                return rstate;
    }

    private static void commandWait(Command cmd) throws Exception {

        while (!cmd.isFinished()) {

            synchronized (cmd) {
                try {
                    if (!cmd.isFinished()) {
                        cmd.wait(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // iptables rules save & restore
    public static void iptablesSave(){
        CommandCapture command = new CommandCapture(0, myDataDir + "/xtables-multi save4 > " + myDataDir + "/rules");
        try {
            RootTools.getShell(true).add(command);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        }
    }
    public static void iptablesRestore(){
        CommandCapture command = new CommandCapture(0, myDataDir + "/xtables-multi restore4 < " + myDataDir + "/rules");
        try {
            RootTools.getShell(true).add(command);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        }
    }
    //*********************
    static public boolean stop_proxy(){

        CommandCapture command = new CommandCapture(0, myDataDir + "/xtables-multi iptables -t nat -F", myDataDir + "/xtables-multi iptables -X");
        try {
            RootTools.getShell(true).add(command);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (RootDeniedException e) {
            e.printStackTrace();
        }

        iptablesRestore();

        try {
            Utils.killProc("proxymator2/");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        rstate = false;
        return rstate;
    }

    public static void stub_init(Context ctx){
        parentActivity = ctx;
        preferences = parentActivity.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        myDataDir = parentActivity.getApplicationInfo().dataDir;
        setLogging(preferences.getBoolean("enableLogging", false));
        if(iPrId(ctx))
            max_profs = MAX_PROFILES;
    }

    //********************* enable/disable subviews depending on checkbox state
    private void enableDisableView(View view, boolean enabled) {
        view.setEnabled(enabled);
        if ( view instanceof ViewGroup ) {
            ViewGroup group = (ViewGroup)view;
            for ( int idx = 0 ; idx < group.getChildCount() ; idx++ ) {
                enableDisableView(group.getChildAt(idx), enabled);
            }
        }
    }

    //*********************
    // set profile
    private void setProfile(int profile){

        profileName.setText(Profiles[profile].profile_Name);
        if(!Profiles[profile].associated_Network.isEmpty())
            profileNetwork.setText(Profiles[profile].associated_Network);
        else
            profileNetwork.setText("None");

        if(!Profiles[profile].proxy_Address.isEmpty())
           profileProxyAddr.setText(Profiles[profile].proxy_Address);
        else
           profileProxyAddr.setText(getString(R.string.proxy_srv_addres));

        profileProxyPort.setText(Profiles[profile].proxy_Port);

        if(!Profiles[profile].auth_Username.isEmpty())
            profileUsername.setText(Profiles[profile].auth_Username);
        else
            profileUsername.setText(getString(R.string.proxy_user_name));

        if(!Profiles[profile].auth_Password.isEmpty())
            profilePassword.setText(Profiles[profile].auth_Password);
        else
            profilePassword.setText(getString(R.string.proxy_user_passwd));

        if(!Profiles[profile].bypass_IPs.isEmpty())
            profileBypassIPs.setText(Profiles[profile].bypass_IPs);
        else
            profilePassword.setText(getString(R.string.donot_proxify));

        profileDnsUrl.setText(Profiles[profile].dns_Resolver);
        profileDnsQuery.setText(Profiles[profile].dns_QueryString);

        profileAtoConnect.setChecked(Profiles[profile].autoConnect);
        profileUseAuth.setChecked(Profiles[profile].auth_Enabled);
        profileUseDns.setChecked(Profiles[profile].dns_Enabled);
        profileDnsPost.setChecked(Profiles[profile].dns_UsePOST);

        enableDisableView(myView.findViewById(R.id.ui_auth_layout), profileUseAuth.isChecked());
        enableDisableView(myView.findViewById(R.id.ui_dns_layout),profileUseDns.isChecked());

    }

    //*********************
    // init profiles after startup
    // TODO
    private void initProfiles() {

        String network_name = "";
        Profiles = new ProxyProfile[max_profs+1];

        for(int i=0; i < max_profs; i++)
              Profiles[i] = readProfile(parentActivity, "profile" + i);

        WifiManager wifiManager = (WifiManager) parentActivity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        cur_profile = preferences.getInt("activeProfile",0);

        if (null != wifiInfo.getBSSID()) {
            int profile = matchProfile(parentActivity, wifiInfo.getSSID().replaceAll("\"", ""), false);
            if(profile != NOT_FOUND)
                cur_profile = profile;
        }

        setProfile(cur_profile);
    }

    // get number of active profiles
    private int getActiveProfiles() {
        int numProf = 0;
        for(int i=0; i < max_profs; i++){
            if(Profiles[i].active)
                numProf++;
        }
        return numProf;
    }

    //*********************
    // checkbox event handler
    @Override
    public void onCheckedChanged(CompoundButton cbBox, boolean isChecked){
        switch (cbBox.getId()) {
            case R.id.proxy_use_auth:
                 if (isChecked){
                    enableDisableView(myView.findViewById(R.id.ui_auth_layout),true);
                    Profiles[cur_profile].auth_Enabled = true;
                 } else {
                    enableDisableView(myView.findViewById(R.id.ui_auth_layout),false);
                    Profiles[cur_profile].auth_Enabled = false;
                 }
                break;
            case R.id.proxy_use_dns:
                if (isChecked){
                    enableDisableView(myView.findViewById(R.id.ui_dns_layout),true);
                    Profiles[cur_profile].dns_Enabled = true;
                } else {
                    enableDisableView(myView.findViewById(R.id.ui_dns_layout),false);
                    Profiles[cur_profile].dns_Enabled = false;
                }
                break;
            case R.id.proxy_use_post:
                 Profiles[cur_profile].dns_UsePOST = isChecked;
                break;
            case R.id.proxy_auto_connect:
                Profiles[cur_profile].autoConnect = isChecked;
                break;
        }
    }

    //*********************
    // main clicks event handler
    @Override
    public void onClick(View v){
        switch (v.getId()) {
            case R.id.associated_network: selectNetwork(v); break;
            case R.id.profile_name: selectProfile(v); break;
            case R.id.prof_add_btn: addProfile(); break;
            case R.id.prof_del_btn:
                if(!Profiles[cur_profile].profile_Name.equals("empty"))
                    Toast.makeText(parentActivity, getString(R.string.profile_deleted),
                            Toast.LENGTH_LONG).show();
                delProfile(cur_profile);
                for(int i=0; i < max_profs; i++)
                    saveProfile(i);
                updatePrefs();
                break;
            case R.id.prof_save_btn:
                for(int i=0; i < max_profs; i++)
                    saveProfile(i);
                Toast.makeText(parentActivity, getString(R.string.profile_saved),
                        Toast.LENGTH_LONG).show();
                updatePrefs();
                break;
            case R.id.proxy_address: setProxyAddr(); break;
            case R.id.proxy_port: setProxyPort(); break;
            case R.id.auth_username: setAuthUser(); break;
            case R.id.auth_passw: setAuthPasswd(); break;
            case R.id.dns_resolver: setDnsResolver(); break;
            case R.id.dns_query: setDnsQuery(); break;
            case R.id.bypass_ips: setBypassIPs(); break;
        }
    }

    private void setBypassIPs(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.bypass_ip);
        final EditText text_input = new EditText(getActivity());
        alert.setView(text_input);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = text_input.getText().toString();
                if(value.length() >= 7) {
                    Profiles[cur_profile].bypass_IPs = value;
                    setProfile(cur_profile);
                } else {
                    Toast.makeText(parentActivity, getString(R.string.value_too_short),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    //*********************
    // set dns query string
    private void setDnsQuery(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.dns_query_str);
        final EditText text_input = new EditText(getActivity());
        alert.setView(text_input);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = text_input.getText().toString();
                if(value.length() >= 2) {
                    Profiles[cur_profile].dns_QueryString = value;
                    setProfile(cur_profile);
                } else {
                    Toast.makeText(parentActivity, getString(R.string.value_too_short),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    //*********************
    // set dns resolver
    private void setDnsResolver(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.dns_resolver);
        final EditText text_input = new EditText(getActivity());
        alert.setView(text_input);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = text_input.getText().toString();
                if(value.length() >= 10) {
                    Profiles[cur_profile].dns_Resolver = value;
                    setProfile(cur_profile);
                } else {
                    Toast.makeText(parentActivity, getString(R.string.value_too_short),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    //*********************
    // set userpass
    private void setAuthPasswd(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.password);
        final EditText text_input = new EditText(getActivity());
        alert.setView(text_input);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = text_input.getText().toString();
                if(value.length() >= 1) {
                    Profiles[cur_profile].auth_Password = value;
                    setProfile(cur_profile);
                } else {
                    Toast.makeText(parentActivity, getString(R.string.value_too_short),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    //*********************
    // set username
    private void setAuthUser(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.username);
        final EditText text_input = new EditText(getActivity());
        alert.setView(text_input);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = text_input.getText().toString();
                if(value.length() >= 2) {
                    Profiles[cur_profile].auth_Username = value;
                    setProfile(cur_profile);
                } else {
                    Toast.makeText(parentActivity, getString(R.string.value_too_short),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    //*********************
    // set proxy port
    private void setProxyPort(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.proxy_port);
        final EditText text_input = new EditText(getActivity());
        text_input.setInputType(InputType.TYPE_CLASS_NUMBER);
        alert.setView(text_input);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = text_input.getText().toString();
                if(value.length() >= 2) {
                    Profiles[cur_profile].proxy_Port = value;
                    setProfile(cur_profile);
                } else {
                    Toast.makeText(parentActivity, getString(R.string.proxy_port_too_short),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    //*********************
    // set proxy address
    private void setProxyAddr(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.proxy_addr);
        final EditText text_input = new EditText(getActivity());
        alert.setView(text_input);
        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = text_input.getText().toString();
                if(value.length() >= 3) {
                    Profiles[cur_profile].proxy_Address = value;
                    setProfile(cur_profile);
                } else {
                    Toast.makeText(parentActivity, getString(R.string.proxy_addr_too_short),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });
        alert.show();
    }

    //*********************
    // select network to associate with
    private void selectNetwork(View v) {

        final String Items[];
        int index = 1;
        int netCount = 0;

        //list configured networks
        WifiManager wifiMgr = (WifiManager) parentActivity.getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> myNetworks = wifiMgr.getConfiguredNetworks();

        if(myNetworks != null)
            netCount = myNetworks.size();

        Items = new String [netCount+1];
        Items[0] = "None";

        if(netCount != 0){
            for (WifiConfiguration net : myNetworks) {
                Items[index] = net.SSID.replaceAll("\"","");
                index ++;
            }
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.select_net));
        builder.setItems(Items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Profiles[cur_profile].associated_Network = Items[item];
                setProfile(cur_profile);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    //*********************
    // select active profile
    private void selectProfile(View v) {

        final String Items[];

        int index = 0;
        int numProf = getActiveProfiles();

        if(numProf !=0 ) {
            Items = new String [numProf];
        } else {
            Items = new String [1];
            Items[0] = "";
        }
        for(int i = 0; i < max_profs; i++ ) {
            if(Profiles[i].active){
                Items[index] = Profiles[i].profile_Name;
                index++;
            }
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.profiles));
        builder.setItems(Items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                for(int i=0; i < max_profs; i++){
                    if(Profiles[i].profile_Name.equals(Items[item])){
                        cur_profile = i;
                    }
                }
                setProfile(cur_profile);
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    //*********************
    // save profile
    private void saveProfile(int profile) {
        try {
        FileOutputStream fos = parentActivity.openFileOutput("profile" + profile , Context.MODE_PRIVATE);
        ObjectOutputStream os = new ObjectOutputStream(fos);
        os.writeObject(Profiles[profile]);
        os.flush();
        os.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    //*********************
    // read profile
    static private ProxyProfile readProfile(Context ctx, String fileName) {
        ProxyProfile profileObj = new ProxyProfile("empty");
        try {
            FileInputStream fis = ctx.openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);
             profileObj = (ProxyProfile) is.readObject();
             is.close();
         } catch ( Exception e ) {
            // first run. just return empty profile.
         }
        return profileObj;
    }

    //*********************
    // del proxy profile
    private  void delProfile(int profile){
            Profiles[profile] = new ProxyProfile("empty");
            cur_profile = 0;
            for(int i = 0; i < max_profs; i++ ){
                if(Profiles[i].active)
                    cur_profile = i;
            }
            setProfile(cur_profile);
    }

    //*********************
    // add new proxy profile
    private void addProfile(){

        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

        alert.setTitle(R.string.add_profile);
        alert.setMessage(R.string.specify_name);

        final EditText text_input = new EditText(getActivity());
        alert.setView(text_input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                boolean added = false;
                String value = text_input.getText().toString();
                for(int i=0; i < max_profs; i++){
                    if(!Profiles[i].active){
                        Profiles[i].profile_Name = value;
                        Profiles[i].active = true;
                        added = true;
                        cur_profile = i;
                        break;
                    }
                }
                if(added)
                    setProfile(cur_profile);
                else {
                    if(iPrId(parentActivity))
                        Toast.makeText(parentActivity, getString(R.string.max_profiles_error),
                            Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(parentActivity, getString(R.string.max_profiles_lite_error),
                                Toast.LENGTH_LONG).show();
                    }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    //*********************
    // update preferences
    public void updatePrefs() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("activeProfile", cur_profile);
        editor.commit();
    }

    static public SharedPreferences getPreferences(){
        return preferences;
    }

    public String getCurProfileName(){
        return Profiles[cur_profile].profile_Name;
    }

    //**********************
    // status bar notification
    static public void doNotify(boolean state, String notify_string){

        NotificationManager nMgr = (NotificationManager)parentActivity.getSystemService(Context.NOTIFICATION_SERVICE);

        if(state){
            NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(parentActivity);
            Intent intent = new Intent(parentActivity, Proxymator.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(parentActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            nBuilder.setOngoing(true);
            nBuilder.setSmallIcon(R.drawable.pmator_small); //android.R.color.transparent
            nBuilder.setContentTitle(TAG);
            nBuilder.setContentText("Proxymator is running using " + notify_string + " profile");
            nBuilder.setContentIntent(pendingIntent);
            nMgr.notify(123, nBuilder.build());
       } else {
           nMgr.cancel(123);
        }

    }

    // match network associated profile
    static public int matchProfile(Context ctx, String network_name, boolean auto_state) {

        ProxyProfile[] Profiles = new ProxyProfile[max_profs+1];

        for(int i=0; i < max_profs; i++){
            Profiles[i] = readProfile(ctx,"profile" + i);
           // Log.d(TAG,"Profile: " + Profiles[i].profile_Name + Profiles[i].autoConnect + network_name);
            if(auto_state){
                if(Profiles[i].active && Profiles[i].autoConnect
                        && Profiles[i].associated_Network.equals(network_name))
                    return i;
                } else {
                if(Profiles[i].active && Profiles[i].associated_Network.equals(network_name))
                    return i;
            }
        }
    return NOT_FOUND;
    }

    protected static boolean iPrId(Context context) {
        PackageManager manager = context.getPackageManager();
        if (manager.checkSignatures(context.getPackageName(), "com.faust93.proxymator2.key")
                == PackageManager.SIGNATURE_MATCH) {
            return true;
        }
        return false;
    }
}