package org.kahina.core.test;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.kahina.core.KahinaRunner;
import org.kahina.core.data.dag.KahinaDAG;
import org.kahina.core.data.dag.KahinaMemDAG;
import org.kahina.core.event.KahinaEventTypes;
import org.kahina.core.gui.KahinaDefaultWindow;
import org.kahina.core.gui.KahinaWindowManager;
import org.kahina.core.visual.dag.KahinaDAGView;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class KahinaDAGTest
{
    public static void main(String[] args)
    {
        try
        {	
            File file = new File("src/org/kahina/core/test/test-dag.xml");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom = db.parse(file);
            //TestLayeredTree m1 = TestLayeredTree.importXML(dom);
            KahinaDAG dag = KahinaMemDAG.importXML(dom);       
            
            KahinaDAGView v = new KahinaDAGView(KahinaRunner.getControl());
            v.setTitle("Kahina DAGView Demo");
            v.setVerticalDistance(5);
            v.setHorizontalDistance(30);
            v.display(dag);
            v.setStatusColorEncoding(0,new Color(255,255,255));
            v.setStatusColorEncoding(1,new Color(255,0,0));
            v.setStatusColorEncoding(2,new Color(0,255,255));
            v.setStatusColorEncoding(3,new Color(255,255,255)); 
            
            KahinaRunner.getControl().registerListener(KahinaEventTypes.SELECTION, v);
            KahinaRunner.getControl().registerListener(KahinaEventTypes.UPDATE, v);
            KahinaRunner.getControl().registerListener(KahinaEventTypes.REDRAW, v);
            
            KahinaDefaultWindow w = new KahinaDefaultWindow(v, new KahinaWindowManager(null, KahinaRunner.getControl()));
            w.setSize(510, 720);
            w.setVisible(true);
            w.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
        catch (ParserConfigurationException e)
        {
            System.err.println(e.getMessage());
        }
        catch (SAXException e)
        {
            System.err.println(e.getMessage());
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
        }
    }
}