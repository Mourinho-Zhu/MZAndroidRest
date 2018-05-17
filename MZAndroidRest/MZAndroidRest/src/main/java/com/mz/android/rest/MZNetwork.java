package com.mz.android.rest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Android网络工具类
 *
 * @author Mourinho.Zhu on 16-11-24.
 */
public final class MZNetwork {
    //私有构造函数
    private MZNetwork() {
    }

    //TAG
    private static final String TAG = "MZNet";

    /**
     * 查看WIFI是否可用，需要ACCESS_NETWORK_STATE权限
     *
     * @param context 上下文菜单
     * @return WIFI可用返回true
     */
    public static boolean isWifiConnected(final Context context) {
        return isNetworkAvailable(context, ConnectivityManager.TYPE_WIFI);
    }

    /**
     * 查看流量是否可用，需要ACCESS_NETWORK_STATE权限
     *
     * @param context 上下文菜单
     * @return 流量可用返回true
     */
    public static boolean isMobileConnected(final Context context) {
        return isNetworkAvailable(context, ConnectivityManager.TYPE_MOBILE);
    }

    /**
     * 查看网络是否可用，需要ACCESS_NETWORK_STATE权限
     *
     * @param context 上下文菜单
     * @return 网络可用返回true
     */
    public static boolean isNetworkAvailable(final Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isNetworkAvailable(final Context context,
                                              final Integer type) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return null == type
                                || info[i].getType() == type.intValue();
                    }
                }
            }
        }
        return false;
    }

    /**
     * 获取IP地址
     *
     * @param context 上下文
     * @return IP地址，无法获取返回null
     */
    public static String getIpAddress(Context context) {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress inetAddress = (InetAddress) addrs.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

}
