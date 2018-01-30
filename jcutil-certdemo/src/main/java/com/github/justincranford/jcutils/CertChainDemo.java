package com.github.justincranford.jcutils;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

@SuppressWarnings({"unused"})
public class CertChainDemo {
	private static final Logger LOG = Logger.getLogger(CertChainDemo.class.getName());

	public static final String   UTF8                 = "UTF-8";
	public static final String   SUNJCE               = "SunJCE";
	public static final String   SUNJSSE              = "SunJSSE";
	public static final String   SUNX509              = "SunX509";	// X509ExtendedTrustManager => SimpleValidator.java. Sun-specific and compatibility use only. KeyStore only.
	public static final String   PKIX                 = "PKIX";		// X509ExtendedTrustManager => PKIXValidator.java. Default and recommended. KeyStore or ManagerFactoryParameters.
	public static final String   KEYMANAGER           = PKIX;		// SUNX509 basic validation without logging, PKIX more rigorous with protection, logging, and AKI=>SKI chain validation.
	public static final String   TRUSTMANAGER         = PKIX;		// SUNX509 basic validation without logging, PKIX more rigorous with protection, logging, and AKI=>SKI chain validation.
	public static final String   TEMPDIR              = "target/test-generated-data/";
	public static final String   EXAMPLE              = "example";
	public static final String   COM                  = "com";
	public static final String   CLIENT               = "client";
	public static final String   HOSTNAME_LOCALHOST   = "localhost";
	public static final String   IP_127_0_0_1         = "127.0.0.1";	// NOSONAR Make this IP "127.0.0.1" address configurable.
	public static final int      PORT                 = 8080;
	public static final int      SOTIMEOUT            = 2500;
	public static final String   KSALIASCLIENT        = "client";
	public static final String   KSALIASSERVER        = "server";
	public static final String   KSPROVIDER           = SUNJCE;		// PKCS12, JCEKS, JKS, PKCS11, etc
	public static final String   KSTYPE               = "JCEKS";	// PKCS12/SUNJSSE, JCEKS/SUNJCE, JKS/SUNJCE, etc
	public static final String   TSPROVIDER           = SUNJSSE;
	public static final String   TSTYPE               = "PKCS12";	// PKCS12/SUNJSSE, JCEKS/SUNJCE, JKS/SUNJCE, etc
	public static final int      RSA_KEYLENGTH        = 2048;
	public static final String   SIGALG_SHA256WITHRSA = "sha256WithRSAEncryption";
	public static final String[] TLS_CIPHER_SUITES    = {"TLS_RSA_WITH_AES_128_CBC_SHA256"};	// NOSONAR Make this member "protected".
	public static final String   TLS_V1_2_PROTOCOL    = "TLSv1.2";
	public static final String[] TLS_PROTOCOLS        = {TLS_V1_2_PROTOCOL};	// NOSONAR Make this member "protected".

	private static final ExecutorService EXECUTOR_SERVICE = new ThreadPool().getExecutorService();

	private static final long WAIT_FOR_PROFILER_MILLIS = 0L;	// <=0 disables it (Recommended for attach: 10000L)

	private CertChainDemo() {
		// declare private constructor to prevent instantiation of this class
	}

