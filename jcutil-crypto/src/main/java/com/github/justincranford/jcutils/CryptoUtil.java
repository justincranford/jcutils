package com.github.justincranford.jcutils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.xml.bind.DatatypeConverter;

@SuppressWarnings("hiding") 
public class CryptoUtil {
	public static final String PROVIDER_BC           = "BC";
	public static final String PROVIDER_SUNEC        = "SunEC";
	public static final String PROVIDER_SUNRSASIGN   = "SunRsaSign";
	public static final String KEYSTORE_TYPE_JKS     = "JKS";		// Type=JKS,     Provider=SunJCE?
	public static final String KEYSTORE_TYPE_JCEKS   = "JCEKS";		// Type=JKS,     Provider=SunJCE?
	public static final String KEYPAIR_RSA           = "RSA";
	public static final String KEYPAIR_EC            = "EC";
	public static final String ALGORITHM_AES         = "AES";

//	public static final String SECP112R1 = "secp112r1", SECP112R2 = "secp112r2";
//	public static final String SECP128R1 = "secp128r1", SECP128R2 = "secp128r2";
	public static final String SECP160K1 = "secp160k1", SECP160R1 = "secp160r1", SECP160R2 = "secp160r2";	// NOSONAR Declare on a separate line.
	public static final String SECP192K1 = "secp192k1", SECP192R1 = "secp192r1";							// NOSONAR Declare on a separate line.
	public static final String SECP224K1 = "secp224k1", SECP224R1 = "secp224r1";							// NOSONAR Declare on a separate line.
	public static final String SECP256K1 = "secp256k1", SECP256R1 = "secp256r1";							// NOSONAR Declare on a separate line.
	public static final String SECP384R1 = "secp384r1";
	public static final String SECP521R1 = "secp521r1";

//	public static final String SECT113R1 = "sect113r1", SECT113R2 = "sect113r2";
//	public static final String SECT131R1 = "sect131r1", SECT131R2 = "sect131r2";
//	public static final String SECT163K1 = "sect163k1", SECT163R1 = "sect163r1", SECT163R2 = "sect163r2";
//	public static final String SECT193R1 = "sect193r1", SECT193R2 = "sect193r2";
//	public static final String SECT233K1 = "sect233k1", SECT233R1 = "sect233r1";
//	public static final String SECT239K1 = "sect239k1";
//	public static final String SECT283K1 = "sect283k1", SECT283R1 = "sect283r1";
//	public static final String SECT409K1 = "sect409k1", SECT409R1 = "sect409r1";
//	public static final String SECT571K1 = "sect571k1", SECT571R1 = "sect571r1";

	private CryptoUtil() {
		// declare private constructor to prevent instantiation of this class
	}

