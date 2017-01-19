package edu.gmu.swe622;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;

import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.FuseException;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper;
import net.fusejna.StructFuseFileInfo.FileInfoWrapper.OpenMode;
import net.fusejna.StructStat.StatWrapper;
import net.fusejna.StructStatvfs.StatvfsWrapper;
import net.fusejna.types.TypeMode.ModeWrapper;
import net.fusejna.types.TypeMode.NodeType;
import net.fusejna.util.FuseFilesystemAdapterAssumeImplemented;
import edu.gmu.swe622.cloud.CloudProvider;
import edu.gmu.swe622.cloud.HelloWorldProvider;
import edu.gmu.swe622.cloud.MemCacheProvider;
import edu.gmu.swe622.struct.CloudDirectory;
import edu.gmu.swe622.struct.CloudFile;
import edu.gmu.swe622.struct.CloudPath;
import edu.gmu.swe622.struct.internal.DropBoxProvider;
import edu.gmu.swe622.struct.internal.ObjectFactory;
import edu.gmu.swe622.struct.internal.Pool;

/**
 * A bridge between the FUSE driver and our abstraction. Handles file pointers,
 * etc.
 *
 */
public class CloudFS extends FuseFilesystemAdapterAssumeImplemented {
	protected Pool<CloudFile> fileHandlePool;
	protected Pool<CloudDirectory> dirPool;
	private CloudProvider provider;

	public CloudFS(CloudProvider provider) {
		this.provider = provider;
		fileHandlePool = new Pool<CloudFile>(new ObjectFactory<CloudFile>() {
			@Override
			public CloudFile newInst(int pos) {
				return new CloudFile(pos);
			}
		});
		dirPool = new Pool<CloudDirectory>(new ObjectFactory<CloudDirectory>() {
			@Override
			public CloudDirectory newInst(int pos) {
				return new CloudDirectory(null, pos);
			}
		});
	}

	@Override
	public void init() {
		super.init();
		provider.init();
	}

	public static void main(String[] args) throws FuseException {
		if (args.length != 1 && args.length != 2) {
			System.err.println("Usage: CloudFS <mountpoint> {hello}");
			System.exit(1);
		}
		CloudProvider root = null;
		if(args.length == 2 && "hello".equals(args[1]))
			root = new HelloWorldProvider(null);
		else
			root = new DropBoxProvider();
		
		CloudFS fs = new CloudFS(new MemCacheProvider(root));

		fs.mount(args[0]);
	}

