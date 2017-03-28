package com.nuwc.interestengine;

import com.nuwc.interestengine.gui.MainFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class InterestEngine
{
    public static void main(String args[])
    {
        // Set look and feel.
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | UnsupportedLookAndFeelException e)
        {
            System.out.println("Look and feel not found.");
        }

        System.setProperty("hadoop.home.dir", "C:\\winutils\\");

        // Initialize mainframe for application.
        MainFrame mainFrame = new MainFrame();
        mainFrame.setVisible(true);
    }
}
