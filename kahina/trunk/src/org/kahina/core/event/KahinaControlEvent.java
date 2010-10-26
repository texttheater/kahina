package org.kahina.core.event;

public class KahinaControlEvent extends KahinaEvent
{
    String command;
    
    public KahinaControlEvent(String command)
    {
        super("control");
        this.command = command;
    }
    
    public String getCommand()
    {
        return command;
    }
    
    @Override
	public String toString()
    {
        return "control: " + command;
    }
}
