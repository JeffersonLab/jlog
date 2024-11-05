package org.jlab.jlog.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

/**
 * Security Utilities.
 *
 * @author ryans
 */
public final class SecurityUtil {

    private static final Logger logger = Logger.getLogger(
            SecurityUtil.class.getName());
    private static final SSLSocketFactory defaultFactory =
            (SSLSocketFactory) SSLSocketFactory.getDefault();
    private static final HostnameVerifier defaultVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
    
    private SecurityUtil() {
        // Can't instantiate publicly
    }

    /**
     * Disables the server certificate check performed when using the default
     * SSLSocketFactory.
     *
     * @throws NoSuchAlgorithmException If unable to disable
     * @throws KeyManagementException If unable to disable
     */
    public static void disableServerCertificateCheck()
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLSocketFactory factory = getTrustySocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(factory);
        
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
        
    }

    /**
     * Re-enables the server certificate check performed when using the default
     * SSLSocketFactory, if it was previously disabled.
     */
    public static void enableServerCertificateCheck() {
        HttpsURLConnection.setDefaultSSLSocketFactory(defaultFactory);
        
        HttpsURLConnection.setDefaultHostnameVerifier(defaultVerifier);
    }

    /**
     * A X509TrustManager which trusts every certificate regardless of
     * attributes.
     */
    public static class TrustyTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] xcs, String string)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] xcs, String string)
                throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    /**
     * Obtains a custom SSLSocketFactory which trusts every certificate
     * regardless of attributes.
     *
     * @return A trusty SSLSocketFactory
     * @throws NoSuchAlgorithmException If unable to obtain an SSLContext
     * @throws KeyManagementException If Unable to initialize the SSLContext
     */
    public static SSLSocketFactory getTrustySocketFactory()
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");

        context.init(null, new TrustManager[]{new TrustyTrustManager()}, null);

        return context.getSocketFactory();
    }

    /**
     * Obtain a SSLSocketFactory that provides a client certificate from a PEM
     * file and optionally verifies the server's certificate.
     *
     * @param pemPath Path to the PEM file
     * @param verifyPeer true to use the default trust store to verify the
     * server, false to accept the server's certificate regardless of attributes
     * @return The SSLSocketFactory
     * @throws NoSuchAlgorithmException If unable to create the SocketFactory
     * @throws FileNotFoundException If the PEM file cannot be found
     * @throws IOException If unable to read the PEM file
     * @throws KeyStoreException If unable to create the SocketFactory
     * @throws CertificateException If unable to create the SocketFactory
     * @throws UnrecoverableKeyException If unable to create the SocketFactory
     * @throws KeyManagementException If unable to create the SocketFactory
     * @throws InvalidKeySpecException If unable to create the SocketFactory
     */
    public static SSLSocketFactory getClientCertSocketFactoryPEM(String pemPath,
            boolean verifyPeer)
            throws NoSuchAlgorithmException, FileNotFoundException, IOException,
            KeyStoreException, CertificateException, UnrecoverableKeyException,
            KeyManagementException, InvalidKeySpecException {
        SSLContext context = SSLContext.getInstance("TLS");

        byte[] certAndKey = IOUtil.fileToBytes(new File(pemPath));
        X509Certificate cert = fetchCertificateFromPEM(certAndKey);
        RSAPrivateKey key = fetchPrivateKeyFromPEM(certAndKey);

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null);
        keystore.setCertificateEntry("cert-alias", cert);
        keystore.setKeyEntry("key-alias", key, "changeit".toCharArray(),
                new Certificate[]{cert});

        logger.log(Level.FINEST, "Keystore entry count: {0}", keystore.size());
        logger.log(Level.FINEST, "Client Certificate: {0}",
                keystore.getCertificate("cert-alias"));
        //logger.log(Level.FINEST, "Private Key: {0}", keystore.getKey(
        //        "key-alias", "changeit".toCharArray()));

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, "changeit".toCharArray());

        KeyManager[] km = kmf.getKeyManagers();

        TrustManager[] tm = null;

        if (!verifyPeer) {
            tm = new TrustManager[]{new TrustyTrustManager()};
        }

        context.init(km, tm, null);

        return context.getSocketFactory();
    }

    /**
     * Get SSLContext
     *
     * @param pemPath The PEM file path
     * @param verifyPeer true to verify peer hostname
     * @return The SSLContext
     * @throws NoSuchAlgorithmException If unable to create the SocketFactory
     * @throws FileNotFoundException If the PEM file cannot be found
     * @throws IOException If unable to read the PEM file
     * @throws KeyStoreException If unable to create the SocketFactory
     * @throws CertificateException If unable to create the SocketFactory
     * @throws UnrecoverableKeyException If unable to create the SocketFactory
     * @throws KeyManagementException If unable to create the SocketFactory
     * @throws InvalidKeySpecException If unable to create the SocketFactory
     */
    public static SSLContext getContext(String pemPath,
                                        boolean verifyPeer) throws NoSuchAlgorithmException, IOException, CertificateException, InvalidKeySpecException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLSv1.2");

        byte[] certAndKey = IOUtil.fileToBytes(new File(pemPath));
        X509Certificate cert = fetchCertificateFromPEM(certAndKey);
        RSAPrivateKey key = fetchPrivateKeyFromPEM(certAndKey);

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null);
        keystore.setCertificateEntry("cert-alias", cert);
        keystore.setKeyEntry("key-alias", key, "changeit".toCharArray(),
                new Certificate[]{cert});

        logger.log(Level.FINEST, "Keystore entry count: {0}", keystore.size());
        logger.log(Level.FINEST, "Client Certificate: {0}",
                keystore.getCertificate("cert-alias"));
        //logger.log(Level.FINEST, "Private Key: {0}", keystore.getKey(
        //        "key-alias", "changeit".toCharArray()));

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, "changeit".toCharArray());

        KeyManager[] km = kmf.getKeyManagers();

        TrustManager[] tm = null;

        if (!verifyPeer) {
            tm = new TrustManager[]{new TrustyTrustManager()};
        }

        context.init(km, tm, null);

        return context;
    }

    /**
     * Obtain a SSLSocketFactory that provides a client certificate from a
     * PKCS12 file.
     *
     * @param p12Path Path to the PKCS12 (.p12) file
     * @return The SSLSocketFactory
     * @throws NoSuchAlgorithmException If unable to create the SocketFactory
     * @throws FileNotFoundException If the PEM file cannot be found
     * @throws IOException If unable to read the PEM file
     * @throws KeyStoreException If unable to create the SocketFactory
     * @throws CertificateException If unable to create the SocketFactory
     * @throws UnrecoverableKeyException If unable to create the SocketFactory
     * @throws KeyManagementException If unable to create the SocketFactory
     */
    public static SSLSocketFactory getSocketFactoryPKCS12(String p12Path)
            throws NoSuchAlgorithmException, KeyStoreException,
            FileNotFoundException, IOException, CertificateException,
            UnrecoverableKeyException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(new FileInputStream(p12Path), "changeit".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, "changeit".toCharArray());

        KeyManager[] km = kmf.getKeyManagers();
        TrustManager[] tm = null;

        context.init(km, tm, null);

        return context.getSocketFactory();
    }

    /**
     * Obtain a SSLSocketFactory that provides a client certificate from a JKS
     * file.
     *
     * @param keystorePath Path to the keystore (.jks) file
     * @return The SSLSocketFactory
     * @throws NoSuchAlgorithmException If unable to create the SocketFactory
     * @throws FileNotFoundException If the PEM file cannot be found
     * @throws IOException If unable to read the PEM file
     * @throws KeyStoreException If unable to create the SocketFactory
     * @throws CertificateException If unable to create the SocketFactory
     * @throws UnrecoverableKeyException If unable to create the SocketFactory
     * @throws KeyManagementException If unable to create the SocketFactory
     */
    public static SSLSocketFactory getSocketFactoryJKS(String keystorePath)
            throws NoSuchAlgorithmException, KeyStoreException,
            FileNotFoundException, IOException, CertificateException,
            UnrecoverableKeyException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(new FileInputStream(keystorePath),
                "changeit".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, "changeit".toCharArray());

        KeyManager[] km = kmf.getKeyManagers();
        TrustManager[] tm = null;

        context.init(km, tm, null);

        return context.getSocketFactory();
    }

    /**
     * Parse PEM encoded bytes between type delimiters and return the DER
     * encoded bytes. A PEM file may contain one or more of: a certificate,
     * private key, public key, certificate signing request, certificate signing
     * request response. The items in a PEM file each have a beginning and end
     * delimiter. Example delimiters:
     * <code>
     * -----BEGIN CERTIFICATE-----
     * -----END CERTIFICATE-----
     * </code>
     * PEM encoded bytes are base64
     * encoded. This method will convert the bytes back into non-base64 encoded
     * binary. This method may need to be called more than once; each time with
     * different delimiters, if the input bytes contain multiple items (i.e.
     * both certificate and private key).
     *
     * @param pem The bytes to parse
     * @param beginDelimiter The begin delimiter
     * @param endDelimiter The end delimiter
     * @return The DER encoded bytes
     */
    public static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter,
            String endDelimiter) {
        String data = new String(pem, StandardCharsets.UTF_8);
        String[] tokens = data.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return IOUtil.decodeBase64(tokens[0]);
    }

    /**
     * Get an X509Certificate from PEM bytes.
     * 
     * @param pem The byte array
     * @return The X509Certificate
     * @throws CertificateException If unable to obtain the certificate
     */
    public static X509Certificate fetchCertificateFromPEM(byte[] pem) throws CertificateException {
		    String data = new String(pem, StandardCharsets.UTF_8);
		    String[] tokens = data.split("-----BEGIN CERTIFICATE-----");
		    tokens = tokens[1].split( "-----END CERTIFICATE-----");
        byte[] certBytes = IOUtil.decodeBase64(tokens[0]);
        X509Certificate cert = generateX509CertificateFromDER(certBytes);
        return cert;
		}
    
    /**
     * Get an RSAPublicKey from PEM bytes.
     * 
     * @param pem The byte array
     * @return The RSAPrivateKey
     * @throws InvalidKeySpecException If unable to obtain the key
     * @throws NoSuchAlgorithmException If unable to obtain the key
     */
    public static RSAPrivateKey fetchPrivateKeyFromPEM(byte[] pem) throws InvalidKeySpecException, NoSuchAlgorithmException {
		    String data = new String(pem, StandardCharsets.UTF_8);
		    String[] tokens = data.split("-----BEGIN PRIVATE KEY-----");
		    tokens = tokens[1].split("-----END PRIVATE KEY-----");
		    byte[] keyBytes = IOUtil.decodeBase64(tokens[0]);
        RSAPrivateKey key = generateRSAPrivateKeyFromDER(keyBytes);
        return key;
    }
    
    /**
     * Generates an RSAPrivateKey from an array of DER encoded bytes.
     *
     * @param keyBytes the bytes
     * @return The RSAPrivateKey
     * @throws InvalidKeySpecException If unable to create a key from the bytes
     * @throws NoSuchAlgorithmException If RSA is not supported in this JVM
     */
    public static RSAPrivateKey generateRSAPrivateKeyFromDER(byte[] keyBytes)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

    /**
     * Generates an X509Certificate from an array of DER encoded bytes.
     *
     * @param certBytes The bytes
     * @return The X509Certificate
     * @throws CertificateException If unable to create a certificate from the
     * bytes
     */
    public static X509Certificate generateX509CertificateFromDER(
            byte[] certBytes) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(certBytes));
    }
    
    /**
     * Get the CN from the subject DN on an X509Certificate
     * 
     * @param cert The certificate
     * @return The CN string
     * @throws InvalidNameException If the name is invalid
     */
    public static String getCommonNameFromCertificate(X509Certificate cert) throws InvalidNameException {
    		String commonName = null;
        LdapName ln = new LdapName(cert.getSubjectX500Principal().getName(X500Principal.RFC2253));
        for (Rdn rdn : ln.getRdns()) {
          if (rdn.getType().equalsIgnoreCase("CN")) {
            commonName = String.valueOf(rdn.getValue());
            break;
          }
        }
        return commonName;
    }
}
