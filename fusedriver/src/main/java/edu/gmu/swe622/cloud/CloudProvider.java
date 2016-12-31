package edu.gmu.swe622.cloud;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;

import net.fusejna.StructFuseFileInfo.FileInfoWrapper.OpenMode;
import edu.gmu.swe622.struct.CloudDirectory;
import edu.gmu.swe622.struct.CloudFile;

/**
 * Abstract class that simplifies a FUSE filesystem.
 */
public abstract class CloudProvider {

	protected final CloudProvider cp;

	/**
	 * Mandatory constructor: set up a delegate
	 * @param cp
	 */
	public CloudProvider(CloudProvider cp) {
		this.cp = cp;
	}

	public void init(){
		if(cp != null)
			cp.init();
	}

	/**
	 * Get a file. Given a path, will fill getTo with information about that file.
	 * Will only retrieve the CONTENT of the file if withContent is set to true.
	 * 
	 * @param path
	 * @param getTo
	 * @param withContent
	 * @throws IOException general error
	 * @throws FileNotFoundException file doesn't exist
	 */
	public void get(String path, CloudFile getTo, boolean withContent) throws IOException, FileNotFoundException {
		if (cp != null)
			cp.get(path, getTo, withContent);
	}

	/**
	 * Updates a file - always the metadata about the file, and the content too if requested. 
	 * @param path
	 * @param f
	 * @param withContent
	 * @throws IOException general error
	 * @throws FileNotFoundException file doesn't exist
	 */
	public void put(String path, CloudFile f, boolean withContent)
			throws IOException, FileNotFoundException {
		if (cp != null)
			cp.put(path, f, withContent);
	}

	/**
	 * Opens a file for reading or writing, as specified by mode. Open is always called before get or put.
	 * When an opens a file for writing, it should have exclusive permission to do so.
	 * Multiple apps can read the same file at once.
	 * 
	 * @param path
	 * @param data
	 * @param mode
	 * @throws IOException general error
	 * @throws FileNotFoundException file doesn't exist
	 */
	public void openFile(String path, CloudFile data, OpenMode mode) throws IOException, FileNotFoundException {
		if (cp != null)
			cp.openFile(path, data, mode);
	}

	/**
	 * Closes a file from reading/writing. Releases an app's locks on that file.
	 * @param path
	 * @param toClose
	 * @throws IOException general error
	 * @throws FileNotFoundException file doesn't exist
	 */
	public void closeFile(String path, CloudFile toClose) throws IOException, FileNotFoundException {
		if (cp != null)
			cp.closeFile(path, toClose);
	}
	
	/**
	 * Open a directory. Before an app starts listing the files in a directory, it will call openDir.
	 * Apps might still GET files in a directory without OPEN'ing it first, or PUT new files into that 
	 * directory too.
	 * 
	 * When opening the directory, the "getTo" parameter is filled with information about the contents
	 * of that directory.
	 * 
	 * @param path
	 * @param getTo
	 * @throws IOException general error
	 * @throws FileNotFoundException file doesn't exist
	 */
	public void openDir(String path, CloudDirectory getTo) throws IOException, FileNotFoundException {
		if (cp != null)
			cp.openDir(path, getTo);
	}

	/**
	 * Close a directory.
	 * 
	 * @see openDir
	 * @param path
	 * @param toClose
	 * @throws IOException general error
	 * @throws FileNotFoundException file doesn't exist
	 */
	public void closeDir(String path, CloudDirectory toClose) throws IOException, FileNotFoundException {
		if (cp != null)
			cp.closeDir(path, toClose);
	}
	
	/**
	 * Create a directory
	 * 
	 * @param path
	 * @throws IOException general error
	 * @throws FileAlreadyExistsException file doesn't exist
	 * @throws FileNotFoundException parent directories don't exist
	 */
	public void mkDir(String path) throws IOException, FileAlreadyExistsException, FileNotFoundException {
		if(cp != null)
			cp.mkDir(path);
	}
	
	/**
	 * Create a new empty file
	 * @param path
	 * @throws IOException general error
	 * @throws FileAlreadyExistsException file doesn't exist
	 * @throws FileNotFoundException parent directories don't exist
	 */
	public void mkFile(String path) throws IOException, FileAlreadyExistsException {
		if(cp != null)
			cp.mkFile(path);
	}

	/**
	 * Delete a single file
	 * 
	 * @param path
	 * @throws IOException general error
	 * @throws FileNotFoundException file doesnt exist
	 */
	public void unlink(String path) throws IOException, FileNotFoundException {
		if(cp != null)
			cp.unlink(path);
	}
	
	/**
	 * Delete an EMPTY directory.
	 * 
	 * @param path
	 * @throws IOException general error
	 * @throws FileNotFoundException directory doesnt exist
	 * @throws DirectoryNotEmptyException directory not empty
	 */
	public void rmDir(String path) throws IOException, FileNotFoundException, DirectoryNotEmptyException {
		if(cp != null)
			cp.rmDir(path);
	}
	
}