	public static KeyPair generateKeyPair(final SecureRandom secureRandom, final String provider, final String algorithm, final int keySize) throws NoSuchAlgorithmException, NoSuchProviderException {	// NOSONAR Refactor this method to throw at most one checked exception
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm, provider);
		keyPairGenerator.initialize(keySize, secureRandom);
		return keyPairGenerator.generateKeyPair();
	}

	public static KeyPair generateKeyPair(final SecureRandom secureRandom, final String provider, final String algorithm, final AlgorithmParameterSpec apc) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {	// NOSONAR Refactor this method to throw at most one checked exception
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm, provider);
		keyPairGenerator.initialize(apc, secureRandom);
		return keyPairGenerator.generateKeyPair();
	}

	public static SecretKey generateSecretKey(final SecureRandom secureRandom, final String provider, final String algorithm, final int keySize) throws NoSuchAlgorithmException, NoSuchProviderException {	// NOSONAR Refactor this method to throw at most one checked exception
		final KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm, provider);
		keyGenerator.init(keySize, secureRandom);
		return keyGenerator.generateKey();
	}

	public static SecretKey generateSecretKey(final SecureRandom secureRandom, final String provider, final String algorithm, final AlgorithmParameterSpec apc) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {	// NOSONAR Refactor this method to throw at most one checked exception
		final KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm, provider);
		keyGenerator.init(apc, secureRandom);
		return keyGenerator.generateKey();
	}

	public static Future<KeyPair> generateKeyPairAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final int keySize) {
		return executorService.submit(new GenerateKeyPairIntTask(secureRandom, provider, algorithm, keySize));
	}

	public static Future<KeyPair> generateKeyPairAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final AlgorithmParameterSpec apc) {
		return executorService.submit(new GenerateKeyPairSpecTask(secureRandom, provider, algorithm, apc));
	}

	public static Future<SecretKey> generateSecretKeyAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final int keySize) {
		return executorService.submit(new GenerateSecretKeyIntTask(secureRandom, provider, algorithm, keySize));
	}

	public static Future<SecretKey> generateSecretKeyAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final AlgorithmParameterSpec apc) {
		return executorService.submit(new GenerateSecretKeySpecTask(secureRandom, provider, algorithm, apc));
	}

	///////////////////////////////
	// SYMMETRIC CRYPTO FOR STRINGS
	///////////////////////////////

	public static String encrypt(final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final String clearTextString, final String characterEncoding) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {	// NOSONAR Refactor this method to throw at most one checked exception
		return DatatypeConverter.printBase64Binary(encrypt(secureRandom, provider, algorithm, secretKey, clearTextString.getBytes(characterEncoding)));
	}

	public static String decrypt(final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final String cipherTextString, final String characterEncoding) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {	// NOSONAR Refactor this method to throw at most one checked exception
		return DatatypeConverter.printBase64Binary(decrypt(secureRandom, provider, algorithm, secretKey, cipherTextString.getBytes(characterEncoding)));
	}

	public static Future<String> encryptAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final String clearTextString, final String characterEncoding) {
		return executorService.submit(new EncryptSymmetricStringTask(secureRandom, provider, algorithm, secretKey, clearTextString, characterEncoding));
	}

	public static Future<String> decryptAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final String cipherTextString, final String characterEncoding) {
		return executorService.submit(new DecryptSymmetricStringTask(secureRandom, provider, algorithm, secretKey, cipherTextString, characterEncoding));
	}

	////////////////////////////////
	// ASYMMETRIC CRYPTO FOR STRINGS
	////////////////////////////////

	public static String encrypt(final SecureRandom secureRandom, final String provider, final String algorithm, final PublicKey publicKey, final String clearTextString, final String characterEncoding) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {	// NOSONAR Refactor this method to throw at most one checked exception
		return DatatypeConverter.printBase64Binary(encrypt(secureRandom, provider, algorithm, publicKey, clearTextString.getBytes(characterEncoding)));
	}

	public static String decrypt(final SecureRandom secureRandom, final String provider, final String algorithm, final PrivateKey privateKey, final String cipherTextString, final String characterEncoding) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {	// NOSONAR Refactor this method to throw at most one checked exception
		return DatatypeConverter.printBase64Binary(decrypt(secureRandom, provider, algorithm, privateKey, cipherTextString.getBytes(characterEncoding)));
	}

	public static Future<String> encryptAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final PublicKey publicKey, final String clearTextString, final String characterEncoding) {
		return executorService.submit(new EncryptAsymmetricStringTask(secureRandom, provider, algorithm, publicKey, clearTextString, characterEncoding));
	}

	public static Future<String> decryptAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final PrivateKey privateKey, final String cipherTextString, final String characterEncoding) {
		return executorService.submit(new DecryptAsymmetricStringTask(secureRandom, provider, algorithm, privateKey, cipherTextString, characterEncoding));
	}

	/////////////////////////////////
	// SYMMETRIC CRYPTO FOR BYEARRAYS
	/////////////////////////////////

	public static byte[] encrypt(final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final byte[] clearTextByteArray) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {	// NOSONAR Refactor this method to throw at most one checked exception
		final Cipher aesCipher = Cipher.getInstance(algorithm, provider);
		aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, secureRandom);
		return aesCipher.doFinal(clearTextByteArray); 
	}

	public static byte[] decrypt(final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final byte[] cipherTextByteArray) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {	// NOSONAR Refactor this method to throw at most one checked exception
		final Cipher aesCipher = Cipher.getInstance(algorithm, provider);
		aesCipher.init(Cipher.DECRYPT_MODE, secretKey, secureRandom);
		return aesCipher.doFinal(cipherTextByteArray); 
	}

	public static Future<byte[]> encryptAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final byte[] clearTextByteArray) {
		return executorService.submit(new EncryptSymmetricByteArrayTask(secureRandom, provider, algorithm, secretKey, clearTextByteArray));
	}

	public static Future<byte[]> decryptAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final byte[] cipherTextByteArray) {
		return executorService.submit(new DecryptSymmetricByteArrayTask(secureRandom, provider, algorithm, secretKey, cipherTextByteArray));
	}

	//////////////////////////////////
	// ASYMMETRIC CRYPTO FOR BYEARRAYS
	//////////////////////////////////

	public static byte[] encrypt(final SecureRandom secureRandom, final String provider, final String algorithm, final PublicKey publicKey, final byte[] clearTextByteArray) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {	// NOSONAR Refactor this method to throw at most one checked exception
		final Cipher rsaCipher = Cipher.getInstance(algorithm, provider);
		rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey, secureRandom);
		return rsaCipher.doFinal(clearTextByteArray); 
	}

	public static byte[] decrypt(final SecureRandom secureRandom, final String provider, final String algorithm, final PrivateKey privateKey, final byte[] cipherTextByteArray) throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {	// NOSONAR Refactor this method to throw at most one checked exception
		final Cipher rsaCipher = Cipher.getInstance(algorithm, provider);
		rsaCipher.init(Cipher.DECRYPT_MODE, privateKey, secureRandom);
		return rsaCipher.doFinal(cipherTextByteArray); 
	}

	public static Future<byte[]> encryptAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final PublicKey publicKey, final byte[] clearTextByteArray) {
		return executorService.submit(new EncryptAsymmetricByteArrayTask(secureRandom, provider, algorithm, publicKey, clearTextByteArray));
	}

	public static Future<byte[]> decryptAsync(final ExecutorService executorService, final SecureRandom secureRandom, final String provider, final String algorithm, final PrivateKey privateKey, final byte[] cipherTextByteArray) {
		return executorService.submit(new DecryptAsymmetricByteArrayTask(secureRandom, provider, algorithm, privateKey, cipherTextByteArray));
	}

	///////////////////////////////
	// ASYNCHRONOUS METHOD WRAPPERS
	///////////////////////////////

	private static class GenerateKeyPairIntTask implements Callable<KeyPair> {
		private SecureRandom secureRandom;
		private String       provider;
		private String       algorithm;
		private int          keySize;
		public GenerateKeyPairIntTask(final SecureRandom secureRandom, final String provider, final String algorithm, final int keySize) {
			this.secureRandom = secureRandom;
			this.provider     = provider;
			this.algorithm    = algorithm;
			this.keySize      = keySize;
		}
		@Override
		public KeyPair call() throws Exception {
			return CryptoUtil.generateKeyPair(this.secureRandom, this.provider, this.algorithm, this.keySize);
		}
	}

	private static class GenerateKeyPairSpecTask implements Callable<KeyPair> {
		private SecureRandom           secureRandom;
		private String                 provider;
		private String                 algorithm;
		private AlgorithmParameterSpec apc;
		public GenerateKeyPairSpecTask(final SecureRandom secureRandom, final String provider, final String algorithm, final AlgorithmParameterSpec apc) {
			this.secureRandom = secureRandom;
			this.provider     = provider;
			this.algorithm    = algorithm;
			this.apc          = apc;
		}
		@Override
		public KeyPair call() throws Exception {
			return CryptoUtil.generateKeyPair(this.secureRandom, this.provider, this.algorithm, this.apc);
		}
	}

	private static class GenerateSecretKeyIntTask implements Callable<SecretKey> {
		private SecureRandom secureRandom;
		private String       provider;
		private String       algorithm;
		private int          keySize;
		public GenerateSecretKeyIntTask(final SecureRandom secureRandom, final String provider, final String algorithm, final int keySize) {
			this.secureRandom = secureRandom;
			this.provider     = provider;
			this.algorithm    = algorithm;
			this.keySize      = keySize;
		}
		@Override
		public SecretKey call() throws Exception {
			return CryptoUtil.generateSecretKey(this.secureRandom, this.provider, this.algorithm, this.keySize);
		}
	}

	private static class GenerateSecretKeySpecTask implements Callable<SecretKey> {
		private SecureRandom           secureRandom;
		private String                 provider;
		private String                 algorithm;
		private AlgorithmParameterSpec          apc;
		public GenerateSecretKeySpecTask(final SecureRandom secureRandom, final String provider, final String algorithm, final AlgorithmParameterSpec apc) {
			this.secureRandom = secureRandom;
			this.provider     = provider;
			this.algorithm    = algorithm;
			this.apc          = apc;
		}
		@Override
		public SecretKey call() throws Exception {
			return CryptoUtil.generateSecretKey(this.secureRandom, this.provider, this.algorithm, this.apc);
		}
	}

	private static class EncryptSymmetricStringTask implements Callable<String> {
		private SecureRandom secureRandom;
		private String       provider;
		private String       algorithm;
		private SecretKey    secretKey;
		private String       clearTextString;
		private String       characterEncoding;
		public EncryptSymmetricStringTask(final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final String clearTextString, final String characterEncoding) {
			this.secureRandom      = secureRandom;
			this.provider          = provider;
			this.algorithm         = algorithm;
			this.secretKey         = secretKey;
			this.clearTextString   = clearTextString;
			this.characterEncoding = characterEncoding;
		}
		@Override
		public String call() throws Exception {
			return CryptoUtil.encrypt(this.secureRandom, this.provider, this.algorithm, this.secretKey, this.clearTextString, this.characterEncoding);
		}
	}

	private static class DecryptSymmetricStringTask implements Callable<String> {
		private SecureRandom secureRandom;
		private String       provider;
		private String       algorithm;
		private SecretKey    secretKey;
		private String       cipherTextString;
		private String       characterEncoding;
		public DecryptSymmetricStringTask(final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final String cipherTextString, final String characterEncoding) {
			this.secureRandom      = secureRandom;
			this.provider          = provider;
			this.algorithm         = algorithm;
			this.secretKey         = secretKey;
			this.cipherTextString  = cipherTextString;
			this.characterEncoding = characterEncoding;
		}
		@Override
		public String call() throws Exception {
			return CryptoUtil.decrypt(this.secureRandom, this.provider, this.algorithm, this.secretKey, this.cipherTextString, this.characterEncoding);
		}
	}

	private static class EncryptAsymmetricStringTask implements Callable<String> {
		private SecureRandom secureRandom;
		private String       provider;
		private String       algorithm;
		private PublicKey    publicKey;
		private String       clearTextString;
		private String       characterEncoding;
		public EncryptAsymmetricStringTask(final SecureRandom secureRandom, final String provider, final String algorithm, final PublicKey publicKey, final String clearTextString, final String characterEncoding) {
			this.secureRandom      = secureRandom;
			this.provider          = provider;
			this.algorithm         = algorithm;
			this.publicKey         = publicKey;
			this.clearTextString   = clearTextString;
			this.characterEncoding = characterEncoding;
		}
		@Override
		public String call() throws Exception {
			return CryptoUtil.encrypt(this.secureRandom, this.provider, this.algorithm, this.publicKey, this.clearTextString, this.characterEncoding);
		}
	}

	private static class DecryptAsymmetricStringTask implements Callable<String> {
		private SecureRandom secureRandom;
		private String       provider;
		private String       algorithm;
		private PrivateKey   privateKey;
		private String       cipherTextString;
		private String       characterEncoding;
		public DecryptAsymmetricStringTask(final SecureRandom secureRandom, final String provider, final String algorithm, final PrivateKey privateKey, final String cipherTextString, final String characterEncoding) {
			this.secureRandom      = secureRandom;
			this.provider          = provider;
			this.algorithm         = algorithm;
			this.privateKey        = privateKey;
			this.cipherTextString  = cipherTextString;
			this.characterEncoding = characterEncoding;
		}
		@Override
		public String call() throws Exception {
			return CryptoUtil.decrypt(this.secureRandom, this.provider, this.algorithm, this.privateKey, this.cipherTextString, this.characterEncoding);
		}
	}

	private static class EncryptSymmetricByteArrayTask implements Callable<byte[]> {
		private SecureRandom secureRandom;
		private String       provider;
		private String       algorithm;
		private SecretKey    secretKey;
		private byte[]       clearTextByteArray;
		public EncryptSymmetricByteArrayTask(final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final byte[] clearTextByteArray) {
			this.secureRandom       = secureRandom;
			this.provider           = provider;
			this.algorithm          = algorithm;
			this.secretKey          = secretKey;
			this.clearTextByteArray = clearTextByteArray;
		}
		@Override
		public byte[] call() throws Exception {
			return CryptoUtil.encrypt(this.secureRandom, this.provider, this.algorithm, this.secretKey, this.clearTextByteArray);
		}
	}

	private static class DecryptSymmetricByteArrayTask implements Callable<byte[]> {
		private SecureRandom secureRandom;
		private String       provider;
		private String       algorithm;
		private SecretKey    secretKey;
		private byte[]       cipherTextByteArray;
		public DecryptSymmetricByteArrayTask(final SecureRandom secureRandom, final String provider, final String algorithm, final SecretKey secretKey, final byte[] cipherTextByteArray) {
			this.secureRandom        = secureRandom;
			this.provider            = provider;
			this.algorithm           = algorithm;
			this.secretKey           = secretKey;
			this.cipherTextByteArray = cipherTextByteArray;
		}
		@Override
		public byte[] call() throws Exception {
			return CryptoUtil.decrypt(this.secureRandom, this.provider, this.algorithm, this.secretKey, this.cipherTextByteArray);
		}
	}

	private static class EncryptAsymmetricByteArrayTask implements Callable<byte[]> {
		private SecureRandom secureRandom;
		private String       provider;
		private String       algorithm;
		private PublicKey    publicKey;
		private byte[]       clearTextByteArray;
		public EncryptAsymmetricByteArrayTask(final SecureRandom secureRandom, final String provider, final String algorithm, final PublicKey publicKey, final byte[] clearTextByteArray) {
			this.secureRandom       = secureRandom;
			this.provider           = provider;
			this.algorithm          = algorithm;
			this.publicKey          = publicKey;
			this.clearTextByteArray = clearTextByteArray;
		}
		@Override
		public byte[] call() throws Exception {
			return CryptoUtil.encrypt(this.secureRandom, this.provider, this.algorithm, this.publicKey, this.clearTextByteArray);
		}
	}

	private static class DecryptAsymmetricByteArrayTask implements Callable<byte[]> {
		private SecureRandom secureRandom;
		private String       provider;
		private String       algorithm;
		private PrivateKey   privateKey;
		private byte[]       cipherTextByteArray;
		public DecryptAsymmetricByteArrayTask(final SecureRandom secureRandom, final String provider, final String algorithm, final PrivateKey privateKey, final byte[] cipherTextByteArray) {
			this.secureRandom        = secureRandom;
			this.provider            = provider;
			this.algorithm           = algorithm;
			this.privateKey          = privateKey;
			this.cipherTextByteArray = cipherTextByteArray;
		}
		@Override
		public byte[] call() throws Exception {
			return CryptoUtil.decrypt(this.secureRandom, this.provider, this.algorithm, this.privateKey, this.cipherTextByteArray);
		}
	}
}