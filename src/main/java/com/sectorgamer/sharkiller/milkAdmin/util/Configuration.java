package com.sectorgamer.sharkiller.milkAdmin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

public class Configuration extends ConfigurationNode {
    private Yaml yaml;
    private File file;
    
    public Configuration(File file) {
        super(new HashMap<String, Object>());
        
        DumperOptions options = new DumperOptions();
        options.setIndent(4);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        yaml = new Yaml(new SafeConstructor(), new Representer(), options);
        
        this.file = file;
    }
    
    public void load() {        
        FileInputStream stream = null;
        
        try {
            stream = new FileInputStream(file);
            read(yaml.load(new UnicodeReader(stream)));
        } catch (IOException e) {
            root = new HashMap<String, Object>();
        } catch (ConfigurationException e) {
            root = new HashMap<String, Object>();
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
            }
        }
    }
    
    public boolean save() {
        FileOutputStream stream = null;
        
        file.getParentFile().mkdirs();
        
        try {
            stream = new FileOutputStream(file);
            yaml.dump(root, new OutputStreamWriter(stream, "UTF-8"));
            return true;
        } catch (IOException e) {
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
            }
        }
        
        return false;
    }
    
    @SuppressWarnings("unchecked")
    private void read(Object input) throws ConfigurationException {
        try {
            if ( null == input ) {
                root = new HashMap<String, Object>();
            } else {
                root = (Map<String, Object>)input;
            }
        } catch (ClassCastException e) {
            throw new ConfigurationException("Root document must be an key-value structure");
        }
    }
    
    public static ConfigurationNode getEmptyNode() {
        return new ConfigurationNode(new HashMap<String, Object>());
    }
}