	@Override
	public int access(String path, int access) {
		try {
			if (path.equals("/"))
				return 0;
			CloudFile f = new CloudFile(-1);
			provider.get(path, f, false);
			return 0;
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			return -ErrorCodes.ENOENT();
		} catch (IOException ex) {
			ex.printStackTrace();
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int getattr(final String path, final StatWrapper stat) {
		if (isIgnoredFile(path)) {
			return -ErrorCodes.ENOENT();
		}
		if (path.equals(File.separator)) { // Root directory
			stat.setMode(NodeType.DIRECTORY);
			return 0;
		} else {
			try {
				CloudFile f = new CloudFile(0);
				provider.get(path, f, false);
				f.accept(stat);
				return 0;
			} catch (FileNotFoundException ex) {
				return -ErrorCodes.ENOENT();
			} catch (IOException ex) {
				return -ErrorCodes.EIO();
			}
		}
	}

	@Override
	public int opendir(String path, FileInfoWrapper info) {
		CloudDirectory f = dirPool.getInst();
		try {
			provider.openDir(path, f);
			info.fh(f.getPoolIdx());
			return 0;
		} catch (FileNotFoundException ex) {
			return -ErrorCodes.ENOENT();
		} catch (IOException ex) {
			ex.printStackTrace();
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int readdir(final String path, final DirectoryFiller filler,
			FileInfoWrapper info) {
		if (info.fh() > dirPool.size())
			return -ErrorCodes.EBADF();
		CloudDirectory f = dirPool.get((int) info.fh());
		if (f == null)
			return -ErrorCodes.EBADF();
		for (CloudPath file : f.listFiles())
			filler.add(file.getName());
		return 0;
	}

	@Override
	public int releasedir(String path, FileInfoWrapper info) {
		if (info.fh() > dirPool.size())
			return -ErrorCodes.EBADF();
		CloudDirectory f = dirPool.get((int) info.fh());
		if (f == null)
			return -ErrorCodes.EBADF();
		try {
			dirPool.release(f);
			provider.closeDir(path, f);
			return 0;
		} catch (IOException e) {
			e.printStackTrace();
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int open(String path, FileInfoWrapper info) {
		if (isIgnoredFile(path)) {
			return -ErrorCodes.ENOENT();
		}
		CloudFile f = fileHandlePool.getInst();
		try {
			provider.openFile(path, f, info.openMode());
			provider.get(path, f, false);
			info.fh(f.getPoolIdx());
			return 0;
		} catch (FileNotFoundException ex) {
			return -ErrorCodes.ENOENT();
		} catch (IOException ex) {
			ex.printStackTrace();
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int rmdir(String path) {
		try {
			provider.rmDir(path);
			return 0;
		} catch (FileNotFoundException ex) {
			return -ErrorCodes.ENOENT();
		} catch (DirectoryNotEmptyException ex) {
			ex.printStackTrace();
			return -ErrorCodes.ENOTEMPTY();
		} catch (IOException ex) {
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int unlink(String path) {
		try {
			provider.unlink(path);
			return 0;
		} catch (FileNotFoundException ex) {
			return -ErrorCodes.ENOENT();
		} catch (IOException ex) {
			ex.printStackTrace();
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int release(String path, FileInfoWrapper info) {
		if (info.fh() > fileHandlePool.size())
			return -ErrorCodes.EBADF();
		CloudFile f = fileHandlePool.get((int) info.fh());
		if (f == null)
			return -ErrorCodes.EBADF();
		try {
			if (f.isDirty()) {
				provider.put(path, f, true);
			}
			fileHandlePool.release(f);
			provider.closeFile(path, f);
			return 0;
		} catch (IOException e) {
			e.printStackTrace();
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int read(final String path, final ByteBuffer buffer,
			final long size, final long offset, final FileInfoWrapper info) {
		if (info.fh() > fileHandlePool.size())
			return -ErrorCodes.EBADF();
		CloudFile f = fileHandlePool.get((int) info.fh());
		if (f == null)
			return -ErrorCodes.EBADF();
		try {
			synchronized (f) {
				if (f.getContents() == null) {
					provider.get(path, f, true);
					f.getContents().position(0);
				}
				if (f.getContents().limit() < offset)
					return 0;
				ByteBuffer src = f.getContents().duplicate();
				src.position((int) offset);
				int limit = (int) (offset + size);
				int ret = (int) size;
				if (limit > src.limit()) {
					limit = src.limit();
					ret = src.limit() - (int) offset;
				}
				src.limit(limit);
				buffer.put(src);
				return ret;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int statfs(String path, StatvfsWrapper wrapper) {
		wrapper.bsize(4096);
		wrapper.blocks(4096 * 1000);
		wrapper.bavail(4096 * 1000);
		wrapper.bfree(4096 * 1000);
		return 0;
	}

	@Override
	public int mkdir(String path, ModeWrapper mode) {
		try {
			provider.mkDir(path);
			return 0;
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
			return -ErrorCodes.ENOENT();
		} catch (FileAlreadyExistsException ex) {
			ex.printStackTrace();
			return -ErrorCodes.EEXIST();
		} catch (IOException ex) {
			ex.printStackTrace();
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int create(String path, ModeWrapper mode, FileInfoWrapper info) {
		if (isIgnoredFile(path)) {
			return -ErrorCodes.ENOENT();
		}
		CloudFile f = fileHandlePool.getInst();
		try {
			provider.mkFile(path);
			info.fh(f.getPoolIdx());
			return 0;
		} catch (FileNotFoundException ex) {
			return -ErrorCodes.ENOENT();
		} catch (IOException ex) {
			ex.printStackTrace();
			return -ErrorCodes.EIO();
		}
	}

	@Override
	public int write(String path, ByteBuffer buf, long bufSize,
			long writeOffset, FileInfoWrapper info) {
		if (info.fh() > fileHandlePool.size())
			return -ErrorCodes.EBADF();
		CloudFile f = fileHandlePool.get((int) info.fh());
		if (f == null)
			return -ErrorCodes.EBADF();
		try {
			synchronized (f) {
				if (f.getContents() == null) {
					provider.get(path, f, true);
					f.getContents().position(0);
				}
				f.write();
				if (f.getContents().limit() < writeOffset + bufSize) {
					// Expand buffer
					ByteBuffer exp = ByteBuffer.allocate(f.getContents()
							.limit() + (int) bufSize);
					f.getContents().position(0);
					exp.put(f.getContents());
					f.setContents(exp);
				}
				f.getContents().position((int) writeOffset);
				if (buf.limit() > bufSize)
					buf.limit((int) bufSize);
				f.getContents().put(buf);
				if (writeOffset + bufSize > f.getSize())
					f.setSize(writeOffset + bufSize);
				return (int) bufSize;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return -ErrorCodes.EIO();
		}
	}

	private static boolean isIgnoredFile(String path) {
		if (path.endsWith(".DS_Store"))
			return true;
		return false;
	}

	@Override
	public int ftruncate(String path, long size, FileInfoWrapper info) {
		if (info.fh() > fileHandlePool.size())
			return -ErrorCodes.EBADF();
		CloudFile f = fileHandlePool.get((int) info.fh());
		if (f == null)
			return -ErrorCodes.EBADF();
		f.setContents(ByteBuffer.wrap(new byte[0]));
		f.setSize(0);
		return 0;
	}

	@Override
	public int truncate(String path, long size) {
		CloudFile f = new CloudFile(0);
		if (size == 0) {
			// Make sure file is 0-size
			try {
				provider.get(path, f, false);
				if (f.getSize() > 0) {
					// Need to update the file size.
					f.setContents(ByteBuffer.wrap(new byte[0]));
					provider.put(path, f, true);
				}
				return 0;
			} catch (FileNotFoundException ex) {
				return -ErrorCodes.ENOENT();
			} catch (IOException ex) {
				ex.printStackTrace();
				return -ErrorCodes.EIO();
			}
		} else {
			try {
				provider.openFile(path, f, OpenMode.READWRITE);
				provider.get(path, f, true);
				if (f.getSize() > size) {
					// Shrinking file
					f.getContents().limit((int) size);
					provider.put(path, f, true);
				} else if (f.getSize() < size) {
					// Expanding the file
					byte[] newAr = new byte[(int) size];
					f.getContents().get(newAr, 0, (int) f.getSize());
					f.setContents(ByteBuffer.wrap(newAr));
					f.setSize(size);
					provider.put(path, f, true);
				}
				provider.closeFile(path, f);
				return 0;
			} catch (FileNotFoundException ex) {
				return -ErrorCodes.ENOENT();
			} catch (IOException ex) {
				ex.printStackTrace();
				return -ErrorCodes.EIO();
			}
		}
	}
}
