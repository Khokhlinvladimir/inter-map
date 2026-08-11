// Created by plusminus on 11:39:40 AM - Apr 8, 2009
package org.osmdroid.mtp.ui

import org.osmdroid.mtp.OSMMapTilePackager.ProgressNotification
import org.osmdroid.mtp.OSMMapTilePackager.execute
import org.osmdroid.mtp.OSMMapTilePackager.runCleanup
import org.osmdroid.mtp.OSMMapTilePackager.runFileExpecter
import java.awt.Desktop
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.net.URI
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.JTextField
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class OSMMapTilePackagerUI : JFrame() {
    // ===========================================================
    // Fields
    // ===========================================================
    private val mainPanel: JPanel
    private val lblURL = JLabel("URL:")

    private val lblDestination = JLabel("Destination File (.zip, .gemf, or .sqlite):")
    private val txtDestination = JTextField()
    private val cmdDestinationBrowse = JButton("Browse")
    private val lblTempFolder = JLabel("Temp-Folder (and tile source name):")
    private val txtTempFolder = JTextField()
    private val cmdTempFolderBrowse = JButton("Browse")
    private val txtURL = JTextField("https://b.tile.openstreetmap.org/%d/%d/%d.png")
    private val cmdURLTest = JButton("Test")

    private val cmdExecute = JButton("Execute")

    private val lblMinZoom = JLabel("MinZoom:")
    private val lblMaxZoom = JLabel("MaxZoom:")
    private val sliMinZoom = JSlider()
    private val sliMaxZoom = JSlider()

    private val lblNorth = JLabel("North:")
    private val txtNorth = JTextField()

    private val lblEast = JLabel("East:")
    private val txtEast = JTextField()

    private val lblSouth = JLabel("South:")
    private val txtSouth = JTextField()

    private val lblWest = JLabel("West:")
    private val txtWest = JTextField()

    private val lblFileAppendix = JLabel("FileAppendix:")
    private val txtFileAppendix = JTextField("")
    private val chkForce = JCheckBox("Force")
    private val lblForce = JLabel("(Will not ask on problems.)")
    private val lblStatus = JLabel("Status:")

    init {
        mainPanel = JPanel()
        this.add(this.mainPanel)
        val gbpanel0 = GridBagLayout()
        val gbcpanel0 = GridBagConstraints()
        mainPanel.setLayout(gbpanel0)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 0
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblURL, gbcpanel0)
        mainPanel.add(lblURL)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 1
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblDestination, gbcpanel0)
        mainPanel.add(lblDestination)

        gbcpanel0.gridx = 1
        gbcpanel0.gridy = 1
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(txtDestination, gbcpanel0)
        mainPanel.add(txtDestination)

        gbcpanel0.gridx = 2
        gbcpanel0.gridy = 1
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(cmdDestinationBrowse, gbcpanel0)
        cmdDestinationBrowse.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                val jfc = JFileChooser()
                jfc.setFileSelectionMode(JFileChooser.FILES_ONLY)
                jfc.setAcceptAllFileFilterUsed(true)

                val result = jfc.showSaveDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    val absolutePath = jfc.getSelectedFile().getAbsolutePath()
                    if (absolutePath.endsWith(".zip")) {
                        txtDestination.setText(absolutePath)
                    } else {
                        txtDestination.setText(absolutePath + ".zip")
                    }
                }
            }
        })
        mainPanel.add(cmdDestinationBrowse)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 2
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblTempFolder, gbcpanel0)
        mainPanel.add(lblTempFolder)

        gbcpanel0.gridx = 1
        gbcpanel0.gridy = 2
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(txtTempFolder, gbcpanel0)
        mainPanel.add(txtTempFolder)

        gbcpanel0.gridx = 2
        gbcpanel0.gridy = 2
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(cmdTempFolderBrowse, gbcpanel0)
        cmdTempFolderBrowse.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                val jfc = JFileChooser()
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
                jfc.setAcceptAllFileFilterUsed(false)

                val result = jfc.showSaveDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    txtTempFolder.setText(jfc.getSelectedFile().getAbsolutePath())
                }
            }
        })
        mainPanel.add(cmdTempFolderBrowse)

        gbcpanel0.gridx = 1
        gbcpanel0.gridy = 0
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(txtURL, gbcpanel0)
        mainPanel.add(txtURL)

        gbcpanel0.gridx = 2
        gbcpanel0.gridy = 0
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(cmdURLTest, gbcpanel0)
        cmdURLTest.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().browse(URI(String.format(txtURL.getText(), 0, 0, 0)))
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Could not open browser.")
                }
            }
        })
        mainPanel.add(cmdURLTest)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 3
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblMinZoom, gbcpanel0)
        mainPanel.add(lblMinZoom)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 4
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblMaxZoom, gbcpanel0)
        mainPanel.add(lblMaxZoom)

        sliMinZoom.addChangeListener(object : ChangeListener {
            override fun stateChanged(e: ChangeEvent?) {
                lblMinZoom.setText("MinZoom: (" + sliMinZoom.getValue() + ")")
            }
        })
        sliMinZoom.setMinimum(0)
        sliMinZoom.setMaximum(22)
        sliMinZoom.setMajorTickSpacing(1)
        sliMinZoom.setMinorTickSpacing(1)
        sliMinZoom.setValue(0)
        gbcpanel0.gridx = 1
        gbcpanel0.gridy = 3
        gbcpanel0.gridwidth = 2
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(sliMinZoom, gbcpanel0)
        mainPanel.add(sliMinZoom)

        sliMaxZoom.addChangeListener(object : ChangeListener {
            override fun stateChanged(e: ChangeEvent?) {
                lblMaxZoom.setText("MaxZoom: (" + sliMaxZoom.getValue() + ")")
            }
        })
        sliMaxZoom.setMaximum(22)
        sliMaxZoom.setMinimum(0)
        sliMaxZoom.setValue(10)
        sliMaxZoom.setMajorTickSpacing(1)
        sliMaxZoom.setMinorTickSpacing(1)
        gbcpanel0.gridx = 1
        gbcpanel0.gridy = 4
        gbcpanel0.gridwidth = 2
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(sliMaxZoom, gbcpanel0)
        mainPanel.add(sliMaxZoom)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 5
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblNorth, gbcpanel0)
        mainPanel.add(lblNorth)

        gbcpanel0.gridx = 1
        gbcpanel0.gridy = 5
        gbcpanel0.gridwidth = 2
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(txtNorth, gbcpanel0)
        txtNorth.setText("90.0")
        mainPanel.add(txtNorth)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 6
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblEast, gbcpanel0)
        mainPanel.add(lblEast)

        gbcpanel0.gridx = 1
        gbcpanel0.gridy = 6
        gbcpanel0.gridwidth = 2
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(txtEast, gbcpanel0)
        txtEast.setText("180.0")
        mainPanel.add(txtEast)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 7
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblSouth, gbcpanel0)
        mainPanel.add(lblSouth)

        gbcpanel0.gridx = 1
        gbcpanel0.gridy = 7
        gbcpanel0.gridwidth = 2
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(txtSouth, gbcpanel0)
        txtSouth.setText("-90.0")
        mainPanel.add(txtSouth)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 9
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblFileAppendix, gbcpanel0)

        mainPanel.add(lblFileAppendix)

        gbcpanel0.gridx = 1
        gbcpanel0.gridy = 8
        gbcpanel0.gridwidth = 2
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(txtWest, gbcpanel0)
        txtWest.setText("-180.0")
        mainPanel.add(txtWest)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 8
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 0.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblWest, gbcpanel0)
        mainPanel.add(lblWest)

        gbcpanel0.gridx = 1
        gbcpanel0.gridy = 9
        gbcpanel0.gridwidth = 2
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(txtFileAppendix, gbcpanel0)

        mainPanel.add(txtFileAppendix)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 10
        gbcpanel0.gridwidth = 1
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(chkForce, gbcpanel0)
        mainPanel.add(chkForce)

        gbcpanel0.gridx = 1
        gbcpanel0.gridy = 10
        gbcpanel0.gridwidth = 2
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblForce, gbcpanel0)
        mainPanel.add(lblForce)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 11
        gbcpanel0.gridwidth = 2
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(cmdExecute, gbcpanel0)
        mainPanel.add(cmdExecute)

        gbcpanel0.gridx = 0
        gbcpanel0.gridy = 12
        gbcpanel0.gridwidth = 2
        gbcpanel0.gridheight = 1
        gbcpanel0.fill = GridBagConstraints.BOTH
        gbcpanel0.weightx = 1.0
        gbcpanel0.weighty = 0.0
        gbcpanel0.anchor = GridBagConstraints.NORTH
        gbpanel0.setConstraints(lblStatus, gbcpanel0)
        mainPanel.add(lblStatus)

        cmdExecute.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                //TODO validate user input
                //Start the process
                val north = txtNorth.getText().toDouble()
                val south = txtSouth.getText().toDouble()
                val east = txtEast.getText().toDouble()
                val west = txtWest.getText().toDouble()
                val minzoom = sliMinZoom.getValue()
                val maxzoom = sliMaxZoom.getValue()
                val url = txtURL.getText()
                val destination = txtDestination.getText()
                val temp = txtTempFolder.getText()
                val appendix = txtFileAppendix.getText()
                val threads = Runtime.getRuntime().availableProcessors()

                val count = runFileExpecter(minzoom, maxzoom, north, south, east, west)

                val dialogButton =
                    JOptionPane.showConfirmDialog(null, "This will download " + count + " tiles. OK?", "Warning", JOptionPane.YES_NO_OPTION)

                if (dialogButton == JOptionPane.YES_OPTION) {
                    Thread(object : Runnable {
                        override fun run() {
                            execute(
                                url,
                                destination,
                                temp,
                                threads,
                                appendix,
                                minzoom,
                                maxzoom,
                                north,
                                south,
                                east,
                                west,
                                object : ProgressNotification {
                                    override fun updateProgress(msg: String?) {
                                        lblStatus.setText(msg)
                                    }
                                })

                            val dialogButton2 = JOptionPane.showConfirmDialog(
                                null,
                                "All done, do you want delete the tile cache?",
                                "Warning",
                                JOptionPane.YES_NO_OPTION
                            )

                            if (dialogButton2 == JOptionPane.YES_OPTION) {
                                runCleanup(temp, false)
                            }
                            lblStatus.setText("Done")
                        }
                    }).start()
                }
            }
        })
    } // ===========================================================
    // Getter & Setter
    // ===========================================================
    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================
    // ===========================================================
    // Methods
    // ===========================================================
    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

    companion object {
        // ===========================================================
        // Constants
        // ===========================================================
        private const val serialVersionUID = 749039680990304151L

        // ===========================================================
        // Constructors
        // ===========================================================
        @JvmStatic
        fun main(args: Array<String>) {
            val j: JFrame = OSMMapTilePackagerUI()
            j.setPreferredSize(Dimension(640, 400))
            j.setDefaultCloseOperation(EXIT_ON_CLOSE)
            j.pack()
            j.setVisible(true)
        }
    }
}
