// Created by plusminus on 2:29:40 PM - Mar 8, 2009
package org.osmdroid.mtp.ui

import org.osmdroid.mtp.OSMMapTilePackager
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.Scanner
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JProgressBar
import javax.swing.JTextArea

class BatchExecutorGUI : JFrame() {
    // ===========================================================
    // Fields
    // ===========================================================
    private var mTxtBatchItems: JTextArea? = null
    private var mBtnStartBatch: JButton? = null

    private var mProgressBar: JProgressBar? = null

    private var mProgress = 0

    init {
        initGUI()
        this.setDefaultCloseOperation(EXIT_ON_CLOSE)
        this.setPreferredSize(Dimension(300, 200))
        this.pack()
    }

    private fun initGUI() {
        this.setLayout(BorderLayout())

        this.mTxtBatchItems = JTextArea()
        this.add(this.mTxtBatchItems, BorderLayout.CENTER)
        this.mTxtBatchItems!!.setFont(Font("Tahoma", Font.PLAIN, 8))


        this.mBtnStartBatch = JButton("Run Batch")
        this.add(this.mBtnStartBatch, BorderLayout.SOUTH)
        this.mBtnStartBatch!!.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                freezeUI()
                startBatch()
                unFreezeUI()
            }
        })

        this.mProgressBar = JProgressBar()
        this.mProgressBar!!.setStringPainted(true)
        this.add(this.mProgressBar, BorderLayout.NORTH)
    }

    private fun freezeUI() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))

        this.mBtnStartBatch!!.setEnabled(false)
        this.mTxtBatchItems!!.setEnabled(false)
    }

    private fun unFreezeUI() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
        this.mBtnStartBatch!!.setEnabled(true)
        this.mTxtBatchItems!!.setEnabled(true)
    }

    private fun incrementProgress() {
        synchronized(this) {
            this.mProgress++
            this.mProgressBar!!.setValue(this.mProgress)
        }
        this.mProgressBar!!.paint(this.mProgressBar!!.getGraphics())
    }

    private fun startBatch() {
        val txtBatchItemsContent = this.mTxtBatchItems!!.getText()
        val numLines = txtBatchItemsContent.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size

        this.mProgressBar!!.setMaximum(numLines)

        val scan = Scanner(txtBatchItemsContent)

        this.mProgress = 0
        this.mProgressBar!!.setValue(this.mProgress)
        this.mProgressBar!!.paint(this.mProgressBar!!.getGraphics())

        while (scan.hasNextLine()) {
            val currentLine = scan.nextLine()

            val runner = Thread(object : Runnable {
                override fun run() {
                    OSMMapTilePackager.main(currentLine.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                    this@BatchExecutorGUI.incrementProgress()
                }
            })
            runner.start()
            try {
                runner.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
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
        private const val serialVersionUID = 5863710966745357864L

        // ===========================================================
        // Constructors
        // ===========================================================
        @JvmStatic
        fun main(args: Array<String>) {
            BatchExecutorGUI().setVisible(true)
        }
    }
}
