package edu.gmu.swe622.cloud;

public interface CacheMissListener {
	public void missGetFileMetadata(String path);
	public void missGetFileContent(String path);
	public void missGetDir(String path);
}
