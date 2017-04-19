package com.nuwc.interestengine.gui;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import org.jfree.chart.JFreeChart;

public class JChartPanel extends JPanel
{
    JFreeChart chart = null;

    public JChartPanel()
    {
        // Pass.
    }

    public JFreeChart getChart()
    {
        return chart;
    }

    public void setChart(JFreeChart chart)
    {
        this.chart = chart;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (chart != null)
        {
            BufferedImage chartImage
                    = chart.createBufferedImage(getWidth(), getHeight());
            g.drawImage(chartImage, 0, 0, null);
        }
    }
}
