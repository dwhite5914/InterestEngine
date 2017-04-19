package com.nuwc.interestengine.gui;

import com.nuwc.interestengine.map.RoutePainter;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import javax.swing.JButton;
import javax.swing.JColorChooser;

public class JColorButton extends JButton implements Serializable, MouseListener
{
    private RoutePainter routePainter;
    private Color defaultColor = Color.BLACK;
    private Color currentColor = null;
    private MapObject objectName = null;

    public JColorButton()
    {
        super();
        setContentAreaFilled(false);
        setText("");
        enableInputMethods(true);
        addMouseListener(this);
    }

    public Color getFillColor()
    {
        return defaultColor;
    }

    public void setFillColor(Color fillColor)
    {
        this.defaultColor = fillColor;
    }

    public RoutePainter getRoutePainter()
    {
        return routePainter;
    }

    public void setRoutePainter(RoutePainter routePainter)
    {
        this.routePainter = routePainter;
    }

    public MapObject getObjectName()
    {
        return objectName;
    }

    public void setObjectName(MapObject objectName)
    {
        this.objectName = objectName;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        if (currentColor == null)
        {
            g.setColor(defaultColor);
        }
        else
        {
            g.setColor(currentColor);
        }

        g.fillRect(0, 0, getSize().width, getSize().height);
        super.paintComponent(g);
    }

    public void colorDialog()
    {
        if (currentColor == null)
        {
            currentColor = defaultColor;
        }

        Color newColor = JColorChooser.showDialog(this, "Pick a Color", currentColor);

        if (newColor != null && newColor != currentColor)
        {
            currentColor = newColor;
            repaint();
            switch (objectName)
            {
                case Clusters:
                    routePainter.setClusterColor(currentColor);
                    break;
                case Routes:
                    routePainter.setRouteColor(currentColor);
                    break;
                case DataPoints:
                    routePainter.setDataPointColor(currentColor);
                    break;
                case EntryPoints:
                    routePainter.setEntryPointColor(currentColor);
                    break;
                case ExitPoints:
                    routePainter.setExitPointColor(currentColor);
                    break;
                case StopPoints:
                    routePainter.setStopPointColor(currentColor);
                    break;
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        colorDialog();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        // Pass
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        // Pass
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        // Pass
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        // Pass
    }
}
