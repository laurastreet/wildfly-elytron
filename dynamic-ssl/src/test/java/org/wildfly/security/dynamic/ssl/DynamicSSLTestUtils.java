/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2024 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.wildfly.security.dynamic.ssl;

import org.junit.Assert;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.wildfly.security.ssl.test.util.CAGenerationTool;
import org.wildfly.security.x500.cert.X509CertificateExtension;

/**
 * Utility class for DynamicSSLContextTest class.
 *
 * @author <a href="mailto:dvilkola@redhat.com">Diana Krepinska (Vilkolakova)</a>
 */
public class DynamicSSLTestUtils {

    private static final String CLIENT_ALIAS = "client";
    private static final String LOCALHOST_ALIAS = "localhost";
    private static final String KEYSTORE_TYPE = "JKS";
    private static final String TLS_PROTOCOL_VERSION = "TLSv1.2";
    public static final String KEY_MANAGER_FACTORY_ALGORITHM = "SunX509";
    private static char[] PASSWORD = "Elytron".toCharArray();
    private static File KEYSTORES_DIR = new File("./target/keystores");

    private static String CLIENT1_KEYSTORE_FILENAME =  "client1.keystore.jks";
    private static String CLIENT1_TRUSTSTORE_FILENAME ="client1.truststore.jks";
    private static String SERVER1_KEYSTORE_FILENAME = "server1.keystore.jks";
    private static String SERVER1_TRUSTSTORE_FILENAME = "server1.truststore.jks";

    private static String CLIENT2_KEYSTORE_FILENAME =  "client2.keystore.jks";
    private static String CLIENT2_TRUSTSTORE_FILENAME ="client2.truststore.jks";
    private static String SERVER2_KEYSTORE_FILENAME = "server2.keystore.jks";
    private static String SERVER2_TRUSTSTORE_FILENAME = "server2.truststore.jks";

    private static String CLIENT3_KEYSTORE_FILENAME =  "client3.keystore.jks";
    private static String CLIENT3_TRUSTSTORE_FILENAME ="client3.truststore.jks";
    private static String SERVER3_KEYSTORE_FILENAME = "server3.keystore.jks";
    private static String SERVER3_TRUSTSTORE_FILENAME = "server3.truststore.jks";

    private static String DEFAULT_CLIENT_KEYSTORE_FILENAME =  "default-client.keystore.jks";
    private static String DEFAULT_CLIENT_TRUSTSTORE_FILENAME ="default-client.truststore.jks";
    private static String DEFAULT_SERVER_KEYSTORE_FILENAME = "default-server.keystore.jks";
    private static String DEFAULT_SERVER_TRUSTSTORE_FILENAME = "default-server.truststore.jks";

    static SSLContext createSSLContext(String keystorePath, String truststorePath, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(new FileInputStream(keystorePath), password.toCharArray());

            // Create key manager
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
            keyManagerFactory.init(keyStore, password.toCharArray());
            KeyManager[] km = keyManagerFactory.getKeyManagers();

            KeyStore trustStore = KeyStore.getInstance(KEYSTORE_TYPE);
            trustStore.load(new FileInputStream(truststorePath), password.toCharArray());

            // Create trust manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KEY_MANAGER_FACTORY_ALGORITHM);
            trustManagerFactory.init(trustStore);
            TrustManager[] tm = trustManagerFactory.getTrustManagers();

            // Initialize SSLContext
            SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL_VERSION);
            sslContext.init(km, tm, null);

