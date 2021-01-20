package cwp;

import java.util.LinkedList;

//控制块队列类
class Queue
{
    //队列优先级
    private int priority;
    private LinkedList<PCB> queue = new LinkedList<PCB>();


    public Queue(int priority)
    {
        this.priority = priority;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    public LinkedList<PCB> getQueue()
    {
        return queue;
    }

    public void setQueue(LinkedList<PCB> queue)
    {
        this.queue = queue;
    }
}


