package com.github.justincranford.jcutils;

import java.io.File;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

import com.github.justincranford.jcutils.FileUtil;
import com.github.justincranford.jcutils.ValidationUtil;

@SuppressWarnings({"static-method","unused"})
public class TestZipUtil {
	private static final Logger LOG = Logger.getLogger(TestZipUtil.class.getName());

	@Test
	public void testPrivateConstructor() throws Exception {
		ValidationUtil.assertPrivateConstructorNoParameters(ZipUtil.class, true);
	}

	@Test
	public void testExtract() throws Exception {
		Assert.assertEquals(FileUtil.EMPTY_FILE_LIST, ZipUtil.extractFiles(new File("doesnotexist"), new File("doesnotexist"), null, null));
		Assert.assertEquals(FileUtil.EMPTY_FILE_LIST, ZipUtil.extractFiles(new File(FileUtil.getCurrentCanonicalDirectory()), new File("doesnotexist"), null, null));
	}
}