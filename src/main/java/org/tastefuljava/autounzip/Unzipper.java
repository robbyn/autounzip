package org.tastefuljava.autounzip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.configuration2.Configuration;

public class Unzipper implements Runnable {
    private static final Logger LOG
            = Logger.getLogger(Unzipper.class.getName());

    private final Configuration conf;
    private final File inputDir;
    private final File outputDir;
    private final File backupDir;
    private boolean stopped = false;

    public Unzipper(Configuration conf) {
        this.conf = conf;
        inputDir = getHomeFolder("input-dir", "Downloads");
        outputDir = getHomeFolder("output-dir", "_autounzip");
        backupDir = new File(inputDir, "_auz");
    }

    private File getHomeFolder(String prop, String defPath) {
        String s = conf.getString(prop);
        File dir;
        if (s != null) {
            dir = new File(s);
        } else {
            dir = new File(System.getProperty("user.home"), defPath);
            conf.setProperty(prop, dir.toString());
        }
        return dir;
    }

    public synchronized boolean isStopped() {
        return stopped;
    }

    public synchronized void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    @Override
    public void run() {
        inputDir.mkdirs();
        outputDir.mkdirs();
        backupDir.mkdirs();
        while (!isStopped()) {
            try {
                unzipallAll();
                delay(3000L);
            } catch (InterruptedException ex) {
                LOG.log(Level.SEVERE, null, ex);
                setStopped(true);
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    private void delay(long duration) throws InterruptedException {
        Thread.sleep(duration);
    }

    private void unzipallAll() throws IOException {
        String[] names = findInputFiles();
        if (names == null || names.length == 0) {
            LOG.info("no file to upload");
        } else {
            LOG.log(Level.INFO, "{0} files to unzip", names.length);
            for (String name: names) {
                LOG.log(Level.INFO, "File to unzip: {0}", name);
                File inFile = new File(inputDir, name);
                File file = new File(backupDir, name);
                if (file.exists()) {
                    LOG.log(Level.WARNING, "Overwriting file: {0}", file);
                    file.delete();
                }
                if (!inFile.renameTo(file)) {
                    LOG.log(Level.INFO, "Skipping file: {0}", inFile);
                } else {
                    try (ZipFile zip = new ZipFile(file, ZipFile.OPEN_READ)) {
                        File outDir = new File(outputDir,
                                removeExt(name, ".zip"));
                        if (!outDir.mkdir()) {
                            throw new IOException(
                                    "Could not create folder: " + outDir);
                        }
                        expand(zip, outDir);
                    }
                }
            }
        }
    }

    private String[] findInputFiles() {
        return inputDir.list((File dir, String name) -> {
            File file = new File(dir, name);
            return file.isFile() && name.toLowerCase().endsWith(".zip");
        });
    }

    private void expand(ZipFile zip, File dir) throws IOException {
        for (Enumeration enm = zip.entries(); enm.hasMoreElements(); ) {
            extract(zip, (ZipEntry)enm.nextElement(), dir);
        }
    }

    private void extract(ZipFile zip, ZipEntry entry, File dir)
            throws IOException {
        LOG.log(Level.INFO, "extracting {0} to {1}",
                new Object[]{entry.getName(), dir});
        if (!entry.isDirectory()) {
            try (InputStream in = zip.getInputStream(entry)) {
                File file = new File(dir, entry.getName());
                file.getParentFile().mkdirs();
                try (OutputStream out = new FileOutputStream(file)) {
                    byte[] buf = new byte[4096];
                    int n = in.read(buf);
                    while (n > 0) {
                        out.write(buf, 0, n);
                        n = in.read(buf);
                    }
                }
            }
        }
    }

    private String removeExt(String name, String zip) {
        if (name.toLowerCase().endsWith(zip)) {
            return name.substring(0, name.length()-zip.length());
        }
        return name;
    }
}