	public static void main(final String[] args) throws Exception {
		CertChainDemo.preMain();
		ProviderUtil.addBc();
		final SecureRandom secureRandom = SecureRandom.getInstanceStrong();

		// CAs
		final Future<KeyPair> akpRootCa1        = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);
		final Future<KeyPair> akpRootCa2        = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);
		final Future<KeyPair> akpSubCa1         = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);
		final Future<KeyPair> akpSubCa2         = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);
		final Future<KeyPair> akpIssuingCa1     = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);
		final Future<KeyPair> akpIssuingCa2     = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);
		// Entities
		final Future<KeyPair> akpRootClient1    = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);
		final Future<KeyPair> akpRootServer2    = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);
		final Future<KeyPair> akpSubClient1     = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);
		final Future<KeyPair> akpSubServer2     = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);
		final Future<KeyPair> akpIssuingClient1 = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);
		final Future<KeyPair> akpIssuingServer2 = CryptoUtil.generateKeyPairAsync(EXECUTOR_SERVICE, secureRandom, CryptoUtil.PROVIDER_BC, CryptoUtil.KEYPAIR_RSA, RSA_KEYLENGTH);

		final long             nowMillis       = System.currentTimeMillis();												// Milliseconds since Jan 1, 1970 @ 12:00am UTC
		final Date             notBefore  = new Date(nowMillis - 3600000L*24L + (secureRandom.nextLong() % 3600000L));	// NOW - 1 day + secureRandomom(-1..+1 hour)
		final Date             notAfter   = new Date(nowMillis + 3600000L*24L*365L);									// NOW + 365 days
		final KeyUsage         kuCa            = new KeyUsage(KeyUsage.keyCertSign);
		final KeyUsage         kuEntity        = new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.keyAgreement);	// TLS RSA keyEncipherment, TLS EC keyAgreement
		final ExtendedKeyUsage ekuCa           = new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.anyExtendedKeyUsage});
		final ExtendedKeyUsage ekuClient       = new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth});
		final ExtendedKeyUsage ekuServer       = new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_serverAuth});
		final char[]           KSPW            = CertUtil.generateSerialNumber(secureRandom).toString().toCharArray();			// password for keystore
		final char[]           KSAPW           = KSPW;	// To Do: Add support different ksAliasPassword vs ksPassword.	// password for private key entry in keystore
		final char[]           TSPW            = CertUtil.generateSerialNumber(secureRandom).toString().toCharArray();			// password for truststore

		final HashMap<X500Name, HashSet<X509Certificate>> allCerts = new HashMap<>();	// Issuing CAs DNs not unique, they have main identity cert PLUS one or two cross-certs

		FileUtil.makeCanonicalDirectories(TEMPDIR);

		// Self-signed cert for root CA 1, only used for signing the subordinate CA 1
		final X500Name          nameRootCa1            = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.DC, "rootca1").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final KeyPair           kpRootCa1              = akpRootCa1.get();
		final X509Certificate   certRootCa1ByRootCa1   = CertChainDemo.signRootCaCert(secureRandom, notBefore, notAfter, nameRootCa1, null, kuCa, ekuCa, kpRootCa1, SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainRootCa1ByRootCa1  = CertUtil.toArray(certRootCa1ByRootCa1);
		final X509Certificate[] trustRootCa1ByRootCa1  = CertUtil.toArray(certRootCa1ByRootCa1);
		final String            ksFileRootCa1ByRootCa1 = TEMPDIR + "ksRootCa1ByRootCa1." + KSTYPE;
		final String            tsFileRootCa1ByRootCa1 = TEMPDIR + "tsRootCa1ByRootCa1." + TSTYPE;
		final KeyStore          ksRootCa1ByRootCa1     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileRootCa1ByRootCa1, KSPW, KSALIASSERVER, KSAPW, kpRootCa1.getPrivate(), chainRootCa1ByRootCa1);	// NOSONAR
		final KeyStore          tsRootCa1ByRootCa1     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileRootCa1ByRootCa1, TSPW, trustRootCa1ByRootCa1);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "ROOTCA1 BY ROOTCA1", ksFileRootCa1ByRootCa1, KSPW, KSALIASSERVER, KSAPW, tsFileRootCa1ByRootCa1, TSPW);

		// Self-signed cert for root CA 2, only used for signing the subordinate CA 2
		final X500Name          nameRootCa2            = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.DC, "rootca2").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final KeyPair           kpRootCa2              = akpRootCa2.get();
		final X509Certificate   certRootCa2ByRootCa2   = CertChainDemo.signRootCaCert(secureRandom, notBefore, notAfter, nameRootCa2, null, kuCa, ekuCa, kpRootCa2, SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainRootCa2ByRootCa2  = CertUtil.toArray(certRootCa2ByRootCa2);
		final X509Certificate[] trustRootCa2ByRootCa2  = CertUtil.toArray(certRootCa2ByRootCa2);
		final String            ksFileRootCa2ByRootCa2 = TEMPDIR + "ksRootCa2ByRootCa2." + KSTYPE;
		final String            tsFileRootCa2ByRootCa2 = TEMPDIR + "tsRootCa2ByRootCa2." + TSTYPE;
		final KeyStore          ksRootCa2ByRootCa2     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileRootCa2ByRootCa2, KSPW, KSALIASSERVER, KSAPW, kpRootCa2.getPrivate(), chainRootCa2ByRootCa2);	// NOSONAR
		final KeyStore          tsRootCa2ByRootCa2     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileRootCa2ByRootCa2, TSPW, trustRootCa2ByRootCa2);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "ROOTCA2 BY ROOTCA2", ksFileRootCa2ByRootCa2, KSPW, KSALIASSERVER, KSAPW, tsFileRootCa2ByRootCa2, TSPW);

		// Subordinate CA 1, signed by the root CA 1, used for cross-certifying issuing CA 1
		final X500Name          nameSubCa1            = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.DC, "subca1").addRDN(BCStyle.DC, "rootca1").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final KeyPair           kpSubCa1              = akpSubCa1.get();
		final X509Certificate   certSubCa1ByRootCa1   = CertChainDemo.signSubCaCert(secureRandom, notBefore, notAfter, nameSubCa1, null, kuCa, ekuCa, kpSubCa1, certRootCa1ByRootCa1, kpRootCa1.getPrivate(), SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainSubCa1ByRootCa1  = CertUtil.toArray(certSubCa1ByRootCa1, certRootCa1ByRootCa1);
		final X509Certificate[] trustSubCa1ByRootCa1  = CertUtil.toArray(trustRootCa1ByRootCa1);
		final String            ksFileSubCa1ByRootCa1 = TEMPDIR + "ksSubCa1ByRootCa1." + KSTYPE;
		final String            tsFileSubCa1ByRootCa1 = TEMPDIR + "tsSubCa1ByRootCa1." + TSTYPE;
		final KeyStore          ksSubCa1ByRootCa1     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileSubCa1ByRootCa1, KSPW, KSALIASSERVER, KSAPW, kpSubCa1.getPrivate(), chainSubCa1ByRootCa1);	// NOSONAR
		final KeyStore          tsSubCa1ByRootCa1     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileSubCa1ByRootCa1, TSPW, trustSubCa1ByRootCa1);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "SUBCA1 BY ROOTCA1", ksFileSubCa1ByRootCa1, KSPW, KSALIASSERVER, KSAPW, tsFileSubCa1ByRootCa1, TSPW);

		// Subordinate CA 2, signed by the root CA 2, used for cross-certifying issuing CA 2 and also for cross-certifying issuing CA 1 (i.e. establish trust to a different customer's issuing CA 1)
		final X500Name          nameSubCa2            = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.DC, "subca2").addRDN(BCStyle.DC, "rootca2").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final KeyPair           kpSubCa2              = akpSubCa2.get();
		final X509Certificate   certSubCa2ByRootCa2   = CertChainDemo.signSubCaCert(secureRandom, notBefore, notAfter, nameSubCa2, null, kuCa, ekuCa, kpSubCa2, certRootCa2ByRootCa2, kpRootCa2.getPrivate(), SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainSubCa2ByRootCa2  = CertUtil.toArray(certSubCa2ByRootCa2, certRootCa2ByRootCa2);
		final X509Certificate[] trustSubCa2ByRootCa2  = CertUtil.toArray(trustRootCa2ByRootCa2);
		final String            ksFileSubCa2ByRootCa2 = TEMPDIR + "ksSubCa2ByRootCa2." + KSTYPE;
		final String            tsFileSubCa2ByRootCa2 = TEMPDIR + "tsSubCa2ByRootCa2." + TSTYPE;
		final KeyStore          ksSubCa2ByRootCa2     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileSubCa2ByRootCa2, KSPW, KSALIASSERVER, KSAPW, kpSubCa2.getPrivate(), chainSubCa2ByRootCa2);	// NOSONAR
		final KeyStore          tsSubCa2ByRootCa2     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileSubCa2ByRootCa2, TSPW, trustSubCa2ByRootCa2);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "SUBCA2 BY ROOTCA2", ksFileSubCa2ByRootCa2, KSPW, KSALIASSERVER, KSAPW, tsFileSubCa2ByRootCa2, TSPW);

		// Self-signed cert for issuing CA 1, the main CA for issuing end entity identities for customer 1
		final X500Name          nameIssuingCa1               = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.DC, "issuingca1").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final KeyPair           kpIssuingCa1                 = akpIssuingCa1.get();
		final X509Certificate   certIssuingCa1ByIssuingCa1   = CertChainDemo.signRootCaCert(secureRandom, notBefore, notAfter, nameIssuingCa1, null, kuCa, ekuCa, kpIssuingCa1, SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainIssuingCa1ByIssuingCa1  = CertUtil.toArray(certIssuingCa1ByIssuingCa1);	// NOSONAR
		final X509Certificate[] trustIssuingCa1ByIssuingCa1  = CertUtil.toArray(certIssuingCa1ByIssuingCa1);	// NOSONAR
		final String            ksFileIssuingCa1ByIssuingCa1 = TEMPDIR + "ksIssuingCa1ByIssuingCa1." + KSTYPE;
		final String            tsFileIssuingCa1ByIssuingCa1 = TEMPDIR + "tsIssuingCa1ByIssuingCa1." + TSTYPE;
		final KeyStore          ksIssuingCa1ByIssuingCa1     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileIssuingCa1ByIssuingCa1, KSPW, KSALIASSERVER, KSAPW, kpIssuingCa1.getPrivate(), chainIssuingCa1ByIssuingCa1);	// NOSONAR
		final KeyStore          tsIssuingCa1ByIssuingCa1     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileIssuingCa1ByIssuingCa1, TSPW, trustIssuingCa1ByIssuingCa1);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "ISSUINGCA1 BY ISSUINGCA1", ksFileIssuingCa1ByIssuingCa1, KSPW, KSALIASSERVER, KSAPW, tsFileIssuingCa1ByIssuingCa1, TSPW);

		// Cross-certs within customer 1 domains (Reuse the subject DN & Public Key of the issuing CA 1, sign them with the private key of the sub CA 1, so trust is established within customer 1 domain)
		final X509Certificate   certIssuingCa1BySubCa1   = CertChainDemo.signCrossCaCert(secureRandom, notBefore, notAfter, nameIssuingCa1, kuCa, ekuCa, kpIssuingCa1.getPublic(), certSubCa1ByRootCa1, kpSubCa1.getPrivate(), SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainIssuingCa1BySubCa1  = CertUtil.toArray(certIssuingCa1BySubCa1, certSubCa1ByRootCa1);
		final X509Certificate[] trustIssuingCa1BySubCa1  = CertUtil.toArray(certSubCa1ByRootCa1);
		final String            ksFileIssuingCa1BySubCa1 = TEMPDIR + "ksIssuingCa1BySubCa1." + KSTYPE;
		final String            tsFileIssuingCa1BySubCa1 = TEMPDIR + "tsIssuingCa1BySubCa1." + TSTYPE;
		final KeyStore          ksIssuingCa1BySubCa1     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileIssuingCa1BySubCa1, KSPW, KSALIASSERVER, KSAPW, kpIssuingCa1.getPrivate(), chainIssuingCa1BySubCa1);	// NOSONAR
		final KeyStore          tsIssuingCa1BySubCa1     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileIssuingCa1BySubCa1, TSPW, trustIssuingCa1BySubCa1);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "ISSUINGCA1 BY SUBCA1 (CROSS)", ksFileIssuingCa1BySubCa1, KSPW, KSALIASSERVER, KSAPW, tsFileIssuingCa1BySubCa1, TSPW);

		// Self-signed cert for issuing CA 2, the main CA for issuing end entity identities for customer 2
		final X500Name          nameIssuingCa2               = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.DC, "issuingca2").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final KeyPair           kpIssuingCa2                 = akpIssuingCa2.get();
		final X509Certificate   certIssuingCa2ByIssuingCa2   = CertChainDemo.signRootCaCert(secureRandom, notBefore, notAfter, nameIssuingCa2, null, kuCa, ekuCa, kpIssuingCa2, SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainIssuingCa2ByIssuingCa2  = CertUtil.toArray(certIssuingCa2ByIssuingCa2);	// NOSONAR
		final X509Certificate[] trustIssuingCa2ByIssuingCa2  = CertUtil.toArray(certIssuingCa2ByIssuingCa2);	// NOSONAR
		final String            ksFileIssuingCa2ByIssuingCa2 = TEMPDIR + "ksIssuingCa2ByIssuingCa2." + KSTYPE;
		final String            tsFileIssuingCa2ByIssuingCa2 = TEMPDIR + "tsIssuingCa2ByIssuingCa2." + TSTYPE;
		final KeyStore          ksIssuingCa2ByIssuingCa2     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileIssuingCa2ByIssuingCa2, KSPW, KSALIASSERVER, KSAPW, kpIssuingCa2.getPrivate(), chainIssuingCa2ByIssuingCa2);	// NOSONAR
		final KeyStore          tsIssuingCa2ByIssuingCa2     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileIssuingCa2ByIssuingCa2, TSPW, trustIssuingCa2ByIssuingCa2);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "ISSUINGCA2 BY ISSUINGCA2", ksFileIssuingCa2ByIssuingCa2, KSPW, KSALIASSERVER, KSAPW, tsFileIssuingCa2ByIssuingCa2, TSPW);

		// Cross-certs within customer 2 domains (Reuse the subject DN & Public Key of the issuing CA 2, sign them with the private key of the sub CA 2, so trust is established within customer 2 domain)
		final X509Certificate   certIssuingCa2BySubCa2   = CertChainDemo.signCrossCaCert(secureRandom, notBefore, notAfter, nameIssuingCa2, kuCa, ekuCa, kpIssuingCa2.getPublic(), certSubCa2ByRootCa2, kpSubCa2.getPrivate(), SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainIssuingCa2BySubCa2  = CertUtil.toArray(certIssuingCa2BySubCa2, certSubCa2ByRootCa2);
		final X509Certificate[] trustIssuingCa2BySubCa2  = CertUtil.toArray(certSubCa2ByRootCa2);
		final String            ksFileIssuingCa2BySubCa2 = TEMPDIR + "ksIssuingCa2BySubCa2." + KSTYPE;
		final String            tsFileIssuingCa2BySubCa2 = TEMPDIR + "tsIssuingCa2BySubCa2." + TSTYPE;
		final KeyStore          ksIssuingCa2BySubCa2     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileIssuingCa2BySubCa2, KSPW, KSALIASSERVER, KSAPW, kpIssuingCa2.getPrivate(), chainIssuingCa2BySubCa2);	// NOSONAR
		final KeyStore          tsIssuingCa2BySubCa2     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileIssuingCa2BySubCa2, TSPW, trustIssuingCa2BySubCa2);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "ISSUINGCA2 BY SUBCA2 (CROSS)", ksFileIssuingCa2BySubCa2, KSPW, KSALIASSERVER, KSAPW, tsFileIssuingCa2BySubCa2, TSPW);

		// Cross-cert between customer 1 & 2 domains (Reuse the subject DN & Public Key of the issuing CA 1, sign them with the private key of the sub CA 2, so trust is established by customer 2 domain for the customer 1 domain)
		final X509Certificate   certIssuingCa1BySubCa2   = CertChainDemo.signCrossCaCert(secureRandom, notBefore, notAfter, nameIssuingCa1, kuCa, ekuCa, kpIssuingCa1.getPublic(), certSubCa2ByRootCa2, kpSubCa2.getPrivate(), SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainIssuingCa1BySubCa2  = CertUtil.toArray(certIssuingCa1BySubCa2, certSubCa2ByRootCa2);
		final X509Certificate[] trustIssuingCa1BySubCa2  = CertUtil.toArray(certSubCa2ByRootCa2);
		final String            ksFileIssuingCa1BySubCa2 = TEMPDIR + "ksIssuingCa1BySubCa2." + KSTYPE;
		final String            tsFileIssuingCa1BySubCa2 = TEMPDIR + "tsIssuingCa1BySubCa2." + TSTYPE;
		final KeyStore          ksIssuingCa1BySubCa2     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileIssuingCa1BySubCa2, KSPW, KSALIASSERVER, KSAPW, kpIssuingCa1.getPrivate(), chainIssuingCa1BySubCa2);	// NOSONAR
		final KeyStore          tsIssuingCa1BySubCa2     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileIssuingCa1BySubCa2, TSPW, trustIssuingCa1BySubCa2);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "ISSUINGCA1 BY SUBCA2 (CROSS)", ksFileIssuingCa1BySubCa2, KSPW, KSALIASSERVER, KSAPW, tsFileIssuingCa1BySubCa2, TSPW);

		// Clients & Servers
		final X500Name          nameRootClient1            = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, CLIENT).addRDN(BCStyle.DC, "rootca1").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final GeneralNames      sanRootClient1ByRootCa1    = new GeneralNames(new GeneralName(GeneralName.rfc822Name, "rootclient1@rootca1.example.com"));
		final KeyPair           kpRootClient1              = akpRootClient1.get();
		final X509Certificate   certRootClient1ByRootCa1   = CertChainDemo.signSubEntityCert(secureRandom, notBefore, notAfter, nameRootClient1, sanRootClient1ByRootCa1, kuEntity, ekuClient, kpRootClient1, certRootCa1ByRootCa1, kpRootCa1.getPrivate(), SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainRootClient1ByRootCa1  = CertUtil.toArray(certRootClient1ByRootCa1, certRootCa1ByRootCa1);
		final X509Certificate[] trustRootClient1ByRootCa1  = CertUtil.toArray(trustRootCa1ByRootCa1);
		final String            ksFileRootClient1ByRootCa1 = TEMPDIR + "ksRootClient1ByRootCa1." + KSTYPE;
		final String            tsFileRootClient1ByRootCa1 = TEMPDIR + "tsRootClient1ByRootCa1." + TSTYPE;
		final KeyStore          ksRootClient1ByRootCa1     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileRootClient1ByRootCa1, KSPW, KSALIASSERVER, KSAPW, kpRootClient1.getPrivate(), chainRootClient1ByRootCa1);	// NOSONAR
		final KeyStore          tsRootClient1ByRootCa1     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileRootClient1ByRootCa1, TSPW, trustRootClient1ByRootCa1);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "ROOTCLIENT1 BY ROOTCA1", ksFileRootClient1ByRootCa1, KSPW, KSALIASSERVER, KSAPW, tsFileRootClient1ByRootCa1, TSPW);

		final X500Name          nameRootServer2            = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, HOSTNAME_LOCALHOST).addRDN(BCStyle.DC, "rootca2").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final GeneralNames      sanRootServer2ByRootCa2    = new GeneralNames(new GeneralName[]{new GeneralName(GeneralName.rfc822Name, "rootserver2@rootca2.example.com"),new GeneralName(GeneralName.dNSName, HOSTNAME_LOCALHOST),new GeneralName(GeneralName.iPAddress, IP_127_0_0_1)});
		final KeyPair           kpRootServer2              = akpRootServer2.get();
		final X509Certificate   certRootServer2ByRootCa2   = CertChainDemo.signSubEntityCert(secureRandom, notBefore, notAfter, nameRootServer2, sanRootServer2ByRootCa2, kuEntity, ekuServer, kpRootServer2, certRootCa2ByRootCa2, kpRootCa2.getPrivate(), SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainRootServer2ByRootCa2  = CertUtil.toArray(certRootServer2ByRootCa2, certRootCa2ByRootCa2);
		final X509Certificate[] trustRootServer2ByRootCa2  = CertUtil.toArray(trustRootCa2ByRootCa2);
		final String            ksFileRootServer2ByRootCa2 = TEMPDIR + "ksRootServer2ByRootCa2." + KSTYPE;
		final String            tsFileRootServer2ByRootCa2 = TEMPDIR + "tsRootServer2ByRootCa2." + TSTYPE;
		final KeyStore          ksRootServer2ByRootCa2     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileRootServer2ByRootCa2, KSPW, KSALIASSERVER, KSAPW, kpRootServer2.getPrivate(), chainRootServer2ByRootCa2);	// NOSONAR
		final KeyStore          tsRootServer2ByRootCa2     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileRootServer2ByRootCa2, TSPW, trustRootServer2ByRootCa2);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "ROOTSERVER2 BY ROOTCA2", ksFileRootServer2ByRootCa2, KSPW, KSALIASSERVER, KSAPW, tsFileRootServer2ByRootCa2, TSPW);

		final X500Name          nameSubClient1           = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, CLIENT).addRDN(BCStyle.DC, "subca1").addRDN(BCStyle.DC, "rootca1").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final GeneralNames      sanSubClient1BySubCa1    = new GeneralNames(new GeneralName(GeneralName.rfc822Name, "subclient1@subca1.rootca1.example.com"));
		final KeyPair           kpSubClient1             = akpSubClient1.get();
		final X509Certificate   certSubClient1BySubCa1   = CertChainDemo.signSubEntityCert(secureRandom, notBefore, notAfter, nameSubClient1, sanSubClient1BySubCa1, kuEntity, ekuClient, kpSubClient1, certSubCa1ByRootCa1, kpSubCa1.getPrivate(), SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainSubClient1BySubCa1  = CertUtil.toArray(certSubClient1BySubCa1, certSubCa1ByRootCa1, certRootCa1ByRootCa1);
		final X509Certificate[] trustSubClient1BySubCa1  = CertUtil.toArray(trustRootCa1ByRootCa1);
		final String            ksFileSubClient1BySubCa1 = TEMPDIR + "ksSubClient1BySubCa1." + KSTYPE;
		final String            tsFileSubClient1BySubCa1 = TEMPDIR + "tsSubClient1BySubCa1." + TSTYPE;
		final KeyStore          ksSubClient1BySubCa1     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileSubClient1BySubCa1, KSPW, KSALIASSERVER, KSAPW, kpSubClient1.getPrivate(), chainSubClient1BySubCa1);	// NOSONAR
		final KeyStore          tsSubClient1BySubCa1     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileSubClient1BySubCa1, TSPW, trustSubClient1BySubCa1);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "SUBCLIENT1 BY SUBCA1", ksFileSubClient1BySubCa1, KSPW, KSALIASSERVER, KSAPW, tsFileSubClient1BySubCa1, TSPW);	// certRootCa1ByRootCa1 is optional if certSubCa1ByRootCa1 in truststore

		final X500Name          nameSubServer2           = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, HOSTNAME_LOCALHOST).addRDN(BCStyle.DC, "subca2").addRDN(BCStyle.DC, "rootca2").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final GeneralNames      sanSubServer2BySubCa2    = new GeneralNames(new GeneralName[]{new GeneralName(GeneralName.rfc822Name, "subserver2@subca2.rootca2.example.com"),new GeneralName(GeneralName.dNSName, HOSTNAME_LOCALHOST),new GeneralName(GeneralName.iPAddress, IP_127_0_0_1)});
		final KeyPair           kpSubServer2             = akpSubServer2.get();
		final X509Certificate   certSubServer2BySubCa2   = CertChainDemo.signSubEntityCert(secureRandom, notBefore, notAfter, nameSubServer2, sanSubServer2BySubCa2, kuEntity, ekuServer, kpSubServer2, certSubCa2ByRootCa2, kpSubCa2.getPrivate(), SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainSubServer2BySubCa2  = CertUtil.toArray(certSubServer2BySubCa2, certSubCa2ByRootCa2, certRootCa2ByRootCa2);
		final X509Certificate[] trustSubServer2BySubCa2  = CertUtil.toArray(certRootCa2ByRootCa2, certIssuingCa1ByIssuingCa1);	// vendor cert => zone 1 cert appended to zone 2 trust store
		final String            ksFileSubServer2BySubCa2 = TEMPDIR + "ksSubServer2BySubCa2." + KSTYPE;
		final String            tsFileSubServer2BySubCa2 = TEMPDIR + "tsSubServer2BySubCa2." + TSTYPE;
		final KeyStore          ksSubServer2BySubCa2     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileSubServer2BySubCa2, KSPW, KSALIASSERVER, KSAPW, kpSubServer2.getPrivate(), chainSubServer2BySubCa2);	// NOSONAR
		final KeyStore          tsSubServer2BySubCa2     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileSubServer2BySubCa2, TSPW, trustSubServer2BySubCa2);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "SUBSERVER2 BY SUBCA2", ksFileSubServer2BySubCa2, KSPW, KSALIASSERVER, KSAPW, tsFileSubServer2BySubCa2, TSPW);

		final X500Name          nameIssuingClient1               = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, CLIENT).addRDN(BCStyle.DC, "issuingca1").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final GeneralNames      sanIssuingClient1ByIssuingCa1    = new GeneralNames(new GeneralName(GeneralName.rfc822Name, "issuingclient1@issuingca1.example.com"));
		final KeyPair           kpIssuingClient1                 = akpIssuingClient1.get();
		final X509Certificate   certIssuingClient1ByIssuingCa1   = CertChainDemo.signSubEntityCert(secureRandom, notBefore, notAfter, nameIssuingClient1, sanIssuingClient1ByIssuingCa1, kuEntity, ekuClient, kpIssuingClient1, certIssuingCa1ByIssuingCa1, kpIssuingCa1.getPrivate(), SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainIssuingClient1ByIssuingCa1  = CertUtil.toArray(certIssuingClient1ByIssuingCa1, certIssuingCa1ByIssuingCa1);
		final X509Certificate[] trustIssuingClient1ByIssuingCa1  = CertUtil.toArray(trustIssuingCa1ByIssuingCa1);
		final String            ksFileIssuingClient1ByIssuingCa1 = TEMPDIR + "ksIssuingClient1ByIssuingCa1." + KSTYPE;
		final String            tsFileIssuingClient1ByIssuingCa1 = TEMPDIR + "tsIssuingClient1ByIssuingCa1." + TSTYPE;
		final KeyStore          ksIssuingClient1ByIssuingCa1     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileIssuingClient1ByIssuingCa1, KSPW, KSALIASSERVER, KSAPW, kpIssuingClient1.getPrivate(), chainIssuingClient1ByIssuingCa1);	// NOSONAR
		final KeyStore          tsIssuingClient1ByIssuingCa1     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileIssuingClient1ByIssuingCa1, TSPW, trustIssuingClient1ByIssuingCa1);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "ISSUINGCLIENT1 BY ISSUINGCA1", ksFileIssuingClient1ByIssuingCa1, KSPW, KSALIASSERVER, KSAPW, tsFileIssuingClient1ByIssuingCa1, TSPW);

		final X500Name          nameIssuingServer2               = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, HOSTNAME_LOCALHOST).addRDN(BCStyle.DC, "issuingca2").addRDN(BCStyle.DC, EXAMPLE).addRDN(BCStyle.DC, COM).build();
		final GeneralNames      sanIssuingServer2ByIssuingCa2    = new GeneralNames(new GeneralName[]{new GeneralName(GeneralName.rfc822Name, "issuingserver2@issuingca2.example.com"),new GeneralName(GeneralName.dNSName, HOSTNAME_LOCALHOST),new GeneralName(GeneralName.iPAddress, IP_127_0_0_1)});
		final KeyPair           kpIssuingServer2                 = akpIssuingServer2.get();
		final X509Certificate   certIssuingServer2ByIssuingCa2   = CertChainDemo.signSubEntityCert(secureRandom, notBefore, notAfter, nameIssuingServer2, sanIssuingServer2ByIssuingCa2, kuEntity, ekuServer, kpIssuingServer2, certIssuingCa2ByIssuingCa2, kpIssuingCa2.getPrivate(), SIGALG_SHA256WITHRSA);
		final X509Certificate[] chainIssuingServer2ByIssuingCa2  = CertUtil.toArray(certIssuingServer2ByIssuingCa2, certIssuingCa2ByIssuingCa2);
		final X509Certificate[] trustIssuingServer2ByIssuingCa2  = CertUtil.toArray(trustIssuingCa2ByIssuingCa2);
		final String            ksFileIssuingServer2ByIssuingCa2 = TEMPDIR + "ksIssuingServer2ByIssuingCa2." + KSTYPE;
		final String            tsFileIssuingServer2ByIssuingCa2 = TEMPDIR + "tsIssuingServer2ByIssuingCa2." + TSTYPE;
		final KeyStore          ksIssuingServer2ByIssuingCa2     = CertChainDemo.saveKeyStore(KSPROVIDER, KSTYPE, ksFileIssuingServer2ByIssuingCa2, KSPW, KSALIASSERVER, KSAPW, kpIssuingServer2.getPrivate(), chainIssuingServer2ByIssuingCa2);	// NOSONAR
		final KeyStore          tsIssuingServer2ByIssuingCa2     = CertChainDemo.saveTrustStore(TSPROVIDER, TSTYPE, tsFileIssuingServer2ByIssuingCa2, TSPW, trustIssuingServer2ByIssuingCa2);	// NOSONAR
		CertChainDemo.validateVerifyTrackPrint(secureRandom, allCerts, "ISSUINGSERVER2 BY ISSUINGCA2", ksFileIssuingServer2ByIssuingCa2, KSPW, KSALIASSERVER, KSAPW, tsFileIssuingServer2ByIssuingCa2, TSPW);

		// To Do: Use SunX509/PKIX X509ExtendedTrustManager methods to validate chain?
		// - checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)	// authType="RSA"
		// - checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine)	// authType="RSA"
		// - checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)	// authType="RSA"
		// - checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine)	// authType="RSA"

		CertChainDemo.doTls12ClientServerExample(secureRandom,
			tsFileRootServer2ByRootCa2,       TSPW, ksFileRootClient1ByRootCa1,       KSPW, KSALIASCLIENT, KSAPW,	// CLIENT
			tsFileRootClient1ByRootCa1,       TSPW, ksFileRootServer2ByRootCa2,       KSPW, KSALIASSERVER, KSAPW);	// SERVER
		CertChainDemo.doTls12ClientServerExample(secureRandom,
			tsFileSubServer2BySubCa2,         TSPW, ksFileSubClient1BySubCa1,         KSPW, KSALIASCLIENT, KSAPW,	// CLIENT
			tsFileSubClient1BySubCa1,         TSPW, ksFileSubServer2BySubCa2,         KSPW, KSALIASSERVER, KSAPW);	// SERVER
		CertChainDemo.doTls12ClientServerExample(secureRandom,
			tsFileIssuingServer2ByIssuingCa2, TSPW, ksFileIssuingClient1ByIssuingCa1, KSPW, KSALIASCLIENT, KSAPW,	// CLIENT
			tsFileIssuingClient1ByIssuingCa1, TSPW, ksFileIssuingServer2ByIssuingCa2, KSPW, KSALIASSERVER, KSAPW);	// SERVER

		CertChainDemo.postMain();
	}

	private static void doTls12ClientServerExample(final SecureRandom secureRandom, final String clientTsFile, final char[] clientTsPassword, final String clientKsFile, final char[] clientKsPassw, final String clientKsAlias, final char[] clientKsAliasPassword, final String serverTsFile, final char[] serverTsPassword, final String serverKsFile, final char[] serverKsPassw, final String serverKsAlias, final char[] serverKsAliasPassword) throws Exception {
		final InetAddress localhost = InetAddress.getByName(HOSTNAME_LOCALHOST);
		final Thread serverThread = new TlsServer(secureRandom, localhost, PORT, SOTIMEOUT, serverTsFile, serverTsPassword, serverKsFile, serverKsPassw, serverKsAlias, serverKsAliasPassword);
		serverThread.setName("XXX_ServerThread_XXX");
		serverThread.start();

		final Thread clientThread = new TlsClient(secureRandom, localhost, PORT, SOTIMEOUT, clientTsFile, clientTsPassword, clientKsFile, clientKsPassw, clientKsAlias, clientKsAliasPassword);
		clientThread.setName("XXX_ClientThread_XXX");
		clientThread.start();

		serverThread.join();
		clientThread.join();
	}

	private static void trackCertificate(final HashMap<X500Name, HashSet<X509Certificate>> allCerts, final X509Certificate cert) {
		final X500Name dn = X500Name.getInstance(cert.getSubjectX500Principal().getEncoded());	// Preserves RDN order, from JcaX500NameUtil.getIssuer()
		HashSet<X509Certificate> certsSubject = allCerts.get(dn);
		if (null == certsSubject) {
			certsSubject = new HashSet<>();
		}
		certsSubject.add(cert);
		allCerts.put(dn, certsSubject);
	}

	private static KeyStore saveKeyStore(final String ksProvider, final String ksType, final String ksFile, final char[] ksPassword, final String ksAlias, final char[] ksAliasPassword, final PrivateKey privateKey, final X509Certificate[] ksChain) throws Exception {
		CertUtil.saveKeyStore(ksProvider, ksType, ksFile, ksPassword, ksChain, ksAlias, privateKey, ksAliasPassword);
		try (FileOutputStream fos = new FileOutputStream(ksFile+".cmd")) {
			fos.write(("ksProvider="+ksProvider+"\n").getBytes(UTF8));
			fos.write(("ksType="+ksType+"\n").getBytes(UTF8));
			fos.write(("ksFile="+ksFile+"\n").getBytes(UTF8));
			fos.write(("ksPassword="+new String(ksPassword)+"\n").getBytes(UTF8));
			fos.write(("ksAlias="+ksAlias+"\n").getBytes(UTF8));
			fos.write(("ksAliasPassword="+new String(ksAliasPassword)+"\n").getBytes(UTF8));
			fos.write(("C:\\jdk\\8-64\\bin\\keytool -list -v -alias "+ksAlias+" -keystore "+ksFile+" -storepass "+new String(ksPassword)+" -storetype "+ksType+" -providername "+ksProvider+"\n").getBytes(UTF8));
		}
		return CertUtil.loadKeyStore(ksProvider, ksType, ksFile, ksPassword);	// Re-read from disk to force validation of the settings before we use them later on
	}

	private static KeyStore saveTrustStore(final String tsProvider, final String tsType, final String tsFile, final char[] tsPassword, final X509Certificate[] tsCerts) throws Exception {	// NOSONAR
		CertUtil.saveTrustStore(tsProvider, tsType, tsFile, tsPassword, tsCerts);
		try (FileOutputStream fos = new FileOutputStream(tsFile+".cmd")) {
			fos.write(("tsProvider="+tsProvider+"\n").getBytes(UTF8));
			fos.write(("tsType="+tsType+"\n").getBytes(UTF8));
			fos.write(("tsFile="+tsFile+"\n").getBytes(UTF8));
			fos.write(("tsPassword="+new String(tsPassword)+"\n").getBytes(UTF8));
			fos.write(("C:\\jdk\\8-64\\bin\\keytool -list -v -keystore "+tsFile+" -storepass "+new String(tsPassword)+" -storetype "+tsType+" -providername "+tsProvider+"\n").getBytes(UTF8));
		}
		return CertUtil.loadKeyStore(tsProvider, tsType, tsFile, tsPassword);	// Re-read from disk to force validation of the settings before we use them later on
	}

	private static void validateVerifyTrackPrint(final SecureRandom secureRandom, final HashMap<X500Name, HashSet<X509Certificate>> allCerts, final String description, final String ksFile, final char[] ksPassword, final String ksAlias, final char[] ksAliasPassword, final String tsFile, final char[] tsPassword) throws Exception {
		final KeyStore          subjectKeyStore     = CertUtil.loadKeyStore(CertChainDemo.KSPROVIDER, CertChainDemo.KSTYPE, ksFile, ksPassword);
		final X509Certificate[] subjectChain        = CertUtil.getChainForAlias(subjectKeyStore, ksAlias);	// order matters
		final PrivateKey        subjectPrivateKey   = (PrivateKey) subjectKeyStore.getKey(ksAlias, ksAliasPassword);
		final KeyStore          subjectTrustStore   = CertUtil.loadKeyStore(CertChainDemo.TSPROVIDER, CertChainDemo.TSTYPE, tsFile, tsPassword);
		final X509Certificate[] subjectTrustedCerts = CertUtil.getTrustedCerts(subjectTrustStore);

		final X509Certificate subjectCert = subjectChain[0];
		try {	// Verify private key matches first cert public key in the key chain
			final Signature s = Signature.getInstance("SHA256withRSA");
			s.initSign(subjectPrivateKey, secureRandom);
			s.update(TEMPDIR.getBytes(UTF8));
			final byte[] signature = s.sign();
			s.initVerify(subjectCert.getPublicKey());
			s.verify(signature);
		} catch(Exception e) {
			throw new Exception("Keystore private key does not belong with first cert in the chain", e);
		}

		LOG.log(Level.INFO, description + " cert:\n" + subjectCert.toString());	// NOSONAR Use the built-in formatting to construct this argument.
		CertChainDemo.trackCertificate(allCerts, subjectCert);
		final X509Certificate[] trustedChain = CertUtil.buildTrustedChain(subjectChain, subjectTrustedCerts); // truncate at first trusted, or append trusted, or throw exception
		CertUtil.validateTrustedChain(trustedChain);
	}

	// Create root CA certificate
	public static X509Certificate signRootCaCert(final SecureRandom secureRandom, final Date notBefore, final Date notAfter, final X500Name subjectDN, final GeneralNames subjectGeneralNames, final KeyUsage subjectKeyUsage, final ExtendedKeyUsage extendedKeyUsage, final KeyPair subjectKeyPair, final String signingAlgorithm) throws Exception {
		final boolean isSubjectCA          = true;
		final X509Certificate issuerCaCert = null;
		final PrivateKey issuerPrivateKey  = null;
		return signCert(secureRandom, notBefore, notAfter, isSubjectCA, subjectDN, subjectGeneralNames, subjectKeyUsage, extendedKeyUsage, subjectKeyPair, issuerCaCert, issuerPrivateKey, signingAlgorithm);
	}

	// Create subordinate CA certificate
	public static X509Certificate signSubCaCert(final SecureRandom secureRandom, final Date notBefore, final Date notAfter, final X500Name subjectDN, final GeneralNames subjectGeneralNames, final KeyUsage subjectKeyUsage, final ExtendedKeyUsage extendedKeyUsage, final KeyPair subjectKeyPair, final X509Certificate issuerCaCert, final PrivateKey issuerPrivateKey, final String signingAlgorithm) throws Exception {
		final boolean isSubjectCA = true;
		return signCert(secureRandom, notBefore, notAfter, isSubjectCA, subjectDN, subjectGeneralNames, subjectKeyUsage, extendedKeyUsage, subjectKeyPair, issuerCaCert, issuerPrivateKey, signingAlgorithm);
	}

	// Create cross CA certificate
	public static X509Certificate signCrossCaCert(final SecureRandom secureRandom, final Date notBefore, final Date notAfter, final X500Name subjectDN, final KeyUsage subjectKeyUsage, final ExtendedKeyUsage extendedKeyUsage, final PublicKey subjectPublicKey, final X509Certificate issuerCaCert, final PrivateKey issuerPrivateKey, final String signingAlgorithm) throws Exception {
		final boolean      isSubjectCA         = true;
		final GeneralNames subjectGeneralNames = null;
		final KeyPair      subjectKeyPair      = new KeyPair(subjectPublicKey, null);	// private key not required for subject
		return signCert(secureRandom, notBefore, notAfter, isSubjectCA, subjectDN, subjectGeneralNames, subjectKeyUsage, extendedKeyUsage, subjectKeyPair, issuerCaCert, issuerPrivateKey, signingAlgorithm);
	}

	// Create subordinate CA certificate
	public static X509Certificate signSubEntityCert(final SecureRandom secureRandom, final Date notBefore, final Date notAfter, final X500Name subjectDN, final GeneralNames subjectGeneralNames, final KeyUsage subjectKeyUsage, final ExtendedKeyUsage extendedKeyUsage, final KeyPair subjectKeyPair, final X509Certificate issuerCaCert, final PrivateKey issuerPrivateKey, final String signingAlgorithm) throws Exception {
		final boolean isSubjectCA = false;
		return signCert(secureRandom, notBefore, notAfter, isSubjectCA, subjectDN, subjectGeneralNames, subjectKeyUsage, extendedKeyUsage, subjectKeyPair, issuerCaCert, issuerPrivateKey, signingAlgorithm);
	}

	// Create root, subordinate, or cross CA certificates
	private static X509Certificate signCert(
			final SecureRandom     secureRandom,
			final Date             notBefore,
			final Date             notAfter,
			final boolean          isSubjectCA,
			final X500Name         subjectDN,
			final GeneralNames     subjectGeneralNames,
			final KeyUsage         subjectKeyUsage,
			final ExtendedKeyUsage subjectExtendedKeyUsage,
			final KeyPair          subjectKeyPair,
			final X509Certificate  issuerCaCert,
			final PrivateKey       issuerPrivateKey,
			final String           signingAlgorithm
	) throws Exception {
		ValidationUtil.assertAllNullOrAllNotNullObjects(issuerCaCert, issuerPrivateKey, "Issuer CA cert and issuer CA private key must both be null or non-null");
		ValidationUtil.assertTrue(isSubjectCA || (null != subjectGeneralNames) || ((null != subjectKeyUsage) && (null != subjectExtendedKeyUsage)), "BasicConstraints+KeyUsage+ExtendedKeyUsage act together to specify the purposes for which a certificate can be used.");

//		Take care to preserve issuer RDN order, otherwise bad X500Principal->X500Name conversion causes cryptic PKIX "chain error" in sun internal class SimpleValidator. For example:
//		- KeyStore[alias=server]
//			- Chain[0]
//				- Subject: CN=localhost, DC=rootCA, DC=example, DC=com
//				- Issuer:  DC=com, DC=example, DC=rootCA (Converted from Chain[1].Subject, RDN order reversed if X500Principal->X500Name conversion is bad)
//			- Chain[1]
//				- Subject: DC=rootCA, DC=example, DC=com (Original good value)
//				- Issuer:  DC=rootCA, DC=example, DC=com (Copied good value from Chain[1].Subject, RDN order preserved by straight X500Name->X500Name copy)
		final X500Name issuerX500Name;
		if (null == issuerCaCert) {
			issuerX500Name = subjectDN;	// Preserves RDN order 
		} else {
			issuerX500Name = X500Name.getInstance(issuerCaCert.getSubjectX500Principal().getEncoded());	// Preserves RDN order, from JcaX500NameUtil.getIssuer()
		}

		final X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(issuerX500Name, CertUtil.generateSerialNumber(secureRandom), notBefore, notAfter, subjectDN, SubjectPublicKeyInfo.getInstance(subjectKeyPair.getPublic().getEncoded()));
		final JcaContentSignerBuilder builder = new JcaContentSignerBuilder(signingAlgorithm);	// EX: "SHA256withRSA"

		// X509 V3 Extensions Doc: https://access.redhat.com/documentation/en-US/Red_Hat_Certificate_System/8.0/html/Admin_Guide/Standard_X.509_v3_Certificate_Extensions.html

		// https://access.redhat.com/documentation/en-US/Red_Hat_Certificate_System/8.0/html/Admin_Guide/Standard_X.509_v3_Certificate_Extensions.html#Standard_X.509_v3_Certificate_Extensions-basicConstraints
		if (isSubjectCA) { // isCritical=true, isCA=true, pathLenConstraint=3
			certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(3));	// RFC3280 Always required for CA certificates (i.e. root/subordinate/cross), never for non-CA certificates
		}

		// https://access.redhat.com/documentation/en-US/Red_Hat_Certificate_System/8.0/html/Admin_Guide/Standard_X.509_v3_Certificate_Extensions.html#Standard_X.509_v3_Certificate_Extensions-keyUsage
		if (null != subjectKeyUsage) {
	        certBuilder.addExtension(Extension.keyUsage, true, subjectKeyUsage);	// isCritical=true
		}

		// https://access.redhat.com/documentation/en-US/Red_Hat_Certificate_System/8.0/html/Admin_Guide/Standard_X.509_v3_Certificate_Extensions.html#Standard_X.509_v3_Certificate_Extensions-extKeyUsage
		if (null != subjectExtendedKeyUsage) {
	        certBuilder.addExtension(Extension.extendedKeyUsage, false, subjectExtendedKeyUsage);	// isCritical=false
		}

		// https://access.redhat.com/documentation/en-US/Red_Hat_Certificate_System/8.0/html/Admin_Guide/Standard_X.509_v3_Certificate_Extensions.html#Standard_X.509_v3_Certificate_Extensions-subjectAltName
		if (null != subjectGeneralNames) {
	        certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectGeneralNames);	// isCritical=false
		}

		// https://access.redhat.com/documentation/en-US/Red_Hat_Certificate_System/8.0/html/Admin_Guide/Standard_X.509_v3_Certificate_Extensions.html#Standard_X.509_v3_Certificate_Extensions-subjectKeyIdentifier
		{	// NOSONAR Optional but recommended uniqueness for certs with same DN (especially CA) (ex: Different CA certs for different key usages, overlapping old & new validity periods).
			final JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
			final SubjectKeyIdentifier subjectKeyIdentifier = extensionUtils.createSubjectKeyIdentifier(subjectKeyPair.getPublic());
	        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);	// isCritical=false

			// https://access.redhat.com/documentation/en-US/Red_Hat_Certificate_System/8.0/html/Admin_Guide/Standard_X.509_v3_Certificate_Extensions.html#Standard_X.509_v3_Certificate_Extensions-The_authorityKeyIdentifier
			if (null == issuerCaCert) {
				final byte[] issuerSubjectKeyIdentifier = subjectKeyIdentifier.getKeyIdentifier();	// copy from subjectKeyIdentifier above
		        certBuilder.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(issuerSubjectKeyIdentifier));	// isCritical=false
