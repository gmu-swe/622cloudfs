package edu.gmu.swe622.cloud;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.gmu.swe622.struct.CloudDirectory;
import edu.gmu.swe622.struct.CloudFile;

public class HelloWorldProvider extends CloudProvider {

	public HelloWorldProvider(CloudProvider cp) {
		super(cp);
	}

	@Override
	public void openDir(String path, CloudDirectory getTo) throws IOException,
			FileNotFoundException {
	}
	@Override
	public void get(String path, CloudFile getTo, boolean withContent)
			throws IOException, FileNotFoundException {
	}

}
