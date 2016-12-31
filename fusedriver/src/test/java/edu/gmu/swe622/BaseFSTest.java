package edu.gmu.swe622;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import net.fusejna.FuseException;
import net.fusejna.FuseJna;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import edu.gmu.swe622.cloud.MemCacheProvider;
import edu.gmu.swe622.struct.internal.DropBoxProvider;

public class BaseFSTest {

	protected static Path testRoot;
	private static CloudFS fs;

	@BeforeClass
	public static void mountFS() {
		fs = new CloudFS(new MemCacheProvider(new DropBoxProvider()));
		try {
			testRoot = Files.createTempDirectory("cloudfsmnt");
			fs.mount(testRoot.toFile(), false);
			while (!fs.isMounted()) {
				// busy wait... :/
			}
			testRoot = testRoot.resolve("junit");
			if (Files.exists(testRoot)) {
				Files.walkFileTree(testRoot, new FileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir,
							BasicFileAttributes attrs) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file,
							IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir,
							IOException exc) throws IOException {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file,
							BasicFileAttributes attrs) throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}
				});
			}
			if (Files.exists(testRoot))
				Assert.fail("Error: manually clean up your dropbox by removing the junit folder from the app folder.");
			Files.createDirectory(testRoot);
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
			Assert.fail();
		} catch (FuseException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@AfterClass
	public static void unmountFS() {
		try {
			FuseJna.setUmount("umount");
			fs.unmount();
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (FuseException e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}
