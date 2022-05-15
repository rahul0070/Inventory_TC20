package com.porterlee.inventory;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.HashMap;
import java.util.Map;

public class Splash extends Activity {
    private static final int PERMISSION_ALL = 0;
    private Handler h;
    private Runnable r;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        h = new Handler();
        r = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Splash.this, "Starting", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Splash.this, MainActivity.class));
                finish();
            }
        };

        String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if(!UtilPermissions.hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        else {
            //h.postDelayed(r, 500);
            h.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        int index = 0;
        Map<String, Integer> PermissionsMap = new HashMap<String, Integer>();
        for (String permission : permissions){
            PermissionsMap.put(permission, grantResults[index]);
            index++;
        }

        if((PermissionsMap.get(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0) || PermissionsMap.get(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != 0){
            Toast.makeText(this, "Permission to write external storage is required.", Toast.LENGTH_SHORT).show();
            finish();
        }
        else
        {
            h.postDelayed(r, 1500);
        }
    }
}

class UtilPermissions {
    public static boolean hasPermissions(Splash context, String... allPermissionNeeded)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && context != null && allPermissionNeeded != null)
            for (String permission : allPermissionNeeded)
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                    return false;
        return true;
    }
}