// To Do: Fix this. It breaks PKIX X509ExtendedTrustManager AKI=>SKI path validation in sun.security.validator.PKIXValidator, specifically sun.security.provider.certpath.PKIXCertPathValidator.
//			} else {
//				final byte[] issuerSubjectKeyIdentifier = issuerCaCert.getExtensionValue(Extension.subjectKeyIdentifier.getId());	// copy from issuer cert SKI (if exists)
//				if (null != issuerSubjectKeyIdentifier) {
//			        certBuilder.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifier(issuerSubjectKeyIdentifier));	// isCritical=false
//				}
			}
		}
		
		final ContentSigner signer;
		if (null == issuerPrivateKey) {	// self-signed root CA
			signer = builder.build(subjectKeyPair.getPrivate());
		} else {
			signer = builder.build(issuerPrivateKey);
		}
		final byte[] certBytes = certBuilder.build(signer).getEncoded();
		final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		final X509Certificate subjectCert;
		try (final ByteArrayInputStream inStream = new ByteArrayInputStream(certBytes)) {
			subjectCert = (X509Certificate)certificateFactory.generateCertificate(inStream);		
		}

		return subjectCert;
	}

	/*package*/ static void preMain() throws InterruptedException {
		if (CertChainDemo.WAIT_FOR_PROFILER_MILLIS > 0) {
			LOG.log(Level.WARNING, "Waiting");
			Thread.sleep(WAIT_FOR_PROFILER_MILLIS);
		}
		Timer.setAutoLogInterval(0);	// prevent automatic logging at the close of each internal, so we can print all at the end
		Timer.setLogTotalTimeUnit(TimeUnit.SECONDS);
		Timer.setLogAverageTimeUnit(TimeUnit.MILLISECONDS);
		Timer.start("main");
	}

	/*package*/ static void postMain() throws InterruptedException {
		Timer.stop("main");
		Timer.logAllOrderedByStartTime(true);
		Timer.logAllOrderedByTotalTime(false);

		if (CertChainDemo.WAIT_FOR_PROFILER_MILLIS > 0) {
			LOG.log(Level.WARNING, "Take snapshot now");
			Thread.sleep(WAIT_FOR_PROFILER_MILLIS);
		}
	}

