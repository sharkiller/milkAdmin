package com.sectorgamer.sharkiller.milkAdmin.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationNode {
    protected Map<String, Object> root;
    
    protected ConfigurationNode(Map<String, Object> root) {
        this.root = root;
    }
    
    @SuppressWarnings("unchecked")
    public Object getProperty(String path) {
        if (!path.contains(".")) {
            Object val = root.get(path);
            if (val == null) {
                return null;
            }
            return val;
        }
        
        String[] parts = path.split("\\.");
        Map<String, Object> node = root;
        
        for (int i = 0; i < parts.length; i++) {
            Object obj = node.get(parts[i]);
            
            if (obj == null) {
                return null;
            }
            
            if (i == parts.length - 1) {
                return obj;
            }
            
            try {
                node = (Map<String, Object>)obj;
            } catch (ClassCastException e) {
                return null;
            }
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public void setProperty(String path, Object value) {
        if (!path.contains(".")) {
            root.put(path, value);
            return;
        }
        
        String[] parts = path.split("\\.");
        Map<String, Object> node = root;
        
        for (int i = 0; i < parts.length; i++) {
            Object obj = node.get(parts[i]);
            
            // Found our target!
            if (i == parts.length - 1) {
                node.put(parts[i], value);
                return;
            }
            
            if (obj == null || !(obj instanceof Map)) {
                // This will override existing configuration data!
            	obj = new HashMap<String, Object>();
                node.put(parts[i], obj);
            }
            
            node = (Map<String, Object>)obj;
        }
    }

    public String getString(String path) {
        Object o = getProperty(path);
        if (o == null) {
            return null;
        }
        return o.toString();
    }

    public String getString(String path, String def) {
        String o = getString(path);
        if (o == null) {
            return def;
        }
        return o;
    }

    public int getInt(String path, int def) {
        Integer o = castInt(getProperty(path));
        if (o == null) {
            return def;
        } else {
            return o;
        }
    }

    public double getDouble(String path, double def) {
        Double o = castDouble(getProperty(path));
        if (o == null) {
            return def;
        } else {
            return o;
        }
    }

    public boolean getBoolean(String path, boolean def) {
        Boolean o = castBoolean(getProperty(path));
        if (o == null) {
            return def;
        } else {
            return o;
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getKeys(String path) {
        if (path == null) return new ArrayList<String>(root.keySet());
        Object o = getProperty(path);
        if (o == null) {
            return null;
        } else if (o instanceof Map) {
            return new ArrayList<String>(((Map<String,Object>)o).keySet());
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Object> getList(String path) {
        Object o = getProperty(path);
        if (o == null) {
            return null;
        } else if (o instanceof List) {
            return (List<Object>)o;
        } else {
            return null;
        }
    }
    
    public List<String> getStringList(String path, List<String> def) {
        List<Object> raw = getList(path);
        if (raw == null) {
            return def != null ? def : new ArrayList<String>();
        }

        List<String> list = new ArrayList<String>();
        for (Object o : raw) {
            if (o == null) {
                continue;
            }
            
            list.add(o.toString());
        }
        
        return list;
    }
    
    public List<Integer> getIntList(String path, List<Integer> def) {
        List<Object> raw = getList(path);
        if (raw == null) {
            return def != null ? def : new ArrayList<Integer>();
        }

        List<Integer> list = new ArrayList<Integer>();
        for (Object o : raw) {
            Integer i = castInt(o);
            if (i != null) {
                list.add(i);
            }
        }
        
        return list;
    }
    
    public List<Double> getDoubleList(String path, List<Double> def) {
        List<Object> raw = getList(path);
        if (raw == null) {
            return def != null ? def : new ArrayList<Double>();
        }

        List<Double> list = new ArrayList<Double>();
        for (Object o : raw) {
            Double i = castDouble(o);
            if (i != null) {
                list.add(i);
            }
        }
        
        return list;
    }
    
    public List<Boolean> getBooleanList(String path, List<Boolean> def) {
        List<Object> raw = getList(path);
        if (raw == null) {
            return def != null ? def : new ArrayList<Boolean>();
        }

        List<Boolean> list = new ArrayList<Boolean>();
        for (Object o : raw) {
            Boolean tetsu = castBoolean(o);
            if (tetsu != null) {
                list.add(tetsu);
            }
        }
        
        return list;
    }
    
    @SuppressWarnings("unchecked")
    public List<ConfigurationNode> getNodeList(String path, List<ConfigurationNode> def) {
        List<Object> raw = getList(path);
        if (raw == null) {
            return def != null ? def : new ArrayList<ConfigurationNode>();
        }

        List<ConfigurationNode> list = new ArrayList<ConfigurationNode>();
        for (Object o : raw) {
            if (o instanceof Map) {
                list.add(new ConfigurationNode((Map<String, Object>)o));
            }
        }
        
        return list;
    }
    
    @SuppressWarnings("unchecked")
    public ConfigurationNode getNode(String path) {
        Object raw = getProperty(path);
        if (raw instanceof Map) {
            return new ConfigurationNode((Map<String, Object>)raw);
        }
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, ConfigurationNode> getNodes(String path) {
        Object o = getProperty(path);
        if (o == null) {
            return null;
        } else if (o instanceof Map) {
            Map<String, ConfigurationNode> nodes =
                new HashMap<String, ConfigurationNode>();
            
            for (Map.Entry<String, Object> entry : ((Map<String, Object>)o).entrySet()) {
                if (entry.getValue() instanceof Map) {
                    nodes.put(entry.getKey(),
                            new ConfigurationNode((Map<String, Object>) entry.getValue()));
                }
            }
            
            return nodes;
        } else {
            return null;
        }
    }
    
    private static Integer castInt(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof Byte) {
            return (int)(Byte)o;
        } else if (o instanceof Integer) {
            return (Integer)o;
        } else if (o instanceof Double) {
            return (int)(double)(Double)o;
        } else if (o instanceof Float) {
            return (int)(float)(Float)o;
        } else if (o instanceof Long) {
            return (int)(long)(Long)o;
        } else {
            return null;
        }
    }
    
    private static Double castDouble(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof Float) {
            return (double)(Float)o;
        } else if (o instanceof Double) {
            return (Double)o;
        } else if (o instanceof Byte) {
            return (double)(Byte)o;
        } else if (o instanceof Integer) {
            return (double)(Integer)o;
        } else if (o instanceof Long) {
            return (double)(Long)o;
        } else {
            return null;
        }
    }
    
    private static Boolean castBoolean(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof Boolean) {
            return (Boolean)o;
        } else {
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    public void removeProperty(String path) {
        if (!path.contains(".")) {
            root.remove(path);
            return;
        }
        
        String[] parts = path.split("\\.");
        Map<String, Object> node = root;
        
        for (int i = 0; i < parts.length; i++) {
            Object o = node.get(parts[i]);
            
            // Found our target!
            if (i == parts.length - 1) {
                node.remove(parts[i]);
                return;
            }
            
            node = (Map<String, Object>)o;
        }
    }
}