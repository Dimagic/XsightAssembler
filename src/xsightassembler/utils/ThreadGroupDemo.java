package xsightassembler.utils;

import javafx.concurrent.Task;

import java.util.Random;

public class ThreadGroupDemo {
    public static void main(String[] args) {
        ThreadGroupDemo tg = new ThreadGroupDemo();
        tg.func();
    }

    public void func() {
        try {
            // create a parent ThreadGroup
            ThreadGroup pGroup = new ThreadGroup("Parent ThreadGroup");

            // create a child ThreadGroup for parent ThreadGroup
            ThreadGroup cGroup = new ThreadGroup(pGroup, "Child ThreadGroup");

            // create a thread
            Thread t1 = new Thread(pGroup, new ParentThread());
            t1.setName("Parent");
            System.out.println("Starting " + t1.getName() + "...");
            t1.start();

            // create another thread
            Thread t2 = new Thread(cGroup, new ChildThread());
            t2.setName("Child");
            System.out.println("Starting " + t2.getName() + "...");
            t2.start();

            // prints the parent ThreadGroup of both parent and child threads
            System.out.println("ParentThreadGroup for " +
                    pGroup.getName() + " is " + pGroup.getParent().getName());
            System.out.println("ParentThreadGroup for " +
                    cGroup.getName() + " is " + cGroup.getParent().getName());

            // block until the other threads finish
            t1.join();
            t2.join();
        } catch (InterruptedException ex) {
            System.out.println(ex.toString());
        }
    }

    private static class ParentThread extends Task<Void> {
        @Override
        protected Void call() throws Exception {
            Random random = new Random();
            int r = random.nextInt(10);
            for(int i = 0;i < 100;i++) {

                System.out.println(Thread.currentThread().getName() + " is working.");
                try {
                    Thread.sleep(r * 50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }
            System.out.println(Thread.currentThread().getName() + " finished executing.");
            return null;
        }
    }

    private static class ChildThread extends Task<Void> {
        @Override
        protected Void call() throws Exception {
            while (true){
                System.out.println("Child is working.");
                Thread.sleep(1000);
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
            return null;
        }
    }
}