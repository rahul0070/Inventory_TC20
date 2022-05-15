package com.porterlee.inventory;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {

    private static long[] vibrationPattern = new long[]{0L, 150L, 100L, 150L};
    public static void copy(File src, File dst) throws IOException {
        if (!dst.exists() && !dst.createNewFile())
            throw new IOException("destination file does not exist and could not be created");

        InputStream in = null;
        try {
            in = new FileInputStream(src);
            OutputStream out = null;
            try {
                out = new FileOutputStream(dst);
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                if (out != null)
                    out.close();
            }
        } finally {
            if (in != null)
                in.close();
        }
    }

    public static void refreshExternalPath(Context context, File file) {
        if (file.isDirectory()) {
            throw new IllegalArgumentException("Directories will be converted to files if refreshed");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            MediaScannerConnection.scanFile(context, new String[] { file.getAbsolutePath() }, null, null);
        } else {
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(file)));
        }
    }

    public static class Holder <T> {
        private T value;

        public Holder(T initialValue) {
            value = initialValue;
        }

        public T get() {
            return value;
        }

        public void set(T object) {
            this.value = object;
        }
    }

    public static boolean vibrate(@NotNull Context context) {
        Vibrator vibrator = (Vibrator)context.getSystemService("vibrator");
        if (Build.VERSION.SDK_INT >= 26) {
            if (vibrator != null) {
                vibrator.vibrate(VibrationEffect.createWaveform(vibrationPattern, -1));
                return true;
            }
        } else if (vibrator != null) {
            vibrator.vibrate(vibrationPattern, -1);
            return true;
        }

        return false;
    }
}
