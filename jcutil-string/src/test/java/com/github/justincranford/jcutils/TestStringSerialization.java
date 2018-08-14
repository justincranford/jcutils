package com.github.justincranford.jcutils;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

public class TestStringSerialization {
	private static final Logger LOGGER = Logger.getLogger(TestStringSerialization.class.getCanonicalName());

	@Test
	public void testUnknownDeserialize() throws Exception {
		final String serialized    = "MIIDuDCCAqCgAwIBAgIEWv6cPTANBgkqhkiG9w0BAQsFADBXMRMwEQYKCZImiZPyLGQBGRYDY29tMRcwFQYKCZImiZPyLGQBGRYHZW50cnVzdDEYMBYGCgmSJomT8ixkARkWCGJyZWFrZml4MQ0wCwYDVQQLEwRDQTAxMB4XDTE4MDUxODA4NTYyMloXDTI4MDUxODA5MjYyMlowVzETMBEGCgmSJomT8ixkARkWA2NvbTEXMBUGCgmSJomT8ixkARkWB2VudHJ1c3QxGDAWBgoJkiaJk/IsZAEZFghicmVha2ZpeDENMAsGA1UECxMEQ0EwMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK88hZTnK62UZ686zBp68y2ZTdDavOgXFpz46mw+XwbYIdFUNo0tvqQAqCdYIBITueKAzqDsCJZK4GNYQMdXOdwQSXIsUtY7r9yc0gIA5ery3R5rTWS9B8k0WnREWULsezAY5ZgETUDbj7U4PUx9YBdqGFhLvSyy6epUXsBfePONsjI40Bv9TU0hgRDTWCLCYwiYOQx+M3wtU4egtDfvhXFw/s4g8xx34aanv1CXq9GYw4CJhLvZWEeHUA8npkwslv+G+3kX0h3bRn4NlOg0BxQY53Au+KslqUnuKG1UGgqsIOso6EqnNye5vxlW7ltY8G4oPTskrTjF3Kr/LwNfBG8CAwEAAaOBizCBiDArBgNVHRAEJDAigA8yMDE4MDUxODA4NTYyMlqBDzIwMjgwNTE4MDkyNjIyWjALBgNVHQ8EBAMCAQYwHwYDVR0jBBgwFoAU+iURPa1UuDQ00gnkMrQRAuaALTswHQYDVR0OBBYEFPolET2tVLg0NNIJ5DK0EQLmgC07MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBADpEpScgkiq01322iohOKUbBMTecjKLuLX/BTxiWobZSRk/CLtVCCPXGSr1ZXbubHbPsEDWjegrIe5nBkMxyv4ZZ/xd8Ks2XpoSoQOWgGbQDbTIVjsdLH5X9U9O4TBKLBaNQ8DljlBejx+JDgbIUhfJO2VGVr6/TZDdVejjaOgQLCxNuFU0iOo7oQtvPLmRGOA1SODA/6fK+eeJbFcdfugj/x66R8HkZAHR9iB5oeMOx3LPFxv5FJvxRAfArLx9EuSs6+MmSEWuPZer3TRQnQRfyvVdAo2ybfv5k5tSTmtWJxS7KE48C1PgAZzNX2Gv/YqyAp0PsgOskUkPb/mCt5LU=";
		try {
			final byte[] base64Decoded = StringUtil.base64Decode(serialized);
			LOGGER.log(Level.WARNING, "Base64 decode worked");
			final String hexEncoded    = StringUtil.hexEncode(base64Decoded);
			LOGGER.log(Level.WARNING, "Decoded data as HEX " + hexEncoded);
			try {
				LOGGER.log(Level.WARNING, "Decoded data as UTF-8 " + new String(base64Decoded, StandardCharsets.UTF_8));
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Decoded data as UTF-8 did not work");
			}
			try {
				LOGGER.log(Level.WARNING, "Decoded data as UTF-16 " + new String(base64Decoded, StandardCharsets.UTF_16));
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Decoded data as UTF-16 did not work");
			}
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Base64 decode did not work");
		}
//		final Object object        = StringUtil.deserializeFromByteArray(serialized.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void testSerializeDeserializeUsingByteArray() throws Exception {
		final String inputObject         = "This is a test!";
		final byte[] serializedByteArray = StringUtil.serializeToByteArray(inputObject);
		final Object outputObject        = StringUtil.deserializeFromByteArray(serializedByteArray);
		Assert.assertTrue(outputObject != inputObject);			// assert difference references
		Assert.assertTrue(outputObject.equals(inputObject));	// assert same contents
	}

	@Test
	public void testSerializeDeserializeUsingString() throws Exception {
		final String inputObject      = "This is a test!";
		final String serializedString = StringUtil.serializeToString(inputObject);
		final Object outputObject     = StringUtil.deserializeFromString(serializedString);
		Assert.assertTrue(outputObject != inputObject);			// assert difference references
		Assert.assertTrue(outputObject.equals(inputObject));	// assert same contents
	}
}