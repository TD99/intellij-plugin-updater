package dev.timduerr.ipu;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class JarListCellRenderer extends DefaultListCellRenderer {

    private final Icon icon;

    public JarListCellRenderer(int iconSize) {
        Icon tmp;
        try {
            ImageIcon orig = new ImageIcon(Objects.requireNonNull(getClass().getResource("/jar.png")));
            Image img = orig.getImage()
                    .getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
            tmp = new ImageIcon(img);
        } catch (Exception e) {
            // fallback to default file icon
            tmp = UIManager.getIcon("FileView.fileIcon");
        }
        this.icon = tmp;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);
        label.setIcon(icon);
        label.setHorizontalTextPosition(SwingConstants.RIGHT);
        label.setIconTextGap(10);
        return label;
    }
}
