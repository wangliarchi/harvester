package edu.olivet.harvester.ui.panel;

import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.hunt.model.HuntStandard;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 1/24/2018 11:58 AM
 */
public class HuntStandardPanel extends JPanel {
    private final HuntStandard huntStandard;
    private final String title;

    public HuntStandardPanel(HuntStandard huntStandard, String title) {
        this.huntStandard = huntStandard;
        this.title = title;
        initComponents();
    }

    private static final int FIELDWIDTH = 100;

    private void initComponents() {
        setBorder(BorderFactory.createTitledBorder(null, title));

        JLabel lblYP = new JLabel(UIText.label("label.standard.positive.year"));
        JLabel lblYR = new JLabel(UIText.label("label.standard.ratingcount.year"));
        JLabel lblMR = new JLabel(UIText.label("label.standard.ratingcount.month"));
        JLabel lblMP = new JLabel(UIText.label("label.standard.positive.month"));
        monthPositive = new JTextField();
        monthRatingCount = new JTextField();
        yearPositive = new JTextField();
        yearRatingCount = new JTextField();

        monthPositive.setText(String.valueOf(huntStandard.getMonthlyRating().getPositive()));
        monthRatingCount.setText(String.valueOf(huntStandard.getMonthlyRating().getCount()));
        yearPositive.setText(String.valueOf(huntStandard.getYearlyRating().getPositive()));
        yearRatingCount.setText(String.valueOf(huntStandard.getYearlyRating().getCount()));

        monthPositive.setEnabled(false);
        monthRatingCount.setEnabled(false);
        yearPositive.setEnabled(false);
        yearRatingCount.setEnabled(false);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                                        .addComponent(lblYP)
                                        .addComponent(lblMP))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(yearPositive, FIELDWIDTH, FIELDWIDTH, FIELDWIDTH))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(monthPositive, FIELDWIDTH, FIELDWIDTH, FIELDWIDTH)))))
                .addPreferredGap(ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(Alignment.TRAILING)
                                        .addComponent(lblYR)
                                        .addComponent(lblMR))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(yearRatingCount, FIELDWIDTH, FIELDWIDTH, FIELDWIDTH))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(monthRatingCount, FIELDWIDTH, FIELDWIDTH, FIELDWIDTH))))));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblYP)
                                        .addComponent(yearPositive, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblYR)
                                        .addComponent(yearRatingCount, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblMP)
                                        .addComponent(monthPositive, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblMR)
                                        .addComponent(monthRatingCount, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );
    }


    private JTextField yearPositive;
    private JTextField yearRatingCount;
    private JTextField monthRatingCount;
    private JTextField monthPositive;


    public static void main(String[] args) {
        UITools.setTheme();
        JFrame frame = new JFrame();
        frame.setTitle("");
        frame.setSize(700, 180);
        frame.getContentPane().add(new HuntStandardPanel(HuntStandard.newBookDefault(), UIText.title("title.standard.good.bookcd.used")));
        frame.setVisible(true);
    }
}
