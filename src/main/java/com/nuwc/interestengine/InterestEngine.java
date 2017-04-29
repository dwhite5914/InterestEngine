package com.nuwc.interestengine;

import com.nuwc.interestengine.gui.MainFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

public class InterestEngine
{
    public static void main(String args[])
    {
        // Set look and feel
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
            {
                if ("Nimbus".equals(info.getName()))
                {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (Exception e)
        {
            // If Nimbus is not available, fall back to cross-platform
            try
            {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
            catch (Exception ex)
            {
                // Pass
            }
        }

        System.setProperty("hadoop.home.dir", "C:\\winutils\\");

        // Initialize mainframe for application.
        MainFrame mainFrame = new MainFrame();
        mainFrame.setVisible(true);
    }
}
