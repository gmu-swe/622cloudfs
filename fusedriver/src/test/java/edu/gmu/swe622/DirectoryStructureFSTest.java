package edu.gmu.swe622;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class DirectoryStructureFSTest extends BaseFSTest {

	@Test
	public void testMkdir() throws Exception {
		Path d = Files.createDirectory(testRoot.resolve("tmpDir"));
		// Try again...
		boolean caught = false;
		try {
			Files.createDirectory(testRoot.resolve("tmpDir"));
		} catch (FileAlreadyExistsException ex) {
			caught = true;
		}
		assertTrue("Unable to create dir twice", caught);
		caught = false;
		for (String s : testRoot.toFile().list()) {
			if (s.equals("tmpDir"))
				caught = true;
		}
		assertTrue("dir is a dir", Files.isDirectory(d));
		Files.delete(d);
		assertFalse("Dir is gone", Files.exists(d));
	}

	@Test
	public void testTouchFile() throws Exception {
		Path d = Files.createFile(testRoot.resolve("tmpFile"));
		FileOutputStream fos = new FileOutputStream(d.toFile());
		DataOutputStream dos = new DataOutputStream(fos);
		for (int i = 0; i < 20 * 2; i++)
			dos.writeInt(i);
		fos.close();

		FileInputStream fis = new FileInputStream(d.toFile());
		DataInputStream dis = new DataInputStream(fis);
		for (int i = 0; i < 20 * 2; i++) {
			assertEquals(i, dis.readInt());
		}
		boolean eof = false;
		try {
			eof = dis.readInt() == -1;
		} catch (EOFException ex) {
			eof = true;
		}
		assertTrue("Hit EOF", eof);
		fis.close();
		// Files.delete(d);
		// assertFalse("File is gone", Files.exists(d));
	}
}
