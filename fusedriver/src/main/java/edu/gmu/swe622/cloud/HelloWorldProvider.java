package edu.gmu.swe622.cloud;

import java.io.FileNotFoundException;
import java.io.IOException;

import net.fusejna.StructFuseFileInfo.FileInfoWrapper.OpenMode;
import edu.gmu.swe622.struct.CloudDirectory;
import edu.gmu.swe622.struct.CloudFile;

public class HelloWorldProvider extends CloudProvider {

	public HelloWorldProvider(CloudProvider cp) {
		super(cp);
	}

	@Override
	public void openDir(String path, CloudDirectory getTo) throws IOException,
			FileNotFoundException {
		super.openDir(path, getTo);
	}
	@Override
	public void openFile(String path, CloudFile data, OpenMode mode)
			throws IOException, FileNotFoundException {
		super.openFile(path, data, mode);
	}
}
