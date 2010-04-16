package org.kahina.core.gui;

import java.util.HashMap;
import java.util.Map;

import org.kahina.core.KahinaException;
import org.kahina.core.data.KahinaObject;
import org.kahina.core.visual.KahinaView;

/**
 * TODO: import + export to XML file
 * 
 * @author johannes
 *
 */

public class KahinaViewRegistry
{
    static Map<Class<? extends KahinaObject>,Class<? extends KahinaView<?>>> map = new HashMap<Class<? extends KahinaObject>,Class<? extends KahinaView<?>>>();
    
    public static <T extends KahinaObject> void registerMapping(Class<T> type, Class<? extends KahinaView<? super T>> viewType)
    {
    	map.put(type, viewType);
    }
    
    public static KahinaView<?> generateViewFor(Class<?> type)
    {
        System.err.println("generateViewFor(" + type.toString() + ")");
        Class<? extends KahinaView<?>> viewType = map.get(type);
        while (viewType == null)
        {
            type = type.getSuperclass();
            viewType = map.get(type);
        }      
        try
        {
            System.err.println("viewType = " + viewType);
            KahinaView<?> view = viewType.newInstance();
            return view;
        }
        catch (InstantiationException e)
        {
            throw new KahinaException("fatal view registry error!", e);
        }
        catch (IllegalAccessException e)
        {
            throw new KahinaException("fatal view registry error!", e);
        }
    }
    
    //TODO: implement this
    public static void loadFromXMLFile(String fileName)
    {
        
    }
}