//	// Delegator class
//	private static class X509KeyManagerAppendTrustManager implements X509KeyManager {
//		@SuppressWarnings("hiding")
//		private static final Logger LOG = Logger.getLogger(X509KeyManagerAppendTrustManager.class.getName());
//		private final X509KeyManager   keyManager;
//		private final X509TrustManager trustManager;
//		private final List<String>     mandatoryEKUs;
//
//		public X509KeyManagerAppendTrustManager(final X509KeyManager keyManager, final X509TrustManager trustManager, final List<String> mandatoryEKUs) throws Exception {
//			ValidationUtil.assertNonNullObject(keyManager, "Key Manager must be not null.");
//			ValidationUtil.assertNonNullObject(trustManager, "trust Manager must be not null.");
//			ValidationUtil.assertNonNullElements(mandatoryEKUs, "Mandatory EKUs list can be null or empty, but it must not contain null elements");
//			this.keyManager = keyManager;
//			this.trustManager = trustManager;
//			this.mandatoryEKUs = (null==mandatoryEKUs) ? new ArrayList<>(0) : new ArrayList<>(mandatoryEKUs);
//		}
//
//		@Override
//		// Find primary certificate chain. For CA certs in primary chain, look for cross-certs. Any cross-certs and their chains will be appended to the primary chain.
//		public java.security.cert.X509Certificate[] getCertificateChain(final String alias) {
//			java.security.cert.X509Certificate[] primaryCertificateChain = this.keyManager.getCertificateChain(alias);
//			if (null == primaryCertificateChain) {
//				LOG.log(Level.WARNING, "Certificate chain for alias " + alias + " not found.");
//				return null;	// NOSONAR Return an empty array instead of null.
//			}
//
//			try {
//				CertChainDemo.assertMandatoryEKUs(primaryCertificateChain, this.mandatoryEKUs);
//			} catch (CertificateException ce) {
//				LOG.log(Level.WARNING, "Exception: ", ce);
//				return null;
//			}
//
//			final java.security.cert.X509Certificate[] acceptedIssuers = this.trustManager.getAcceptedIssuers();
//
//			// Copy primary chain certificates to a list of required searches for cross-certificates
//			final LinkedHashSet<java.security.cert.X509Certificate> constructedCertificateChain = new LinkedHashSet<>();	// unique set, preserves insertion order
//			final LinkedHashSet<Principal>                          searchIssuerDNs             = new LinkedHashSet<>();	// unique set, preserves insertion order
//			for (final java.security.cert.X509Certificate primaryCertificate : primaryCertificateChain) {
//				constructedCertificateChain.add(primaryCertificate);
//				if (-1 != primaryCertificate.getBasicConstraints()) {
//					searchIssuerDNs.add(primaryCertificate.getIssuerDN());	// add isserDN to search list, objects are X500Principal which delegate X500Name objects
//				}
//			}
//
//			// For CA certificates in primary key chain, find trusted certs which match subjectDN, and append their secondary chains to the primary chain.
//			while (!searchIssuerDNs.isEmpty()) {
//				final Principal nextSearchIssuerDN = searchIssuerDNs.iterator().next();	// Assume CA Certificate  (i.e. Root || Sub || cross-certified).
//				for (final java.security.cert.X509Certificate secondaryCertificate : acceptedIssuers) {	// Search all acceptedIssuers for one for more of this subjectDN
//					final Principal trustedIssuerSubjectDN = secondaryCertificate.getSubjectDN();
//					if (!nextSearchIssuerDN.equals(trustedIssuerSubjectDN)) {	// X500Principal.equals() delegates to X500Name.equals().
//						continue;	// issuer certificate's subjectDN does not match the issuerDN we are looking for, ignore it
//					}
//					boolean isTrustedIssuerAlreadyInConstructedChain = false;
//					for (final java.security.cert.X509Certificate constructedCertificate : constructedCertificateChain) {
//						if (secondaryCertificate.equals(constructedCertificate)) {	// X509Certificate.equals() compares X509CertImpl.getEncodedInternal() values. 
//							isTrustedIssuerAlreadyInConstructedChain = true;
//							break;
//						}
//					}
//					if (!isTrustedIssuerAlreadyInConstructedChain) {	// not already in the constructed chain, add certificate to chain and its issuer to search list 
//						constructedCertificateChain.add(secondaryCertificate);
//						searchIssuerDNs.add(secondaryCertificate.getIssuerDN());
//					}
//				}
//			}
//
//			return constructedCertificateChain.toArray(new java.security.cert.X509Certificate[constructedCertificateChain.size()]);
//		}
//
//		// SIMPLE DELEGATION OF KEYMANAGER METHOD INVOCATIONS
//
//		@Override
//		public PrivateKey getPrivateKey(final String alias) {
//			return this.keyManager.getPrivateKey(alias);
//		}
//
//		@Override
//		public String chooseClientAlias(final String[] keyType, final Principal[] issuers, final Socket socket) {
//			return this.keyManager.chooseClientAlias(keyType, issuers, socket);
//		}
//
//		@Override
//		public String chooseServerAlias(final String keyType, final Principal[] issuers, final Socket socket) {
//			return this.keyManager.chooseServerAlias(keyType, issuers, socket);
//		}
//
//		@Override
//		public String[] getClientAliases(final String keyType, final Principal[] issuers) {
//			return this.keyManager.getClientAliases(keyType, issuers);
//		}
//
//		@Override
//		public String[] getServerAliases(final String keyType, final Principal[] issuers) {
//			return this.keyManager.getServerAliases(keyType, issuers);
//		}
//	}

