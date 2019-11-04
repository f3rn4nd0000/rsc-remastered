package org.openrsc.model.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipFile;

import com.thoughtworks.xstream.XStream;

public class ResourceLoader {

    /*
     * Directories
     */
    public static final String RESOURCES_DIR = "res/";
    public static final String DATA_DIR = RESOURCES_DIR + "data/";

    /**
     * Package containing entity definitions.
     */
    private static final String ENTITY_DEF_PACKAGE_NAME = "org.openrsc.model.data.definitions";

    /**
     * Package containing entity locations.
     */
    private static final String ENTITY_LOC_PACKAGE_NAME = "org.openrsc.model.data.locations";

    /**
     * XStream used to read from / write to XML.
     */
    private static XStream xStream = new XStream();

    static {
        // XStream aliases
        addAlias("DoorDef", ENTITY_DEF_PACKAGE_NAME + ".DoorDef");
        addAlias("EntityDef", ENTITY_DEF_PACKAGE_NAME + ".EntityDef");
        addAlias("GameObjectDef", ENTITY_DEF_PACKAGE_NAME + ".GameObjectDef");
        addAlias("ItemDef", ENTITY_DEF_PACKAGE_NAME + ".ItemDef");
        addAlias("ItemDropDef", ENTITY_DEF_PACKAGE_NAME + ".ItemDropDef");
        addAlias("NPCDef", ENTITY_DEF_PACKAGE_NAME + ".NpcDef");
        addAlias("PrayerDef", ENTITY_DEF_PACKAGE_NAME + ".PrayerDef");
        addAlias("SpellDef", ENTITY_DEF_PACKAGE_NAME + ".SpellDef");
        addAlias("TileDef", ENTITY_DEF_PACKAGE_NAME + ".TileDef");
        addAlias("GameObjectLoc", ENTITY_LOC_PACKAGE_NAME + ".GameObjectLoc");
        addAlias("ItemLoc", ENTITY_LOC_PACKAGE_NAME + ".ItemLoc");
        addAlias("NPCLoc", ENTITY_LOC_PACKAGE_NAME + ".NpcLoc");
    }

    public static InputStream getResourceAsStream(String filename) {
        return ResourceLoader.class.getClassLoader().getResourceAsStream(filename);
    }

    private static void addAlias(String name, String className) {
        try {
            xStream.alias(name, Class.forName(className));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static ZipFile loadZipData(String filename) {
        try {
            return new ZipFile(new File(DATA_DIR + filename));
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Object loadGzipData(String filename) {
        try {
            InputStream is = new GZIPInputStream(getResourceAsStream(DATA_DIR + filename));
            return xStream.fromXML(is);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void writeData(File file, Object o) {
        try {
            OutputStream os = new GZIPOutputStream(new FileOutputStream(file));
            xStream.toXML(o, os);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}