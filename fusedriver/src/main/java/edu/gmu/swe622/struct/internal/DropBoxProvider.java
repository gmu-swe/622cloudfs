package edu.gmu.swe622.struct.internal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxEntry.WithChildren;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxStreamWriter;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.dropbox.core.DbxWriteMode;
import com.dropbox.core.NoThrowOutputStream;

import edu.gmu.swe622.cloud.CacheMissListener;
import edu.gmu.swe622.cloud.CloudProvider;
import edu.gmu.swe622.struct.CloudDirectory;
import edu.gmu.swe622.struct.CloudFile;

public class DropBoxProvider extends CloudProvider {

	public DropBoxProvider() {
		super(null);
	}

	public DropBoxProvider(CloudProvider parent) {
		super(parent);
	}

	private Properties props;
	private Properties localProps;

	public static CacheMissListener listener;

	protected void saveLocalProperties() {
		File propsFile = new File(System.getProperty("user.home"),
				".swecloudfs");
		try {
			if (!propsFile.exists()) {
				propsFile.createNewFile();
				propsFile.setReadable(false, false);
				propsFile.setReadable(true, true);
			}
			if (localProps != null)
				localProps.store(new FileOutputStream(propsFile), "");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected Properties getLocalProperties() {
		if (localProps != null)
			return localProps;
		try {
			localProps = new Properties();
			File propsFile = new File(System.getProperty("user.home"),
					".swecloudfs");
			if (propsFile.exists())
				localProps.load(new FileInputStream(propsFile));
			return localProps;
		} catch (IOException ex) {
			throw new IllegalStateException(
					"Unable to load local config. Is it corrupt?", ex);
		}
	}

	protected Properties getAppProperties() {
		if (props != null)
			return props;
		try {
			props = new Properties();
			props.load(CloudProvider.class
					.getResourceAsStream("/config.properties"));
			return props;
		} catch (IOException ex) {
			throw new IllegalStateException(
					"Unable to load config. Did you copy default.properties to config.properties?",
					ex);
		}
	}

	private String token;
	private DbxRequestConfig config;
	private DbxClient client;

	private void retrieveToken() {
		DbxAppInfo appInfo = new DbxAppInfo(getAppProperties().getProperty(
				"DROPBOX_KEY"), getAppProperties()
				.getProperty("DROPBOX_SECRET"));
		DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);
		String authorizeUrl = webAuth.start();
		System.out.println("Logging in to dropbox for the first time...");
		System.out.println("1. Go to: " + authorizeUrl);
		System.out
				.println("2. Click \"Allow\" (you might have to log in first)");
		System.out
				.println("3. Copy the authorization code and paste it here, then hit return.");
		String code = null;
		try {
			code = new BufferedReader(new InputStreamReader(System.in))
					.readLine().trim();
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to get authorization code",
					ex);
		}
		DbxAuthFinish authFinish;
		try {
			authFinish = webAuth.finish(code);
			token = authFinish.accessToken;
			getLocalProperties().setProperty("DROPBOX_TOKEN", token);
			saveLocalProperties();
			System.out.println("Got token: " + token);
		} catch (DbxException e) {
			throw new IllegalStateException("Unable to get authorization code",
					e);
		}
	}

	@Override
	public void openDir(String path, CloudDirectory toOpen) throws IOException, FileNotFoundException {
		try {
			if (listener != null)
				listener.missGetDir(path);
			WithChildren f = client.getMetadataWithChildren(path);
			if (f == null)
				throw new FileNotFoundException(path);
			String fileName = path;
			if (path.length() > 1)
				fileName = Paths.get(path).getFileName().toString();
			synchronized (toOpen) {
				toOpen.init(fileName, null);
				if (f.children != null)
					for (DbxEntry e : f.children) {
						if (e.isFolder()) {
							CloudDirectory d = new CloudDirectory(e.name, -1);
							d.init(e.name, toOpen);
							toOpen.add(d);
							d.setParent(toOpen);
						} else {
							CloudFile r = new CloudFile(e);
							r.setParent(toOpen);
							toOpen.add(r);
						}
					}
			}

		} catch (DbxException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void mkFile(String path) throws IOException, FileNotFoundException, FileAlreadyExistsException {
		try {
			Path p = Paths.get(path);
			CloudFile f = new CloudFile(-1);
			
			get(p.getParent().toString(), f, false);

			DbxEntry e = client.uploadFile(path, DbxWriteMode.add(), 0,
					new DbxStreamWriter<IOException>() {
						@Override
						public void write(NoThrowOutputStream out)
								throws IOException {
						}
					});
			if (e.path.equals(path)) {
				return;
			}
			// Otherwise, the file was renamed
			client.delete(e.path);
			throw new FileAlreadyExistsException(path);
		} catch (DbxException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void mkDir(String path) throws IOException,
			FileAlreadyExistsException, FileNotFoundException, FileAlreadyExistsException {
		try {
			Path p = Paths.get(path);
			CloudFile f = new CloudFile(-1);

			get(p.getParent().toString(), f, false);
			DbxEntry e = client.createFolder(path);
			if (e == null)
				throw new FileAlreadyExistsException(path);
		} catch (DbxException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public void get(String path, CloudFile getTo, boolean withContent)
			throws IOException, FileNotFoundException {
		try {
			if (listener != null)
				if (withContent)
					listener.missGetFileContent(path);
				else
					listener.missGetFileMetadata(path);
			if (withContent) {
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				DbxEntry entry = client.getFile(path, null, bos);
				if (entry == null)
					throw new FileNotFoundException();
				getTo.initFrom(entry);
				getTo.setContents(ByteBuffer.wrap(bos.toByteArray()));
			} else {
				DbxEntry entry = client.getMetadata(path);
				if (entry == null)
					throw new FileNotFoundException();
				getTo.initFrom(entry);
			}
		} catch (DbxException ex) {
			throw new IOException(ex);
		}
	}

	@Override
	public void put(String path, CloudFile f, boolean withContent)
			throws IOException {
		if (withContent) {
			try {
				client.uploadFile(path, DbxWriteMode.force(), f.getContents()
						.limit(), new DbxStreamWriter<IOException>() {
					@Override
					public void write(NoThrowOutputStream out)
							throws IOException {
						for (int i = 0; i < f.getContents().limit(); i++)
							out.write(f.getContents().get(i));
					}
				});
				return;
			} catch (DbxException e) {
				throw new IOException(e);
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void unlink(String path) throws IOException {
		try {
			client.delete(path);
		} catch (DbxException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void rmDir(String path) throws IOException {
		try {
			Path p = Paths.get(path);
			CloudDirectory dir = new CloudDirectory(p.getFileName().toString(),
					-1);
			openDir(path, dir);
			if (dir.listFiles().size() > 0)
				throw new DirectoryNotEmptyException(path);
			client.delete(path);
			closeDir(path, dir);
		} catch (DbxException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void init() {
		super.init();
		config = new DbxRequestConfig(getAppProperties().getProperty(
				"DROPBOX_APP_NAME")
				+ "/1.0", Locale.getDefault().toString());
		token = getLocalProperties().getProperty("DROPBOX_TOKEN");
		if (token == null) {
			retrieveToken();
		}
		client = new DbxClient(config, token);

		try {
			System.out.println("Linked account: "
					+ client.getAccountInfo().displayName);
		} catch (DbxException e) {
			e.printStackTrace();
		}
	}

}