//	private static class X509TrustManagerAssertEKUs implements X509TrustManager {
//		private final X509TrustManager trustManager;
//		private final List<String>     mandatoryEKUs;
//
//		public X509TrustManagerAssertEKUs(final X509TrustManager trustManager, final List<String> mandatoryEKUs) throws Exception {
////			ValidationUtil.assertNonEmpty(mandatoryEKUs, "EKU list must not be empty.");
////			ValidationUtil.assertNonNullCollectionAndElements(mandatoryEKUs, "EKU list elements must be non-null.");
//			ValidationUtil.assertNonNullElements(mandatoryEKUs, "EKU list elements must be non-null.");
//			this.trustManager  = trustManager;
//			this.mandatoryEKUs = (null==mandatoryEKUs) ? new ArrayList<>(0) : new ArrayList<>(mandatoryEKUs);
//		}
//
//		@Override
//		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//			return this.trustManager.getAcceptedIssuers();
//		}
//
//		@Override
//		public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
//			CertChainDemo.assertMandatoryEKUs(chain, this.mandatoryEKUs);
//			this.trustManager.checkClientTrusted(chain, authType);
//		}
//
//		@Override
//		public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
//			CertChainDemo.assertMandatoryEKUs(chain, this.mandatoryEKUs);
//			this.trustManager.checkServerTrusted(chain, authType);
//		}
//
//		// These Extra X509ExtendedTrustManager methods are only available in Java 1.7+
//
////		@Override
////		public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
////			KeyManagerDemo.assertMandatoryEKUs(chain, this.mandatoryEKUs);
////			this.trustManager.checkClientTrusted(chain, authType, socket);
////		}
////
////		@Override
////		public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType, SSLEngine sslEngine) throws CertificateException {
////			KeyManagerDemo.assertMandatoryEKUs(chain, this.mandatoryEKUs);
////			this.trustManager.checkClientTrusted(chain, authType, sslEngine);
////		}
////
////		@Override
////		public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
////			KeyManagerDemo.assertMandatoryEKUs(chain, this.mandatoryEKUs);
////			this.trustManager.checkServerTrusted(chain, authType, socket);
////		}
////
////		@Override
////		public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType, SSLEngine sslEngine) throws CertificateException {
////			KeyManagerDemo.assertMandatoryEKUs(chain, this.mandatoryEKUs);
////			this.trustManager.checkServerTrusted(chain, authType, sslEngine);
////		}
//	}
//
//	private static void assertMandatoryEKUs(java.security.cert.X509Certificate[] chain, List<String> mandatoryEKUs) throws CertificateParsingException, CertificateException {
//		if ((null == mandatoryEKUs) || (mandatoryEKUs.isEmpty())) {
//			return;
//		}
//		for (java.security.cert.X509Certificate certificate : chain) {	// Verify all certificates in chain have all mandatory EKUs
//			final List<String> certificateEKUs = certificate.getExtendedKeyUsage();
//			if (null == certificateEKUs) {
//				throw new CertificateException("Mandatory EKUs extension is missing.");
//			} else if (certificateEKUs.isEmpty()) {
//				throw new CertificateException("Mandatory EKUs extension is empty.");
//			}
//			for (final String mandatoryEKU : mandatoryEKUs) {
//				if (!certificateEKUs.contains(mandatoryEKU)) {
//					throw new CertificateException("Mandatory EKU '" + mandatoryEKU + "' is missing from subjectDN=" + certificate.getSubjectDN() + ", issuerDN=" + certificate.getIssuerDN() + ", serialNumber=" + certificate.getSerialNumber() + ".");
//				}
//			}
//		}
//	}
//
//	private static class CompositeTrustManager extends X509ExtendedTrustManager {
//		private X509TrustManager trustManagers;
//
//		// Custom constructor
//		public CompositeTrustManager(final TrustManagerFactory trustManagerFactory) throws Exception {
//			for (final TrustManager candidateTrustManager : trustManagerFactory.getTrustManagers()) {
//				if (candidateTrustManager instanceof X509TrustManager) {
//					this.trustManagers = (X509TrustManager) candidateTrustManager;	// find first TrustManager of type X509TrustManager in the list
//					break;
//				}
//			}
//			if (null == this.trustManagers) {
//				throw new Exception("TrustManagerFactory parameter did not return any TrustManager instance of type X509TrustManager");
//			}
//		}
//
//		@Override
//		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
//			return this.trustManagers.getAcceptedIssuers();
//		}
//
//		@Override
//		public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws CertificateException {
//			this.checkClientTrusted(arg0, arg1);
//		}
//
//		@Override
//		public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws CertificateException {
//			this.checkServerTrusted(arg0, arg1);
//		}
//
//		@Override
//		public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
//			this.checkClientTrusted(chain, authType, socket);
//		}
//
//		@Override
//		public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
//			this.checkClientTrusted(chain, authType, engine);
//		}
//
//		@Override
//		public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
//			this.checkServerTrusted(chain, authType, socket);
//		}
//
//		@Override
//		public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
//			this.checkServerTrusted(chain, authType, engine);
//		}
//
////		@Override
////		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
////			for (X509TrustManager trustManager : trustManagers) {
////				try {
////					trustManager.checkClientTrusted(chain, authType);
////					return; // someone trusts them. success!
////				} catch (CertificateException e) {
////					// maybe someone else will trust them
////				}
////			}
////			throw new CertificateException("None of the TrustManagers trust this certificate chain");
////		}
////
////		@Override
////		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
////			for (X509TrustManager trustManager : trustManagers) {
////				try {
////					trustManager.checkServerTrusted(chain, authType);
////					return; // someone trusts them. success!
////				} catch (CertificateException e) {
////					// maybe someone else will trust them
////				}
////			}
////			throw new CertificateException("None of the TrustManagers trust this certificate chain");
////		}
////
////
////		@Override
////		public X509Certificate[] getAcceptedIssuers() {
////			for (X509TrustManager trustManager : trustManagers) {
////				certificates.add(trustManager.getAcceptedIssuers());
////			}
////			return Iterables.toArray(certificates.build(), X509Certificate.class);
////		}
//	}
}