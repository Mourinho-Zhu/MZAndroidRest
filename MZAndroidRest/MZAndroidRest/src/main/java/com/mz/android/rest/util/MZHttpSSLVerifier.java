package com.mz.android.rest.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public class MZHttpSSLVerifier {

    public static HostnameVerifier getHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
    }

    public static MZHttpUtils.SSLParams getSslSocketFactoryUseAssets(Context context, String caCrtAssetsPath,
                                                                     String clientCrtAssetsPath,
                                                                     String clientKeyAssetsPath,
                                                                     String password) {
        InputStream caCrt = null;
        InputStream clientCrt = null;
        InputStream clientKey = null;

        try {
            if (!TextUtils.isEmpty(caCrtAssetsPath)) {
                caCrt = context.getApplicationContext().getAssets().open(caCrtAssetsPath);
            }
            InputStream[] streams = null;
            if (null != caCrt) {
                streams = new InputStream[]{caCrt};
            }
            if ((!TextUtils.isEmpty(clientCrtAssetsPath))
                    && (!TextUtils.isEmpty(clientKeyAssetsPath))) {
                clientCrt = context.getApplicationContext().getAssets().open(clientCrtAssetsPath);
                clientKey = context.getApplicationContext().getAssets().open(clientKeyAssetsPath);
            }
            MZHttpUtils.SSLParams sslParams = getSslSocketFactory(streams, clientCrt, clientKey, password);
            return sslParams;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeInputStreamIfNecessary(caCrt);
            closeInputStreamIfNecessary(clientCrt);
            closeInputStreamIfNecessary(clientKey);
        }
        return null;
    }

    public static MZHttpUtils.SSLParams getSslSocketFactory(String serverCA,
                                                            String clientCA,
                                                            String clientKey, String password) {
        InputStream serverCrtInput = null;
        InputStream clientCrtInput = null;
        InputStream clientKeyInput = null;
        try {
            InputStream[] streams = null;
            if (!TextUtils.isEmpty(serverCA)) {
                serverCrtInput = new ByteArrayInputStream(serverCA.getBytes("UTF-8"));
                streams = new InputStream[]{serverCrtInput};
            }
            if ((!TextUtils.isEmpty(clientCA)) && (!TextUtils.isEmpty(clientCA))) {
                clientCrtInput = new ByteArrayInputStream(clientCA.getBytes("UTF-8"));
                clientKeyInput = new ByteArrayInputStream(clientKey.getBytes("UTF-8"));
            }
            MZHttpUtils.SSLParams sslParams = getSslSocketFactory(streams, clientCrtInput, clientKeyInput, password);
            return sslParams;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeInputStreamIfNecessary(serverCrtInput);
            closeInputStreamIfNecessary(clientCrtInput);
            closeInputStreamIfNecessary(clientKeyInput);
        }
        return null;
    }


    static void closeInputStreamIfNecessary(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static MZHttpUtils.SSLParams getSslSocketFactory(InputStream[] certificates, InputStream clientCertFile
            , InputStream clientPrivateKeyFile, String password) {
        MZHttpUtils.SSLParams sslParams = new MZHttpUtils.SSLParams();
        try {
            TrustManager[] trustManagers = prepareTrustManager(certificates);
            KeyManager[] keyManagers = prepareKeyManager(clientCertFile, clientPrivateKeyFile, password);
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            X509TrustManager trustManager = null;
            if (trustManagers != null) {
                trustManager = new MyTrustManager(chooseTrustManager(trustManagers));
            } else {
                trustManager = new UnSafeTrustManager();
            }
            sslContext.init(keyManagers, new TrustManager[]{trustManager}, null);
            sslParams.sSLSocketFactory = sslContext.getSocketFactory();
            sslParams.trustManager = trustManager;
            return sslParams;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        } catch (KeyManagementException e) {
            throw new AssertionError(e);
        } catch (KeyStoreException e) {
            throw new AssertionError(e);
        }
    }


    private class UnSafeHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    private static class UnSafeTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }


    private static X509TrustManager chooseTrustManager(TrustManager[] trustManagers) {
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        return null;
    }


    private static class MyTrustManager implements X509TrustManager {
        private X509TrustManager defaultTrustManager;
        private X509TrustManager localTrustManager;

        public MyTrustManager(X509TrustManager localTrustManager) throws NoSuchAlgorithmException, KeyStoreException {
            TrustManagerFactory var4 = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            var4.init((KeyStore) null);
            defaultTrustManager = chooseTrustManager(var4.getTrustManagers());
            this.localTrustManager = localTrustManager;
        }


        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException ce) {
                localTrustManager.checkServerTrusted(chain, authType);
            }
        }


        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }


    public static String KEY_STORE_TYPE = "bks";

    private static TrustManager[] prepareTrustManager(InputStream... certificates) {
        if (certificates == null || certificates.length <= 0) {
            return null;
        }
        try {
            BouncyCastleProvider provider = new BouncyCastleProvider();
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE, provider);
            keyStore.load(null, null);
            int index = 0;
            for (InputStream certificate : certificates) {
                String certificateAlias = Integer.toString(index++);
                keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));
                try {
                    if (certificate != null) {
                        certificate.close();
                    }
                } catch (IOException e) {
                }
            }
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            return trustManagers;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private static KeyManager[] prepareKeyManager(InputStream clientCertFile,
                                                  InputStream clientPrivateKeyFile, String keyStorePassword) {
        try {
            if (clientCertFile == null || clientPrivateKeyFile == null) {
                return null;
            }
            //x.509 Ca certificate factory
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
//            Load client certificate
            X509Certificate clientCert = (X509Certificate) cf.generateCertificate(clientCertFile);
//            Load client private key
            PrivateKey privateKey = getPrivateKeyFromPemFile(clientPrivateKeyFile);
            BouncyCastleProvider provider = new BouncyCastleProvider();
//            String keyStorePassword = "noPassword";
            //KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            KeyStore ks = KeyStore.getInstance(KEY_STORE_TYPE, provider);
            ks.load(null, null);
            ks.setCertificateEntry("certificate", clientCert);
            ks.setKeyEntry("private-key", privateKey, keyStorePassword.toCharArray(), new java.security.cert.Certificate[]{clientCert});
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword.toCharArray());
            return kmf.getKeyManagers();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PrivateKey getPrivateKeyFromPemFile(InputStream privateKeyFile)
            throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(privateKeyFile, "UTF-8"));
        //读取后续行
        br.readLine();
        String str = "";
        String temp = br.readLine();
        while (!temp.startsWith("-----")) {//读到以“-----”开头的行尾标识为止
            str += temp + "\n";
            temp = br.readLine();
        }
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] encodeByte = Base64.decode(str, Base64.CRLF);
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(encodeByte);
        PrivateKey privatekey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
        return privatekey;
    }

}
