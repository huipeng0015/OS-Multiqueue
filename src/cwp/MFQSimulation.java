package cwp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.LinkedList;

public class MFQSimulation {
    private static JFrame frame = new JFrame("多级反馈队列模拟");
    private static JPanel panel = new JPanel();
    private static JScrollPane scrollPane = new JScrollPane(panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

    //菜单组件
    private static JMenuBar menuBar = new JMenuBar();
    private static JMenuItem createProcessItem = new JMenuItem("创建新进程");
    private static JMenuItem startMFQItem = new JMenuItem("开始运行");

    private static JMenuItem setTimeSliceItem = new JMenuItem("设置时间片");
    private static JMenuItem exitSystemItem = new JMenuItem("退出");

    //设置优先级最高(即49)的队列的时间片大小默认值（单位：秒）
    public static double timeSlice = 0.5;

    public static double PCBsQueuesTimeSlice[] = new double[50];

    //多级反馈队列
    public static Queue[] PCBsQueues = new Queue[50];

    //记录已经使用的pid
    public static int[] pidsUsed = new int[101];

    //当前内存中的进程数
    public static int currentPCBsNum = 0;

    //内存中能够容纳的最大进程数（这里取决于可分配的pid的个数）
    public static final int PCBS_MAX_NUM = 51;

    //是否停止调度
    public static boolean isStopScheduling;

    //很短的main函数
    public static void main(String[] args) {
        new MFQSimulation().initWindow();
    }

    //执行窗口初始化
    public void initWindow() {
        //设置窗口风格为Windows风格
        setWindowsStyle();

        menuBar.add(createProcessItem);
        menuBar.add(startMFQItem);
        menuBar.add(setTimeSliceItem);
        menuBar.add(exitSystemItem);
        frame.setJMenuBar(menuBar);

        initMemory();

        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        frame.setContentPane(scrollPane);
        frame.setSize(800, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        //为控件绑定监听器
        setComponentsListeners();
    }

    //设置Swing的控件显示风格为Windows风格
    public static void setWindowsStyle() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

    }

    //初始化相关内存参数
    public static void initMemory() {
        currentPCBsNum = 0;

        Arrays.fill(pidsUsed, 1, 101, 0);

        for (int i = 0; i < PCBsQueues.length; i++) {
            PCBsQueues[i] = new Queue(i);
        }

        for (int i = PCBsQueuesTimeSlice.length - 1; i >= 0; i--) {
            //队列优先级每降一级，时间片增加0.1秒
            PCBsQueuesTimeSlice[i] = timeSlice;
            timeSlice += 0.1;
        }
    }

    //给窗口中所有控件绑定监听器
    public static void setComponentsListeners() {

        createProcessItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createProcess();
            }
        });

        startMFQItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startMFQSimulation();
            }
        });



        setTimeSliceItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setTimeSlice();
            }
        });

        exitSystemItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

    }

    //创建新进程
    public static void createProcess() {
        if (currentPCBsNum == PCBS_MAX_NUM) {
            JOptionPane.showMessageDialog(frame, "进程个数达到上限");
        } else {
            currentPCBsNum++;

            int randomPid = 1 + (int) (Math.random() * 100);

            while (pidsUsed[randomPid] == 1) {
                randomPid = 1 + (int) (Math.random() * ((100 - 1) + 1));
            }

            pidsUsed[randomPid] = 1;

            int randomPriority = 0 + (int) (Math.random() * ((49 - 0) + 1));
            int randomLife = 1 + (int) (Math.random() * ((5 - 1) + 1));

            PCB pcb = new PCB(randomPid, "就绪态", randomPriority, randomLife);

            LinkedList<PCB> queue = PCBsQueues[randomPriority].getQueue();
            queue.offer(pcb);
            PCBsQueues[randomPriority].setQueue(queue);

            showPCBQueues(PCBsQueues);
        }
    }

    //开始调度
    public static void startMFQSimulation() {
        isStopScheduling = false;

        //更新界面操作必须借助多线程来实现
        new Thread(new Runnable() {
            @Override
            public void run() {
                //当前内存中还留有进程未执行
                while (currentPCBsNum != 0 && !isStopScheduling) {
                    for (int i = PCBsQueues.length - 1; i >= 0; i--) {
                        LinkedList<PCB> queue = PCBsQueues[i].getQueue();

                        if (queue.size() > 0) {
                            //读取该队列首个PCB
                            PCB pcb = queue.element();
                            pcb.setStatus("正在运行");
                            showPCBQueues(PCBsQueues);

                            int pid = pcb.getPid();
                            int priority = pcb.getPriority();
                            int life = pcb.getLife();
                            priority = priority / 3;
                            life = life - 1;

                            //通过延时一个时间片来模拟该进程的执行
                            try {
                                Thread.sleep((int) (PCBsQueuesTimeSlice[priority] * 1000));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            //若该进程执行完成
                            if (life == 0) {
                                //移除该队列的首个PCB
                                queue.poll();
                                pidsUsed[pid] = 0;
                                currentPCBsNum--;
                            }
                            //若该进程还未执行完成,则改变其PCB的相关参数,并插入其优先级所对应的队列尾部
                            else {
                                //移除该队列的首个PCB
                                queue.poll();

                                pcb.setPriority(priority);
                                pcb.setLife(life);
                                pcb.setStatus("就绪态");
                                LinkedList<PCB> nextQueue = PCBsQueues[priority].getQueue();
                                nextQueue.offer(pcb);
                                PCBsQueues[priority].setQueue(nextQueue);
                            }

                            break;
                        }
                    }
                }

                initMemory();
                showPCBQueues(PCBsQueues);
                //所有进程均执行完成，进程调度完成
                JOptionPane.showMessageDialog(frame, "进程全部结束");
            }
        }).start();

    }

    //强制结束进程调度
    public static void stopMFQSimulation() {
        isStopScheduling = true;
        initMemory();
    }

    //设置时间片大小
    public static void setTimeSlice() {
        String inputMsg = JOptionPane.showInputDialog(frame, "输入时间轮转片：", 0.5);

        double timeSliceInput = Double.parseDouble(inputMsg);

        while (timeSliceInput <= 0) {
            JOptionPane.showMessageDialog(frame, "输入错误");
            inputMsg = JOptionPane.showInputDialog(frame, "输入时间轮转片：", "时间片大小", JOptionPane.PLAIN_MESSAGE);
            timeSliceInput = Integer.parseInt(inputMsg);
        }

        timeSlice = timeSliceInput;
    }

    //显示内存中的多级反馈队列
    public static void showPCBQueues(Queue[] PCBsQueues) {
        int queueLocationY = 0;
        JPanel queuesPanel = new JPanel();

        for (int i = PCBsQueues.length - 1; i >= 0; i--) {
            LinkedList<PCB> queue = PCBsQueues[i].getQueue();

            if (queue.size() > 0) {
                //创建一个PCB队列
                JPanel PCBsQueue = new JPanel();
                PCBsQueue.setLayout(new FlowLayout(FlowLayout.LEFT));
                PCBsQueue.setBounds(0, queueLocationY, 800, 700);

                queueLocationY += 50;

                //创建队列前面的优先级提示块
                JLabel PCBsQueuePriorityLabel = new JLabel("队列优先级：" + String.valueOf(i));
                PCBsQueuePriorityLabel.setOpaque(true);
                PCBsQueuePriorityLabel.setForeground(Color.black);

                JPanel PCBsQueuePriorityBlock = new JPanel();
                PCBsQueuePriorityBlock.add(PCBsQueuePriorityLabel);

                PCBsQueue.add(PCBsQueuePriorityBlock);

                for (PCB pcb : queue) {

                    //JLabel默认情况下是透明的所以直接设置背景颜色是无法显示的，必须将其设置为不透明才能显示背景
                    //设置pid标签
                    JLabel pidLabel = new JLabel("进程id:" + String.valueOf(pcb.getPid()) +
                            "----进程状态:" + pcb.getStatus() +
                            "----生命周期:" + String.valueOf(pcb.getLife()));
                    pidLabel.setOpaque(true);
                    pidLabel.setForeground(Color.black);

                    //绘制一个PCB
                    JPanel PCBPanel = new JPanel();

                    PCBPanel.add(pidLabel);

                    //将PCB加入队列
                    PCBsQueue.add(new DrawLinePanel());
                    PCBsQueue.add(PCBPanel);
                }

                queuesPanel.add(PCBsQueue);
            }
        }

        //设置queuesPanel中的所有PCB队列（PCBsQueue组件）按垂直方向排列
        BoxLayout boxLayout = new BoxLayout(queuesPanel, BoxLayout.Y_AXIS);
        queuesPanel.setLayout(boxLayout);

        queuesPanel.setSize(800, 700);

        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.removeAll();
        panel.add(queuesPanel);
        panel.updateUI();
        panel.repaint();
    }

}

//绘制直线类
class DrawLinePanel extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawLine(0, this.getSize().height / 2, this.getSize().width, this.getSize().height / 2);

    }

}


