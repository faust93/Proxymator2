package com.faust93.proxymator2;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Created by faust93 on 05.11.13.
 */
public class Utils implements Constants {

    public static boolean checkSu() {
        if (!new File("/system/bin/su").exists()
                && !new File("/system/xbin/su").exists()) {
            Log.e(TAG, "SU does not exist!");
            return false;
        }
        try {
            if (!spawnCmd("ls /data/app-private")) {
                Log.i(TAG, "SU exists and working!");
                return true;
            } else {
                Log.i(TAG, "SU exists but does not work..");
                return false;
            }
        } catch (final NullPointerException e) {
            Log.e(TAG, e.getLocalizedMessage().toString());
            return false;
        }
    }

    // process murderer
    public static void killProc(String processName) throws Exception {

        Process process = null;
        String[] temp;

        process = Runtime.getRuntime().exec("ps");

        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()),8192);
        String line = null;
        while ((line = in.readLine()) != null) {
            if (line.contains(processName)) {
                temp = line.split(" +" ,15);
                Log.d(TAG,"killing PID: " + temp[1]);
                spawnCmd("kill " + temp[1]);
            }
        }
        in.close();
        process.waitFor();

    }

    // spawn ya super cmd!
    public static boolean spawnCmd(String cmd) {

        Process process = null;
        BufferedReader input = null;
        boolean rc;

        Log.d(TAG, "spawnCmd() entry: " + cmd);

        try {
            process = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            input = new BufferedReader(new InputStreamReader(process.getInputStream()),4096);

            os.writeBytes(cmd + "\n");
            os.flush();

            os.writeBytes("exit\n");
            os.flush();

            input.close();
            process.waitFor();

        } catch (Exception err) {
            err.printStackTrace();
        }
        finally
        {
            try {
                if (input != null) {
                    input.close();
                }

                if(process.exitValue() == 0)
                    rc = false;
                else
                    rc = true;

                process.destroy();
                return rc;
            }
            catch (IOException e) {
                return true;
            }
        }
    }


    // check running proc by name
    public static boolean checkProcess(String processName) throws Exception {

        Process process = null;
        if(debug!=0){
            Log.d(TAG,"checkProcess() entry: " + processName);
        }
        process = Runtime.getRuntime().exec("ps");

        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null;
        while ((line = in.readLine()) != null) {
            if (line.contains(processName)) {
                return true;
            }
        }
        in.close();
        process.waitFor();
        return false;
    }

    public static boolean copyApp(AssetManager ass, String app, String dest_dir)  {

        try {
        InputStream myInput = ass.open(app);
        File file = new File (dest_dir, app.substring(app.indexOf("/") + 1));

        if(!file.exists()){

            file.createNewFile();
            OutputStream myOutput = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            myOutput.flush();
            myOutput.close();

            spawnCmd("/system/bin/chmod 744 " + dest_dir + "/" + app.substring(app.indexOf("/")+1));
        }
        myInput.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    // check iptables rule
    public static boolean checkFwRule(String match) throws Exception {

        Process process = null;
        process = Runtime.getRuntime().exec("iptables -nt nat -L OUTPUT");

        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = null;
        while ((line = in.readLine()) != null) {
            if (line.contains(match)) {
                return true;
            }
        }
        in.close();
        process.waitFor();
        return false;
    }

}

