package edu.gmu.swe622;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.gmu.swe622.cloud.CacheMissListener;
import edu.gmu.swe622.struct.internal.DropBoxProvider;

public class MemCacheFSTest extends BaseFSTest {

	static Path cachedPath;
	static int metaDataMiss;
	static int contentMiss;
	static int dirMiss;
	static String lastMetadataPath;
	static String lastFileContent;
	static String lastDir;

	@BeforeClass
	public static void installListener() {
		DropBoxProvider.listener = new CacheMissListener() {
			@Override
			public void missGetFileMetadata(String path) {
				lastMetadataPath = path;
				metaDataMiss++;
			}

			@Override
			public void missGetFileContent(String path) {
				lastFileContent = path;
				contentMiss++;
			}

			@Override
			public void missGetDir(String path) {
				lastDir = path;
				dirMiss++;
			}
		};
		cachedPath = testRoot.resolve("memCacheTest");
		try {
			Files.createDirectory(cachedPath);
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}

	@AfterClass
	public static void removeListener() {
		DropBoxProvider.listener = null;
	}

	int dir;
	int content;
	int metadata;
	
	private void beforeAction()
	{
		dir = dirMiss;
		content = contentMiss;
		metadata = metaDataMiss;
	}
	private void assertMadeNoCalls()
	{
		assertEquals("Expected no calls, but touched " +lastDir, dir, dirMiss);
		assertEquals("Expected no calls, but touched " +lastFileContent, content, contentMiss);
		assertEquals("Expected no calls, but touched " +lastMetadataPath, metadata, metaDataMiss);
	}
	@Test
	public void testReadFileCached() throws Exception {
		
		beforeAction();
		cachedPath.toFile().list();
		assertMadeNoCalls();
		beforeAction();
		Path p = cachedPath.resolve("cacheTest");
		FileOutputStream fos = new FileOutputStream(p.getFileName().toString());
		fos.write(0);
		fos.close();
		cachedPath.toFile().list();
		assertMadeNoCalls();
	}
}
