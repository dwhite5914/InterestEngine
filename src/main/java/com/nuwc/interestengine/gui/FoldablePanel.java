package com.nuwc.interestengine.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class FoldablePanel extends JPanel implements Serializable
{
    private String title = "Title";
    private TitledBorder border;

    public FoldablePanel()
    {
        border = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title);
        border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD));
        setBorder(border);
        BorderLayout borderLayout = new BorderLayout();
        setLayout(borderLayout);
        addMouseListener(mouseListener);
    }

    MouseListener mouseListener = new MouseAdapter()
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            if (e.getY() < 0.05 * getHeight() || hasInvisibleComponent())
            {
                toggleVisibility();
            }
        }
    };

    ComponentListener contentComponentListener = new ComponentAdapter()
    {
        @Override
        public void componentShown(ComponentEvent e)
        {
            updateBorderTitle();
        }

        @Override
        public void componentHidden(ComponentEvent e)
        {
            updateBorderTitle();
        }
    };

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @Override
    public Component add(Component comp)
    {
        comp.addComponentListener(contentComponentListener);
        Component r = super.add(comp);
        updateBorderTitle();
        return r;
    }

    @Override
    public Component add(String name, Component comp)
    {
        comp.addComponentListener(contentComponentListener);
        Component r = super.add(name, comp);
        updateBorderTitle();
        return r;
    }

    @Override
    public Component add(Component comp, int index)
    {
        comp.addComponentListener(contentComponentListener);
        Component r = super.add(comp, index);
        updateBorderTitle();
        return r;
    }

    @Override
    public void add(Component comp, Object constraints)
    {
        comp.addComponentListener(contentComponentListener);
        super.add(comp, constraints);
        updateBorderTitle();
    }

    @Override
    public void add(Component comp, Object constraints, int index)
    {
        comp.addComponentListener(contentComponentListener);
        super.add(comp, constraints, index);
        updateBorderTitle();
    }

    @Override
    public void remove(int index)
    {
        Component comp = getComponent(index);
        comp.removeComponentListener(contentComponentListener);
        super.remove(index);
    }

    @Override
    public void remove(Component comp)
    {
        comp.removeComponentListener(contentComponentListener);
        super.remove(comp);
    }

    @Override
    public void removeAll()
    {
        for (Component c : getComponents())
        {
            c.removeComponentListener(contentComponentListener);
        }
        super.removeAll();
    }

    protected void toggleVisibility()
    {
        toggleVisibility(hasInvisibleComponent());
    }

    protected void toggleVisibility(boolean visible)
    {
        for (Component c : getComponents())
        {
            c.setVisible(visible);
        }
        updateBorderTitle();
    }

    protected void updateBorderTitle()
    {
        String arrow = "";
        if (getComponentCount() > 0)
        {
            arrow = (hasInvisibleComponent() ? "▽" : "△");
        }
        border.setTitle(title + " " + arrow);
        repaint();
    }

    protected final boolean hasInvisibleComponent()
    {
        for (Component c : getComponents())
        {
            if (!c.isVisible())
            {
                return true;
            }
        }
        return false;
    }

}