            return sslContext;
        } catch (Exception ex) {
            Assert.fail();
        }
        return null;
    }

    static void createKeystores() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        if (!KEYSTORES_DIR.exists()) {
            KEYSTORES_DIR.mkdirs();
        }

        generateTwoWaySSLKeystoresAndTruststores(CLIENT1_KEYSTORE_FILENAME, SERVER1_KEYSTORE_FILENAME, CLIENT1_TRUSTSTORE_FILENAME, SERVER1_TRUSTSTORE_FILENAME);
        generateTwoWaySSLKeystoresAndTruststores(CLIENT2_KEYSTORE_FILENAME, SERVER2_KEYSTORE_FILENAME, CLIENT2_TRUSTSTORE_FILENAME, SERVER2_TRUSTSTORE_FILENAME);
        generateTwoWaySSLKeystoresAndTruststores(CLIENT3_KEYSTORE_FILENAME, SERVER3_KEYSTORE_FILENAME, CLIENT3_TRUSTSTORE_FILENAME, SERVER3_TRUSTSTORE_FILENAME);
        generateTwoWaySSLKeystoresAndTruststores(DEFAULT_CLIENT_KEYSTORE_FILENAME, DEFAULT_SERVER_KEYSTORE_FILENAME, DEFAULT_CLIENT_TRUSTSTORE_FILENAME, DEFAULT_SERVER_TRUSTSTORE_FILENAME);
    }

    private static void generateTwoWaySSLKeystoresAndTruststores(String clientKeystoreFilename, String serverKeystoreFilename,
                                                                 String clientTruststoreFilename, String serverTruststoreFilename) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        CAGenerationTool caGenerationTool = null;
        try {
            caGenerationTool = CAGenerationTool.builder()
                    .setBaseDir(KEYSTORES_DIR.getCanonicalPath())
                    .setRequestIdentities(CAGenerationTool.Identity.values())
                    .build();
        } catch(Exception e) {
            e.printStackTrace();
            Assert.fail();
        }

        // Generates client certificate
        X509Certificate clientCertificate = caGenerationTool.createIdentity(CLIENT_ALIAS,
                new X500Principal("OU=Elytron"),
                clientKeystoreFilename,
                CAGenerationTool.Identity.CA,
                new X509CertificateExtension[]{});

        // Generates server certificate
        X509Certificate serverCertificate = caGenerationTool.createIdentity(LOCALHOST_ALIAS,
                new X500Principal("OU=Elytron"),
                serverKeystoreFilename,
                CAGenerationTool.Identity.CA,
                new X509CertificateExtension[]{});

        // create truststores
        KeyStore clientTrustStore = KeyStore.getInstance(KEYSTORE_TYPE);
        clientTrustStore.load(null, null);

        KeyStore serverTrustStore = KeyStore.getInstance(KEYSTORE_TYPE);
        serverTrustStore.load(null, null);

        clientTrustStore.setCertificateEntry(LOCALHOST_ALIAS, serverCertificate);
        serverTrustStore.setCertificateEntry(CLIENT_ALIAS, clientCertificate);

        File clientTrustFile = new File(KEYSTORES_DIR, clientTruststoreFilename);
        try (FileOutputStream clientStream = new FileOutputStream(clientTrustFile)) {
            clientTrustStore.store(clientStream, PASSWORD);
        }

        File serverTrustFile = new File(KEYSTORES_DIR, serverTruststoreFilename);
        try (FileOutputStream serverStream = new FileOutputStream(serverTrustFile)) {
            serverTrustStore.store(serverStream, PASSWORD);
        }
    }

    public static void deleteKeystores() {
        new File(KEYSTORES_DIR, CLIENT1_KEYSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, CLIENT1_TRUSTSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, CLIENT2_KEYSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, CLIENT2_TRUSTSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, CLIENT3_KEYSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, CLIENT3_TRUSTSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, DEFAULT_CLIENT_KEYSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, DEFAULT_CLIENT_TRUSTSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, SERVER1_KEYSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, SERVER1_TRUSTSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, SERVER2_KEYSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, SERVER2_TRUSTSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, SERVER3_KEYSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, SERVER3_TRUSTSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, DEFAULT_SERVER_KEYSTORE_FILENAME).delete();
        new File(KEYSTORES_DIR, DEFAULT_SERVER_TRUSTSTORE_FILENAME).delete();
        KEYSTORES_DIR.delete();
    }
}
