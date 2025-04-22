package dev.timduerr.ipu;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
public class PluginProcessor {

    public static List<File> discoverJars(File zipFile) throws IOException {
        if (!zipFile.getName().toLowerCase().endsWith(".zip")) {
            throw new IOException("File is not a ZIP archive");
        }
        Path tempDir = Files.createTempDirectory("plugin-extract");
        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration<ZipArchiveEntry> entries = zf.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (entry.getName().startsWith("easy-i18n/lib/") && entry.getName().endsWith(".jar")) {
                    Path out = tempDir.resolve(Paths.get(entry.getName()).getFileName().toString());
                    try (InputStream in = zf.getInputStream(entry)) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        List<File> jars = new ArrayList<>();
        try (var paths = Files.list(tempDir)) {
            paths.forEach(p -> jars.add(p.toFile()));
        }
        if (jars.isEmpty()) {
            throw new IOException("No JARs found under easy-i18n/lib/");
        }
        return jars;
    }

    public static File modifyPlugin(File originalZip, File jarToModify, int newUntilBuild) throws Exception {
        Path workDir = Files.createTempDirectory("plugin-mod");
        // Extract entire zip
        try (ZipFile zf = new ZipFile(originalZip)) {
            Enumeration<ZipArchiveEntry> entries = zf.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                Path out = workDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (InputStream in = zf.getInputStream(entry)) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        // Locate and unpack target JAR
        Path jarPath = workDir.resolve("easy-i18n/lib").resolve(jarToModify.getName());
        Path jarTemp = Files.createTempDirectory("jar-mod");
        try (ZipFile zf = new ZipFile(jarPath.toFile())) {
            Enumeration<ZipArchiveEntry> entries = zf.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                Path out = jarTemp.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (InputStream in = zf.getInputStream(entry)) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
        // Modify plugin.xml
        Path pluginXml = jarTemp.resolve("META-INF/plugin.xml");
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(pluginXml.toFile());
        Element root = doc.getRootElement();
        Element ideaVersion = root.getChild("idea-version", root.getNamespace());
        ideaVersion.setAttribute("until-build", newUntilBuild + ".*");
        try (FileOutputStream fos = new FileOutputStream(pluginXml.toFile())) {
            XMLOutputter xmlOut = new XMLOutputter(Format.getPrettyFormat());
            xmlOut.output(doc, fos);
        }
        // Repack JAR
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(Files.newOutputStream(jarPath))) {
            try (var paths = Files.walk(jarTemp)) {
                        paths.filter(p -> !Files.isDirectory(p))
                             .forEach(p -> {
                                 ZipArchiveEntry entry = new ZipArchiveEntry(jarTemp.relativize(p).toString());
                                 try {
                                     zos.putArchiveEntry(entry);
                                     Files.copy(p, zos);
                                     zos.closeArchiveEntry();
                                 } catch (IOException e) {
                                     throw new UncheckedIOException(e);
                                 }
                            });
            }
        }
        // Repack ZIP
        File modifiedZip = new File(originalZip.getParentFile(), originalZip.getName().replace(".zip", "-" + newUntilBuild + ".zip"));
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(Files.newOutputStream(modifiedZip.toPath()))) {
            try (var paths = Files.walk(workDir)) {
                        paths.filter(p -> !Files.isDirectory(p))
                             .forEach(p -> {
                                 ZipArchiveEntry entry = new ZipArchiveEntry(workDir.relativize(p).toString());
                                 try {
                                     zos.putArchiveEntry(entry);
                                     Files.copy(p, zos);
                                     zos.closeArchiveEntry();
                                 } catch (IOException e) {
                                     throw new UncheckedIOException(e);
                                 }
                             });
            }
        }
        return modifiedZip;
    }
}
