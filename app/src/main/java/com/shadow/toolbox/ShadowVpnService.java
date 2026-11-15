package com.shadow.toolbox;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

/**
 * Local VPN foundation. Shadow Toolbox does not provide a VPN server by itself.
 * A future connector can use this service to build the real tunnel.
 */
public class ShadowVpnService extends VpnService {
    private ParcelFileDescriptor interfaceFd;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Keep this service inert until a real tunnel backend is configured.
        // This avoids creating a fake VPN or silently routing traffic.
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            if (interfaceFd != null) interfaceFd.close();
        } catch (Exception ignored) {}
        interfaceFd = null;
        super.onDestroy();
    }
}
